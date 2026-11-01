[CmdletBinding()]
param(
    [string]$Kubeconfig = (Join-Path $PSScriptRoot '../../.generated/kubeconfig'),
    [string]$Context = 'kind-oficina-local'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'LocalVault.ps1')
$Kubeconfig = [IO.Path]::GetFullPath($Kubeconfig)
$token = Get-LocalVaultToken
$forward = $null
try {
    $forward = Start-LocalVaultPortForward $Kubeconfig $Context
    $baseUrl = "http://127.0.0.1:$($forward.Port)"
    $mounts = Invoke-LocalVaultApi $baseUrl $token 'GET' 'sys/mounts'
    if (!$mounts.PSObject.Properties['secret/']) {
        Invoke-LocalVaultApi $baseUrl $token 'POST' 'sys/mounts/secret' @{
            type = 'kv'
            options = @{ version = '2' }
        } | Out-Null
    } elseif ($mounts.'secret/'.options.version -ne '2') {
        throw 'Vault mount secret/ exists but is not KV version 2.'
    }

    $databaseUser = Read-Host 'PostgreSQL username'
    $databasePassword = ConvertTo-PlainText (Read-Host 'PostgreSQL password (minimum 16 characters)' -AsSecureString)
    $adminUser = Read-Host 'Local admin username'
    $adminPassword = ConvertTo-PlainText (Read-Host 'Local admin password (minimum 12 characters)' -AsSecureString)
    $secret = @{
        data = @{
            database_username = $databaseUser
            database_password = $databasePassword
            admin_user        = $adminUser
            admin_password    = $adminPassword
        }
    }
    if ([string]::IsNullOrWhiteSpace($databaseUser) -or $databasePassword.Length -lt 16 -or
        [string]::IsNullOrWhiteSpace($adminUser) -or $adminPassword.Length -lt 12) {
        throw 'Vault credentials are blank or do not meet the minimum password lengths.'
    }
    if ($databaseUser -cne 'oficina_user') { throw 'The project local database username must be oficina_user.' }
    Invoke-LocalVaultApi $baseUrl $token 'POST' 'secret/data/oficina-local' $secret | Out-Null
    Write-Host 'Local database and admin credentials were stored in Vault KV v2.'
} finally {
    Stop-LocalVaultPortForward $forward
}
