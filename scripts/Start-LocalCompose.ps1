[CmdletBinding()]
param(
    [string]$Kubeconfig = (Join-Path $PSScriptRoot '../infra/local/.generated/kubeconfig'),
    [string]$Context = 'kind-oficina-local'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
. (Join-Path $repoRoot 'infra/local/vault/scripts/LocalVault.ps1')
$Kubeconfig = [IO.Path]::GetFullPath($Kubeconfig)
$token = Get-LocalVaultToken
$forward = $null
$environmentKeys = @('SPRING_DATASOURCE_USERNAME', 'SPRING_DATASOURCE_PASSWORD', 'ADMIN_USER', 'ADMIN_PASSWORD')
$previousEnvironment = @{}
$previousVaultToken = [Environment]::GetEnvironmentVariable('VAULT_TOKEN', 'Process')

try {
    $forward = Start-LocalVaultPortForward $Kubeconfig $Context
    $credentials = Get-LocalVaultCredentials "http://127.0.0.1:$($forward.Port)" $token
    $environment = @{
        SPRING_DATASOURCE_USERNAME = $credentials.database_username
        SPRING_DATASOURCE_PASSWORD = $credentials.database_password
        ADMIN_USER                 = $credentials.admin_user
        ADMIN_PASSWORD             = $credentials.admin_password
    }
    foreach ($key in $environmentKeys) {
        $previousEnvironment[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
        [Environment]::SetEnvironmentVariable($key, [string]$environment[$key], 'Process')
    }
    [Environment]::SetEnvironmentVariable('VAULT_TOKEN', $null, 'Process')

    Push-Location $repoRoot
    try {
        & docker compose up --build
        if ($LASTEXITCODE -ne 0) { throw "docker compose up failed with exit code $LASTEXITCODE." }
    } finally { Pop-Location }
} finally {
    [Environment]::SetEnvironmentVariable('VAULT_TOKEN', $previousVaultToken, 'Process')
    foreach ($key in $environmentKeys) {
        [Environment]::SetEnvironmentVariable($key, $previousEnvironment[$key], 'Process')
    }
    Stop-LocalVaultPortForward $forward
}
