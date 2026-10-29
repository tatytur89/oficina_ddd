[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$tempParent = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
$testRoot = Join-Path $tempParent ('oficina-cluster-test-' + [Guid]::NewGuid().ToString('N'))
$clusterName = 'oficina-local-test-' + [Guid]::NewGuid().ToString('N').Substring(0, 10)
$checks = New-Object System.Collections.Generic.List[string]
$applyAttempted = $false
$cleanupSafe = $true
$failure = $null
$cleanupFailure = $null

function Assert-Check([bool]$Condition, [string]$Message) {
    if (!$Condition) { throw "FAILED: $Message" }
    $checks.Add($Message)
    Write-Host "PASS: $Message"
}

function Invoke-Native([string]$Name, [string[]]$Arguments, [int]$TimeoutSeconds = 60) {
    $executable = (Get-Command $Name -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
    $start = New-Object System.Diagnostics.ProcessStartInfo
    $start.FileName = $executable
    $start.Arguments = (($Arguments | ForEach-Object { '"' + $_.Replace('"', '\"') + '"' }) -join ' ')
    $start.WorkingDirectory = $repoRoot
    $start.UseShellExecute = $false
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $process = [System.Diagnostics.Process]::Start($start)
    $output = $process.StandardOutput.ReadToEndAsync()
    $errorOutput = $process.StandardError.ReadToEndAsync()
    $watch = [System.Diagnostics.Stopwatch]::StartNew()
    $nextUpdate = 30
    while (!$process.WaitForExit(1000)) {
        if ($watch.Elapsed.TotalSeconds -ge $TimeoutSeconds) {
            $process.Kill()
            throw "$Name timed out. Preserve the temporary Terraform state for recovery: $testRoot"
        }
        if ($watch.Elapsed.TotalSeconds -ge $nextUpdate) {
            Write-Host "Waiting for $Name ($([int]$watch.Elapsed.TotalSeconds) seconds)..."
            $nextUpdate += 30
        }
    }
    return [pscustomobject]@{ Code = $process.ExitCode; Output = $output.Result; Error = $errorOutput.Result }
}

function Invoke-Success([string]$Name, [string[]]$Arguments, [int]$TimeoutSeconds = 60) {
    $result = Invoke-Native $Name $Arguments $TimeoutSeconds
    if ($result.Code -ne 0) { throw "$Name failed (exit $($result.Code)): $($result.Error)`n$($result.Output)" }
    return $result.Output
}

function Get-ConfigHash([string]$Path) {
    if (!(Test-Path -LiteralPath $Path)) { return '<missing>' }
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
}

function Get-ExternalState {
    return [pscustomobject]@{
        DockerContext = (Invoke-Success 'docker' @('context', 'show')).Trim()
        Containers = ((Invoke-Success 'docker' @('ps', '-aq', '--no-trunc')) -split '\r?\n' | Sort-Object) -join ','
        KindClusters = ((Invoke-Success 'kind' @('get', 'clusters')) -split '\r?\n' | Sort-Object) -join ','
        DockerConfig = Get-ConfigHash (Join-Path $env:USERPROFILE '.docker/config.json')
        KubeConfig = Get-ConfigHash (Join-Path $env:USERPROFILE '.kube/config')
        KubeContexts = (Invoke-Success 'kubectl' @('config', 'get-contexts', '-o', 'name')).Trim()
        KubeConfigEnvironment = $env:KUBECONFIG
        DockerContextEnvironment = $env:DOCKER_CONTEXT
        DockerHostEnvironment = $env:DOCKER_HOST
        UserPath = [Environment]::GetEnvironmentVariable('Path', 'User')
        MachinePath = [Environment]::GetEnvironmentVariable('Path', 'Machine')
    }
}

function Get-AvailablePort {
    $listener = New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Loopback, 0)
    $listener.Start()
    try { return $listener.LocalEndpoint.Port } finally { $listener.Stop() }
}

function Invoke-Terraform([string[]]$Arguments, [int]$TimeoutSeconds = 60) {
    return Invoke-Native 'terraform' (@("-chdir=$moduleRoot") + $Arguments) $TimeoutSeconds
}

function Assert-TerraformSuccess([string[]]$Arguments, [int]$TimeoutSeconds = 60) {
    $result = Invoke-Terraform $Arguments $TimeoutSeconds
    if ($result.Code -ne 0) { throw "Terraform failed (exit $($result.Code)): $($result.Error)`n$($result.Output)" }
    return $result.Output
}

function Invoke-Lifecycle([string]$ScriptName) {
    $oldRoot = $env:OFICINA_ROOT
    $oldName = $env:OFICINA_NAME
    try {
        $env:OFICINA_ROOT = $moduleRoot
        $env:OFICINA_NAME = $clusterName
        return Invoke-Native 'powershell.exe' @('-NoProfile', '-ExecutionPolicy', 'RemoteSigned', '-File',
            (Join-Path $moduleRoot "scripts/$ScriptName.ps1"), '-Root', $moduleRoot, '-Name', $clusterName) 240
    } finally {
        $env:OFICINA_ROOT = $oldRoot
        $env:OFICINA_NAME = $oldName
    }
}

$before = Get-ExternalState
Assert-Check (!(($before.KindClusters -split ',') -contains $clusterName)) 'Unique smoke cluster does not already exist'
$moduleRoot = Join-Path $testRoot 'infra/local'
$httpPort = Get-AvailablePort
do { $httpsPort = Get-AvailablePort } while ($httpsPort -eq $httpPort)

try {
    New-Item -ItemType Directory -Path $moduleRoot -Force | Out-Null
    $sourceRoot = Join-Path $repoRoot 'infra/local'
    foreach ($file in Get-ChildItem -LiteralPath $sourceRoot -Recurse -File) {
        if ($file.FullName -match '[\\/]\.(terraform|generated)[\\/]') { continue }
        if ($file.Extension -notin @('.tf', '.ps1', '.psm1') -and $file.Name -ne '.terraform.lock.hcl') { continue }
        $relative = $file.FullName.Substring($sourceRoot.Length).TrimStart([char[]]'\/')
        $destination = Join-Path $moduleRoot $relative
        New-Item -ItemType Directory -Path (Split-Path $destination) -Force | Out-Null
        Copy-Item -LiteralPath $file.FullName -Destination $destination
    }
    Assert-Check (!(Test-Path -LiteralPath (Join-Path $moduleRoot 'terraform.tfstate'))) 'Smoke copy contains no existing Terraform state'
    Assert-Check (!(Test-Path -LiteralPath (Join-Path $moduleRoot '.generated'))) 'Smoke copy contains no existing ownership or kubeconfig'
    $vars = [pscustomobject]@{ cluster_name = $clusterName; http_port = $httpPort; https_port = $httpsPort }
    $varsFile = Join-Path $moduleRoot 'smoke.auto.tfvars.json'
    [System.IO.File]::WriteAllText($varsFile, ($vars | ConvertTo-Json))
    Assert-TerraformSuccess @('fmt', '-check', '-recursive') | Out-Null
    Assert-Check $true 'Terraform formatting is valid'
    Assert-TerraformSuccess @('init', '-input=false', '-no-color') 180 | Out-Null
    Assert-TerraformSuccess @('validate', '-no-color') | Out-Null
    Assert-Check $true 'Isolated local Terraform initializes and validates'
    $plan = Invoke-Terraform @('plan', '-input=false', '-no-color', '-detailed-exitcode', '-out=smoke.tfplan') 180
    Assert-Check ($plan.Code -eq 2) 'Initial smoke plan contains only a new local cluster lifecycle'
    $planJson = Assert-TerraformSuccess @('show', '-json', 'smoke.tfplan') | ConvertFrom-Json
    foreach ($change in @($planJson.resource_changes)) {
        Assert-Check ($change.type -in @('terraform_data', 'kind_cluster')) 'Planned resource is restricted to local kind lifecycle'
    }
    $applyAttempted = $true
    $cleanupSafe = $false
    Assert-TerraformSuccess @('apply', '-input=false', '-no-color', '-auto-approve', 'smoke.tfplan') 900 | Out-Null
    Assert-Check $true 'Terraform created the exclusively named smoke cluster'
    $outputs = Assert-TerraformSuccess @('output', '-json') | ConvertFrom-Json
    Assert-Check ($outputs.cluster_name.value -eq $clusterName) 'Terraform output identifies only the smoke cluster'
    $kubeconfig = [System.IO.Path]::GetFullPath($outputs.kubeconfig_path.value)
    $context = $outputs.context.value
    Assert-Check ($kubeconfig.StartsWith($moduleRoot, [StringComparison]::OrdinalIgnoreCase)) 'Kubeconfig is private to the isolated Terraform copy'
    Assert-Check (Test-Path -LiteralPath $kubeconfig) 'Private kubeconfig exists'
    Assert-Check ($context -eq "kind-$clusterName") 'kubectl context is specific to the smoke cluster'
    $kubectlArgs = @('--kubeconfig', $kubeconfig, '--context', $context)
    Invoke-Success 'kubectl' ($kubectlArgs + @('wait', '--for=condition=Ready', 'nodes', '--all', '--timeout=180s')) 200 | Out-Null
    $nodes = Invoke-Success 'kubectl' ($kubectlArgs + @('get', 'nodes', '-o', 'json')) | ConvertFrom-Json
    Assert-Check (@($nodes.items).Count -eq 2) 'Cluster contains exactly one control-plane and one worker'
    $controlPlanes = @($nodes.items | Where-Object { $_.metadata.labels.PSObject.Properties.Name -contains 'node-role.kubernetes.io/control-plane' })
    Assert-Check ($controlPlanes.Count -eq 1) 'Exactly one node is the control-plane'
    foreach ($node in $nodes.items) {
        Assert-Check ($node.status.nodeInfo.kubeletVersion -match '^v1\.34\.') 'Node runs the pinned Kubernetes 1.34 line'
    }
    $ready = Invoke-Success 'kubectl' ($kubectlArgs + @('get', '--raw=/readyz'))
    Assert-Check ($ready.Trim() -eq 'ok') 'Kubernetes API readiness endpoint responds successfully'
    $check = Invoke-Lifecycle 'check'
    Assert-Check ($check.Code -eq 0) 'Runtime checker validates ownership and readiness'
    $server = Invoke-Success 'kubectl' ($kubectlArgs + @('config', 'view', '--minify', '-o', 'jsonpath={.clusters[0].cluster.server}'))
    Assert-Check ($server -match '^https://127\.0\.0\.1:') 'API endpoint binds to loopback'
    $controlPlaneName = $controlPlanes[0].metadata.name
    $bindings = Invoke-Success 'docker' @('inspect', $controlPlaneName, '--format', '{{json .HostConfig.PortBindings}}') | ConvertFrom-Json
    foreach ($binding in $bindings.PSObject.Properties) {
        foreach ($address in $binding.Value) {
            Assert-Check ($address.HostIp -eq '127.0.0.1') 'Published API/Ingress ports bind only to loopback'
        }
    }
    $ownerPath = Join-Path $moduleRoot '.generated/owner.json'
    $ownerText = [IO.File]::ReadAllText($ownerPath)
    $heldOwner = Join-Path $moduleRoot '.generated/owner.held'
    try {
        Move-Item -LiteralPath $ownerPath -Destination $heldOwner
        $refused = Invoke-Lifecycle 'destroy'
        Assert-Check ($refused.Code -ne 0) 'Destroy refuses a cluster without its ownership marker'
    } finally { Move-Item -LiteralPath $heldOwner -Destination $ownerPath }
    try {
        $wrongOwner = $ownerText | ConvertFrom-Json
        $wrongOwner.node_ids = @('not-the-owned-container')
        [IO.File]::WriteAllText($ownerPath, ($wrongOwner | ConvertTo-Json))
        $refused = Invoke-Lifecycle 'destroy'
        Assert-Check ($refused.Code -ne 0) 'Destroy refuses containers not recorded by this root'
    } finally { [IO.File]::WriteAllText($ownerPath, $ownerText) }
    $refused = Invoke-Lifecycle 'create'
    Assert-Check ($refused.Code -ne 0) 'Create refuses an existing cluster instead of adopting it'
    $secondPlan = Invoke-Terraform @('plan', '-input=false', '-no-color', '-detailed-exitcode') 180
    Assert-Check ($secondPlan.Code -eq 0) 'Second Terraform plan is idempotent with no changes'
    $badPlan = Invoke-Terraform @('plan', '-input=false', '-no-color', '-var=cluster_name=another-project') 60
    Assert-Check ($badPlan.Code -ne 0) 'Terraform rejects cluster names outside the project prefix'
    $badPlan = Invoke-Terraform @('plan', '-input=false', '-no-color', "-var=https_port=$httpPort") 60
    Assert-Check ($badPlan.Code -ne 0) 'Terraform rejects colliding HTTP and HTTPS ports'
    $workerName = @($nodes.items | Where-Object { $_.metadata.name -ne $controlPlaneName })[0].metadata.name
    Invoke-Success 'docker' @('rm', '-f', $workerName) | Out-Null
    $unhealthy = Invoke-Lifecycle 'check'
    Assert-Check ($unhealthy.Code -ne 0) 'Runtime checker detects a node removed outside Terraform'
} catch {
    $failure = $_
} finally {
    if ($applyAttempted) {
        try {
            Assert-TerraformSuccess @('destroy', '-input=false', '-no-color', '-auto-approve') 300 | Out-Null
            $state = Assert-TerraformSuccess @('state', 'list')
            Assert-Check ([string]::IsNullOrWhiteSpace($state)) 'Managed destroy clears the isolated Terraform resource state'
            $clusters = (Invoke-Success 'kind' @('get', 'clusters')) -split '\r?\n'
            Assert-Check (!($clusters -contains $clusterName)) 'Managed destroy removes only the smoke cluster'
            $remaining = Invoke-Success 'docker' @('ps', '-aq', '--filter', "label=io.x-k8s.kind.cluster=$clusterName")
            Assert-Check ([string]::IsNullOrWhiteSpace($remaining)) 'No smoke-cluster containers remain'
            $cleanupSafe = $true
        } catch { $cleanupFailure = $_ }
    }
    try {
        $after = Get-ExternalState
        foreach ($property in $before.PSObject.Properties.Name) {
            Assert-Check ($before.$property -ceq $after.$property) "External state unchanged: $property"
        }
    } catch { if ($null -eq $cleanupFailure) { $cleanupFailure = $_ } }
    if ($cleanupSafe) {
        $resolvedRoot = [System.IO.Path]::GetFullPath($testRoot)
        if (!$resolvedRoot.StartsWith($tempParent, [StringComparison]::OrdinalIgnoreCase) -or
                !(Split-Path $resolvedRoot -Leaf).StartsWith('oficina-cluster-test-')) {
            throw 'Unsafe temporary cleanup target.'
        }
        if (Test-Path -LiteralPath $resolvedRoot) { Remove-Item -LiteralPath $resolvedRoot -Recurse -Force }
    }
}

if ($null -ne $cleanupFailure) { throw "Cleanup or preservation failed. Keep $testRoot for recovery. $cleanupFailure" }
if ($null -ne $failure) { throw $failure }
Write-Host "Local cluster smoke checks passed: $($checks.Count). The managed test cluster was destroyed."
