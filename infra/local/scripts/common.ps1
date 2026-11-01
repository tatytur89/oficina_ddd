Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Invoke-Tool([string]$Tool, [string[]]$Arguments) {
    # Native stderr is progress output for kind; capture it without treating it as a PS exception.
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $result = & $Tool @Arguments 2>&1
        $code = $LASTEXITCODE
    } finally { $ErrorActionPreference = $previous }
    if ($code -ne 0) { throw "$Tool failed (exit $code): $($result -join [Environment]::NewLine)" }
    return ($result | ForEach-Object { "$_" }) -join [Environment]::NewLine
}

function Initialize-LocalPaths([string]$Root, [string]$Name) {
    if ($Name -notmatch '^oficina-local(-[a-z0-9]+)*$' -or $Name.Length -gt 50) { throw 'Invalid managed cluster name.' }
    if ([string]::IsNullOrWhiteSpace($Root) -or ![IO.Path]::IsPathRooted($Root)) { throw 'An absolute Terraform root is required.' }
    $script:Root = [IO.Path]::GetFullPath($Root)
    $script:Name = $Name
    $script:Generated = Join-Path $script:Root '.generated'
    $script:Kubeconfig = Join-Path $script:Generated 'kubeconfig'
    $script:Marker = Join-Path $script:Generated 'owner.json'
    $script:Config = Join-Path $script:Generated 'kind.json'
    $script:Context = "kind-$Name"
    foreach ($tool in @('docker', 'kind', 'kubectl')) { Get-Command $tool -CommandType Application -ErrorAction Stop | Out-Null }
    # Pin kind and docker to the same local runtime for the duration of this child process.
    $env:KIND_EXPERIMENTAL_PROVIDER = 'docker'
    if ($env:DOCKER_CONTEXT) {
        $endpoint = Invoke-Tool 'docker' @('context', 'inspect', $env:DOCKER_CONTEXT, '--format', '{{.Endpoints.docker.Host}}')
    } elseif ($env:DOCKER_HOST) {
        $endpoint = $env:DOCKER_HOST
    } else {
        $endpoint = Invoke-Tool 'docker' @('context', 'inspect', '--format', '{{.Endpoints.docker.Host}}')
    }
    $endpoint = $endpoint.Trim()
    if ($endpoint -notmatch '^(npipe:/{2,4}\./pipe/[^/]+$|unix:///[^\r\n]+$)') {
        throw 'Only a local Docker named pipe or Unix socket is supported.'
    }
    $env:DOCKER_CONTEXT = $null
    $env:DOCKER_HOST = $endpoint
    $script:EngineId = (Invoke-Tool 'docker' @('info', '--format', '{{.ID}}')).Trim()
    if ([string]::IsNullOrWhiteSpace($script:EngineId)) { throw 'Docker Engine identity is unavailable.' }
}

function Enter-ClusterLock {
    $mutex = New-Object System.Threading.Mutex($false, "Local\OficinaKind-$script:Name")
    try {
        try { $acquired = $mutex.WaitOne(0) } catch [System.Threading.AbandonedMutexException] { $acquired = $true }
        if (!$acquired) { throw 'Another process is managing this cluster name. Retry after it finishes.' }
        return $mutex
    } catch { $mutex.Dispose(); throw }
}

function Get-ManagedNodes {
    $ids = Invoke-Tool 'docker' @('ps', '-aq', '--no-trunc', '--filter', "label=io.x-k8s.kind.cluster=$script:Name")
    if ([string]::IsNullOrWhiteSpace($ids)) { return @() }
    return @($ids -split '\r?\n' | Where-Object { $_ })
}

function Read-Owner {
    if (!(Test-Path -LiteralPath $script:Marker)) { throw 'Ownership marker missing. Refusing to manage existing containers.' }
    $owner = Get-Content -LiteralPath $script:Marker -Raw | ConvertFrom-Json
    if ($owner.name -cne $script:Name -or $owner.root -cne $script:Root) { throw 'Ownership marker does not match this Terraform root and cluster.' }
    if ($owner.PSObject.Properties.Name -notcontains 'engine_id' -or $owner.engine_id -cne $script:EngineId) {
        throw 'Docker Engine differs from the recorded owner. Restore the original Docker context before managing this cluster.'
    }
    return $owner
}

function Assert-OwnedNodes($Owner, [string[]]$Nodes) {
    foreach ($id in $Nodes) {
        if ($id -notin @($Owner.node_ids)) { throw "Unrecognized container for $script:Name. Refusing to modify cluster." }
    }
}

function Write-Owner($Owner) {
    [IO.File]::WriteAllText($script:Marker, ($Owner | ConvertTo-Json -Depth 10))
}

function Remove-ManagedFiles {
    foreach ($file in @($script:Kubeconfig, $script:Config, $script:Marker)) {
        if (Test-Path -LiteralPath $file) { Remove-Item -LiteralPath $file -Force }
    }
}
