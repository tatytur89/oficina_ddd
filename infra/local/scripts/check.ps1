param(
    [string]$Root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..')),
    [string]$Name = 'oficina-local'
)
. (Join-Path $PSScriptRoot 'common.ps1')
Initialize-LocalPaths $Root $Name
$owner = Read-Owner
$nodes = @(Get-ManagedNodes)
Assert-OwnedNodes $owner $nodes
if ($nodes.Count -ne 2 -or $owner.status -ne 'ready') { throw 'Managed cluster is incomplete or absent. Terraform state alone does not prove runtime health.' }
$argsForCluster = @('--kubeconfig', $script:Kubeconfig, '--context', $script:Context)
Invoke-Tool 'kubectl' ($argsForCluster + @('wait', '--for=condition=Ready', 'nodes', '--all', '--timeout=180s')) | Write-Host
$nodeData = Invoke-Tool 'kubectl' ($argsForCluster + @('get', 'nodes', '-o', 'json')) | ConvertFrom-Json
if (@($nodeData.items).Count -ne 2) { throw 'Unexpected Kubernetes node count.' }
foreach ($node in $nodeData.items) {
    if ($node.status.nodeInfo.kubeletVersion -ne 'v1.34.11') { throw 'Unexpected Kubernetes node version.' }
}
$ready = Invoke-Tool 'kubectl' ($argsForCluster + @('get', '--raw=/readyz'))
if ($ready.Trim() -ne 'ok') { throw 'Kubernetes API is not ready.' }
Write-Host "Cluster ${script:Name}: two Ready nodes, Kubernetes v1.34.11, API ready."
