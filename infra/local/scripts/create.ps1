. (Join-Path $PSScriptRoot 'common.ps1')
Initialize-LocalPaths $env:OFICINA_ROOT $env:OFICINA_NAME
$managementLock = Enter-ClusterLock
try {
    if (@(Get-ManagedNodes).Count -gt 0) { throw 'Cluster already exists. Creation will not adopt or delete it.' }
    if ((Test-Path -LiteralPath $script:Marker) -or (Test-Path -LiteralPath $script:Kubeconfig)) {
        throw 'Managed files already exist. Check the previous lifecycle before retrying.'
    }
    $version = Invoke-Tool 'kind' @('version')
    if ($version -notmatch '\bv0\.33\.0\b') { throw 'This configuration requires kind v0.33.0.' }
    $dockerOS = Invoke-Tool 'docker' @('info', '--format', '{{.OSType}}')
    if ($dockerOS.Trim() -ne 'linux') { throw 'Docker must run Linux containers.' }
    $configText = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($env:OFICINA_CONFIG))
    New-Item -ItemType Directory -Path $script:Generated -Force | Out-Null
    [IO.File]::WriteAllText($script:Config, $configText)
    $owner = [pscustomobject]@{ name = $script:Name; root = $script:Root; engine_id = $script:EngineId; config_hash = $env:OFICINA_CONFIG_HASH; node_ids = @(); status = 'creating' }
    Write-Owner $owner
    $kindCreated = $false
    try {
        Invoke-Tool 'kind' @('create', 'cluster', '--name', $script:Name, '--kubeconfig', $script:Kubeconfig, '--config', $script:Config, '--wait', '180s') | Write-Host
        $kindCreated = $true
        $owner.node_ids = @(Get-ManagedNodes)
        Write-Owner $owner
        if ($owner.node_ids.Count -ne 2) { throw 'Expected exactly two kind nodes.' }
        $owner.status = 'ready'
        Write-Owner $owner
        & (Join-Path $PSScriptRoot 'check.ps1') -Root $script:Root -Name $script:Name
    } catch {
        $creationFailure = $_
        if (!$kindCreated) {
            # kind cleans up its own failed create by default. Never adopt nodes after a failed command:
            # another caller outside these helpers could have created a homonymous cluster.
            if (@(Get-ManagedNodes).Count -eq 0) { Remove-ManagedFiles }
            throw $creationFailure
        }
        # Only a successful create under the name lock establishes ownership.
        if ($owner.node_ids.Count -gt 0) {
            & (Join-Path $PSScriptRoot 'destroy.ps1')
        }
        throw $creationFailure
    }
} finally { $managementLock.ReleaseMutex(); $managementLock.Dispose() }
