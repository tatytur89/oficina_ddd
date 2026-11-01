. (Join-Path $PSScriptRoot 'common.ps1')
Initialize-LocalPaths $env:OFICINA_ROOT $env:OFICINA_NAME
$managementLock = Enter-ClusterLock
try {
    $nodes = @(Get-ManagedNodes)
    if (!(Test-Path -LiteralPath $script:Marker)) {
        if ($nodes.Count -gt 0) { throw 'Ownership marker missing; refusing to delete cluster.' }
        Write-Host 'No managed cluster remains.'
        return
    }
    $owner = Read-Owner
    Assert-OwnedNodes $owner $nodes
    Invoke-Tool 'kind' @('delete', 'cluster', '--name', $script:Name, '--kubeconfig', $script:Kubeconfig) | Write-Host
    if (@(Get-ManagedNodes).Count -gt 0) { throw 'Cluster containers remain; preserving ownership files for recovery.' }
    Remove-ManagedFiles
    Write-Host "Removed managed cluster $script:Name."
} finally { $managementLock.ReleaseMutex(); $managementLock.Dispose() }
