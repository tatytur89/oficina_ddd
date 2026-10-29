[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$tempParent = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$testRoot = Join-Path $tempParent ('oficina-guard-tests-' + [Guid]::NewGuid().ToString('N'))
$powershell = (Get-Command powershell.exe -CommandType Application | Select-Object -First 1).Source
$checks = New-Object System.Collections.Generic.List[string]

function Assert-Check([bool]$Condition, [string]$Message) {
    if (!$Condition) { throw "FAILED: $Message" }
    $checks.Add($Message)
    Write-Host "PASS: $Message"
}

function New-Case([string]$Name, [string]$Mode) {
    $root = Join-Path $testRoot $Name
    New-Item -ItemType Directory -Path $root -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $repoRoot 'infra/local/scripts') -Destination $root -Recurse
    $state = Join-Path $root 'mock-state'
    New-Item -ItemType Directory -Path $state | Out-Null
    return [pscustomobject]@{ Root=$root; State=$state; Mode=$Mode; Name=('oficina-local-guard-' + [Guid]::NewGuid().ToString('N').Substring(0, 8)) }
}

function Start-Guard([object]$Case, [string]$Script, [hashtable]$ExtraEnvironment = @{}) {
    $start = New-Object Diagnostics.ProcessStartInfo
    $start.FileName = $powershell
    $scriptPath = Join-Path $Case.Root "scripts/$Script.ps1"
    $start.Arguments = '-NoProfile -ExecutionPolicy RemoteSigned -File "' + $scriptPath + '"'
    if ($Script -eq 'check') { $start.Arguments += ' -Root "' + $Case.Root + '" -Name "' + $Case.Name + '"' }
    $start.UseShellExecute = $false
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    # The restricted child PATH contains no real Docker/kind/kubectl executable.
    $start.EnvironmentVariables['Path'] = $mockBin + ';' + $env:SystemRoot + '/System32;' + (Split-Path $powershell)
    $start.EnvironmentVariables['OFICINA_ROOT'] = $Case.Root
    $start.EnvironmentVariables['OFICINA_NAME'] = $Case.Name
    $start.EnvironmentVariables['OFICINA_CONFIG'] = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('{"kind":"Cluster"}'))
    $start.EnvironmentVariables['OFICINA_CONFIG_HASH'] = 'mock-config-hash'
    $start.EnvironmentVariables['MOCK_STATE'] = $Case.State
    $start.EnvironmentVariables['MOCK_MODE'] = $Case.Mode
    $start.EnvironmentVariables['MOCK_ENDPOINT'] = 'npipe:////./pipe/oficina-guard-test'
    $start.EnvironmentVariables['DOCKER_HOST'] = 'npipe:////./pipe/oficina-guard-test'
    $start.EnvironmentVariables['KIND_EXPERIMENTAL_PROVIDER'] = 'podman'
    $start.EnvironmentVariables.Remove('DOCKER_CONTEXT')
    foreach ($key in $ExtraEnvironment.Keys) { $start.EnvironmentVariables[$key] = [string]$ExtraEnvironment[$key] }
    $process = [Diagnostics.Process]::Start($start)
    return [pscustomobject]@{ Process=$process; Output=$process.StandardOutput.ReadToEndAsync(); Error=$process.StandardError.ReadToEndAsync() }
}

function Wait-Guard([object]$Running) {
    if (!$Running.Process.WaitForExit(60000)) { $Running.Process.Kill(); throw 'Mock guard process timed out.' }
    return [pscustomobject]@{ Code=$Running.Process.ExitCode; Output=$Running.Output.Result; Error=$Running.Error.Result }
}

function Get-Events([object]$Case) {
    $log = Join-Path $Case.State 'events.jsonl'
    if (!(Test-Path -LiteralPath $log)) { return @() }
    return @(Get-Content -LiteralPath $log | ForEach-Object { $_ | ConvertFrom-Json })
}

function Seed-Owner([object]$Case, [string]$Engine) {
    $generated = Join-Path $Case.Root '.generated'
    New-Item -ItemType Directory -Path $generated -Force | Out-Null
    [IO.File]::WriteAllText((Join-Path $Case.State 'nodes.txt'), "mock-node-a`nmock-node-b")
    $owner = [pscustomobject]@{name=$Case.Name; root=$Case.Root; engine_id=$Engine; config_hash='mock-config-hash'; node_ids=@('mock-node-a','mock-node-b'); status='ready'}
    [IO.File]::WriteAllText((Join-Path $generated 'owner.json'), ($owner | ConvertTo-Json))
    [IO.File]::WriteAllText((Join-Path $generated 'kubeconfig'), 'mock-kubeconfig')
}

try {
    $mockBin = Join-Path $testRoot 'mock-bin'
    New-Item -ItemType Directory -Path $mockBin -Force | Out-Null
    $mockSource = @'
$ErrorActionPreference = 'Stop'
$tool = [string]$args[0]
$toolArgs = @($args | Select-Object -Skip 1)
$joined = $toolArgs -join ' '
$state = $env:MOCK_STATE
$nodesPath = Join-Path $state 'nodes.txt'
$mutexKey = 'Local\OficinaMockLog-' + ([IO.Path]::GetFileName($state)) + '-' + ([IO.Path]::GetFileName((Split-Path $state)))
$mutex = New-Object Threading.Mutex($false, $mutexKey)
try {
    $null = $mutex.WaitOne(10000)
    $event = [pscustomobject]@{tool=$tool; arguments=$joined; provider=$env:KIND_EXPERIMENTAL_PROVIDER}
    [IO.File]::AppendAllText((Join-Path $state 'events.jsonl'), ($event | ConvertTo-Json -Compress) + [Environment]::NewLine)
} finally { $mutex.ReleaseMutex(); $mutex.Dispose() }
if ($tool -eq 'docker') {
    if ($joined -match '^context show') { 'mock-local'; exit 0 }
    if ($joined -match '^context inspect') {
        if ($joined -match '--format') { $env:MOCK_ENDPOINT }
        else { '[{"Name":"mock-local","Endpoints":{"docker":{"Host":"' + $env:MOCK_ENDPOINT + '"}}}]' }
        exit 0
    }
    if ($joined -match '^info' -and $joined -match '\.ID') { 'mock-engine-a'; exit 0 }
    if ($joined -match '^info' -and $joined -match '\.OSType') { 'linux'; exit 0 }
    if ($joined -match '^ps') { if (Test-Path -LiteralPath $nodesPath) { Get-Content -LiteralPath $nodesPath }; exit 0 }
}
if ($tool -eq 'kind') {
    if ($joined -eq 'version') { 'kind v0.33.0 mock'; exit 0 }
    if ($joined -match '^create cluster') {
        if ($env:MOCK_MODE -eq 'already-exists') {
            [IO.File]::WriteAllText($nodesPath, "foreign-node-a`nforeign-node-b")
            [Console]::Error.WriteLine('ERROR: failed to create cluster: node(s) already exist for a cluster with the name')
            exit 1
        }
        if ($env:MOCK_MODE -eq 'partial-failure') {
            [IO.File]::WriteAllText($nodesPath, 'mock-node-a')
            [Console]::Error.WriteLine('Simulated node bootstrap failure')
            exit 1
        }
        Start-Sleep -Milliseconds 1500
        if ((Test-Path -LiteralPath $nodesPath) -and ![string]::IsNullOrWhiteSpace([IO.File]::ReadAllText($nodesPath))) {
            [Console]::Error.WriteLine('ERROR: node(s) already exist for a cluster with the name')
            exit 1
        }
        if ($env:MOCK_MODE -eq 'success-one-node') { [IO.File]::WriteAllText($nodesPath, 'mock-node-a') }
        else { [IO.File]::WriteAllText($nodesPath, "mock-node-a`nmock-node-b") }
        $index = [Array]::IndexOf($toolArgs, '--kubeconfig')
        if ($index -ge 0) { [IO.File]::WriteAllText($toolArgs[$index + 1], 'mock-kubeconfig') }
        exit 0
    }
    if ($joined -match '^delete cluster') { [IO.File]::WriteAllText($nodesPath, ''); exit 0 }
}
if ($tool -eq 'kubectl') {
    if ($joined -match 'get nodes') {
        '{"items":[{"status":{"nodeInfo":{"kubeletVersion":"v1.34.11"}}},{"status":{"nodeInfo":{"kubeletVersion":"v1.34.11"}}}]}'
        exit 0
    }
    if ($joined -match 'wait') { 'nodes Ready'; exit 0 }
    if ($joined -match 'get --raw=/readyz') { 'ok'; exit 0 }
}
[Console]::Error.WriteLine("Unexpected mocked command: $tool $joined")
exit 99
'@
    [IO.File]::WriteAllText((Join-Path $mockBin 'mock-tool.ps1'), $mockSource)
    foreach ($tool in @('docker','kind','kubectl')) {
        $wrapper = '@echo off' + "`r`n" + '"' + $powershell + '" -NoProfile -ExecutionPolicy RemoteSigned -File "%~dp0mock-tool.ps1" ' + $tool + ' %*' + "`r`n"
        [IO.File]::WriteAllText((Join-Path $mockBin "$tool.cmd"), $wrapper)
    }

    $wrongEngine = New-Case 'wrong-engine' 'normal'
    Seed-Owner $wrongEngine 'mock-engine-b'
    $ownerPath = Join-Path $wrongEngine.Root '.generated/owner.json'
    $ownerHash = (Get-FileHash -LiteralPath $ownerPath -Algorithm SHA256).Hash
    foreach ($operation in @('check','destroy')) {
        $result = Wait-Guard (Start-Guard $wrongEngine $operation)
        Assert-Check ($result.Code -ne 0 -and $result.Error -match '(?i)engine|daemon') "$operation refuses an owner from another Docker engine"
    }
    Assert-Check ((Get-FileHash -LiteralPath $ownerPath -Algorithm SHA256).Hash -eq $ownerHash) 'Engine mismatch preserves the ownership marker'
    Assert-Check (Test-Path -LiteralPath (Join-Path $wrongEngine.Root '.generated/kubeconfig')) 'Engine mismatch preserves the private kubeconfig'
    Assert-Check (@(Get-Events $wrongEngine | Where-Object { $_.tool -eq 'kind' -and $_.arguments -match '^delete' }).Count -eq 0) 'Engine mismatch never invokes kind deletion'

    $remote = New-Case 'remote-endpoint' 'normal'
    $result = Wait-Guard (Start-Guard $remote 'create' @{DOCKER_HOST='tcp://example.invalid:2375'; MOCK_ENDPOINT='tcp://example.invalid:2375'})
    Assert-Check ($result.Code -ne 0 -and $result.Error -match '(?i)local|endpoint|npipe|unix') 'Creation refuses a remote Docker endpoint'
    Assert-Check (@(Get-Events $remote | Where-Object { $_.tool -eq 'kind' }).Count -eq 0) 'Remote endpoint refusal happens before any kind operation'

    $remotePipe = New-Case 'remote-named-pipe' 'normal'
    $result = Wait-Guard (Start-Guard $remotePipe 'create' @{DOCKER_HOST='npipe:////remote-server/pipe/docker_engine'; MOCK_ENDPOINT='npipe:////remote-server/pipe/docker_engine'})
    Assert-Check ($result.Code -ne 0 -and $result.Error -match '(?i)local|endpoint|npipe|unix') 'Creation refuses a named pipe on a remote host'
    Assert-Check (@(Get-Events $remotePipe | Where-Object { $_.tool -eq 'kind' }).Count -eq 0) 'Remote named pipe refusal happens before any kind operation'

    $race = New-Case 'concurrent-a' 'normal'
    $other = New-Case 'concurrent-b' 'normal'
    $other.Name = $race.Name
    $other.State = $race.State
    $first = Start-Guard $race 'create'
    $second = Start-Guard $other 'create'
    $a = Wait-Guard $first
    $b = Wait-Guard $second
    if (@($a,$b | Where-Object {$_.Code -eq 0}).Count -ne 1) {
        throw "Unexpected concurrent child results. First exit=$($a.Code): $($a.Error) $($a.Output). Second exit=$($b.Code): $($b.Error) $($b.Output)"
    }
    Assert-Check (@($a,$b | Where-Object {$_.Code -eq 0}).Count -eq 1) 'Concurrent roots produce exactly one successful owner'
    $events = Get-Events $race
    Assert-Check (@($events | Where-Object {$_.tool -eq 'kind' -and $_.arguments -match '^create'}).Count -eq 1) 'Cluster-name mutex serializes concurrent creation across roots'
    Assert-Check (@($events | Where-Object {$_.tool -eq 'kind' -and $_.arguments -match '^delete'}).Count -eq 0) 'Losing concurrent root does not delete the winning cluster'
    Assert-Check (@($events | Where-Object {$_.tool -eq 'kind' -and $_.provider -ne 'docker'}).Count -eq 0) 'Lifecycle forces the Docker provider despite inherited podman setting'
    $owners = @($race,$other | Where-Object {Test-Path -LiteralPath (Join-Path $_.Root '.generated/owner.json')})
    Assert-Check ($owners.Count -eq 1) 'Only the winning root records ownership'

    $already = New-Case 'already-exists' 'already-exists'
    $result = Wait-Guard (Start-Guard $already 'create')
    Assert-Check ($result.Code -ne 0) 'Already-exists failure remains a failed creation'
    Assert-Check (@(Get-Events $already | Where-Object {$_.tool -eq 'kind' -and $_.arguments -match '^delete'}).Count -eq 0) 'Already-exists cleanup never deletes newly observed foreign nodes'
    Assert-Check ([IO.File]::ReadAllText((Join-Path $already.State 'nodes.txt')) -match 'foreign-node') 'Foreign nodes survive an already-exists race'

    $partial = New-Case 'partial-failure' 'partial-failure'
    $result = Wait-Guard (Start-Guard $partial 'create')
    Assert-Check ($result.Code -ne 0) 'Partial bootstrap failure remains visible to Terraform'
    Assert-Check (@(Get-Events $partial | Where-Object {$_.tool -eq 'kind' -and $_.arguments -match '^delete'}).Count -eq 0) 'Failed kind creation never deletes containers it cannot prove it owns'
    Assert-Check ([IO.File]::ReadAllText((Join-Path $partial.State 'nodes.txt')) -match 'mock-node-a') 'Failed kind creation preserves unverified node containers for diagnosis'
    $partialOwnerPath = Join-Path $partial.Root '.generated/owner.json'
    Assert-Check (Test-Path -LiteralPath $partialOwnerPath) 'Failed kind creation preserves its diagnostic ownership marker'
    $partialOwner = Get-Content -LiteralPath $partialOwnerPath -Raw | ConvertFrom-Json
    Assert-Check (@($partialOwner.node_ids).Count -eq 0 -and $partialOwner.status -eq 'creating') 'Failed kind creation does not adopt observed container IDs'

    $incomplete = New-Case 'success-one-node' 'success-one-node'
    $result = Wait-Guard (Start-Guard $incomplete 'create')
    Assert-Check ($result.Code -ne 0) 'Successful kind invocation with an incomplete topology still fails validation'
    Assert-Check (@(Get-Events $incomplete | Where-Object {$_.tool -eq 'kind' -and $_.arguments -match '^delete'}).Count -eq 1) 'Successful kind invocation records ownership before cleaning its incomplete topology'
    Assert-Check ([string]::IsNullOrWhiteSpace([IO.File]::ReadAllText((Join-Path $incomplete.State 'nodes.txt')))) 'Owned incomplete topology is cleaned up'
    Assert-Check (!(Test-Path -LiteralPath (Join-Path $incomplete.Root '.generated/owner.json'))) 'Successful cleanup removes the incomplete topology ownership marker'

    Write-Host "Mock lifecycle guard checks passed: $($checks.Count). No real Docker/kind/kubectl tool was used."
} finally {
    $resolvedRoot = [IO.Path]::GetFullPath($testRoot)
    if (!$resolvedRoot.StartsWith($tempParent, [StringComparison]::OrdinalIgnoreCase) -or
            !(Split-Path $resolvedRoot -Leaf).StartsWith('oficina-guard-tests-')) {
        throw 'Unsafe temporary cleanup target.'
    }
    if (Test-Path -LiteralPath $resolvedRoot) { Remove-Item -LiteralPath $resolvedRoot -Recurse -Force }
}
