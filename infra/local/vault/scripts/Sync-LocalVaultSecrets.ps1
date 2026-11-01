[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidatePattern('^(oficina-local|oficina-db-test-[a-z0-9]+)$')][string]$Namespace,
    [string]$Kubeconfig = (Join-Path $PSScriptRoot '../../.generated/kubeconfig'),
    [string]$Context = 'kind-oficina-local',
    [switch]$DatabaseOnly,
    [string]$CredentialFile,
    [string]$JwtFile,
    [string]$ApplicationSecretFile
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'LocalVault.ps1')
$Kubeconfig = [IO.Path]::GetFullPath($Kubeconfig)
$vaultForward = $null

try {
    if ($CredentialFile) {
        if ($Namespace -notmatch '^oficina-db-test-') { throw 'CredentialFile is allowed only for disposable test namespaces.' }
        $allowed = @('SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME', 'SPRING_DATASOURCE_PASSWORD', 'JWT_SECRET', 'ADMIN_USER', 'ADMIN_PASSWORD')
        $fileValues = Read-LocalSecretFile $CredentialFile $allowed
        foreach ($key in @('SPRING_DATASOURCE_USERNAME', 'SPRING_DATASOURCE_PASSWORD')) {
            if (!$fileValues.ContainsKey($key)) { throw "Test credential file is missing $key." }
        }
        if ($fileValues.SPRING_DATASOURCE_USERNAME -cne 'oficina_user') { throw 'The test database username must be oficina_user.' }
        $adminUser = if ($fileValues.ContainsKey('ADMIN_USER')) { $fileValues.ADMIN_USER } else { '' }
        $adminPassword = if ($fileValues.ContainsKey('ADMIN_PASSWORD')) { $fileValues.ADMIN_PASSWORD } else { '' }
        $credentials = @{
            database_username = $fileValues.SPRING_DATASOURCE_USERNAME
            database_password = $fileValues.SPRING_DATASOURCE_PASSWORD
            admin_user        = $adminUser
            admin_password    = $adminPassword
        }
        if (!$DatabaseOnly -and (!$credentials.admin_user -or !$credentials.admin_password)) {
            throw 'Test credential file is missing admin credentials.'
        }
    } else {
        $token = Get-LocalVaultToken
        $vaultForward = Start-LocalVaultPortForward $Kubeconfig $Context
        $credentials = Get-LocalVaultCredentials "http://127.0.0.1:$($vaultForward.Port)" $token
    }
    if ([string]$credentials.database_password -notmatch '^.{16,}$') {
        throw 'Database password must contain at least 16 characters.'
    }
    if ([string]$credentials.database_username -cne 'oficina_user') {
        throw 'The local PostgreSQL username must be oficina_user.'
    }

    Publish-LocalKubernetesSecret $Kubeconfig $Context $Namespace 'postgres-secret' @{
        POSTGRES_DB       = 'oficina_db'
        POSTGRES_USER     = $credentials.database_username
        POSTGRES_PASSWORD = $credentials.database_password
    }

    if (!$DatabaseOnly) {
        if ($CredentialFile) {
            foreach ($key in @('ADMIN_USER', 'ADMIN_PASSWORD', 'JWT_SECRET')) {
                if (!$fileValues.ContainsKey($key)) { throw "Test credential file is missing $key." }
            }
            $jwt = $fileValues.JWT_SECRET
        } else {
            if (!$JwtFile) { throw 'Provide JwtFile for the API secret.' }
            $jwtValues = Read-LocalSecretFile $JwtFile @('JWT_SECRET')
            $jwt = $jwtValues.JWT_SECRET
        }
        if ([string]::IsNullOrWhiteSpace($jwt) -or $jwt.Length -lt 32) { throw 'JWT_SECRET must contain at least 32 characters.' }
        if ($jwt -match '[\r\n]') { throw 'JWT_SECRET cannot contain line breaks.' }

        if ($ApplicationSecretFile) {
            $applicationValues = @(
                'SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/oficina_db',
                "SPRING_DATASOURCE_USERNAME=$($credentials.database_username)",
                "SPRING_DATASOURCE_PASSWORD=$($credentials.database_password)",
                "JWT_SECRET=$jwt",
                "ADMIN_USER=$($credentials.admin_user)",
                "ADMIN_PASSWORD=$($credentials.admin_password)"
            )
            [IO.File]::WriteAllLines([IO.Path]::GetFullPath($ApplicationSecretFile), $applicationValues, (New-Object Text.UTF8Encoding($false)))
        }
    }
    Write-Host "Vault credentials synchronized to namespace '$Namespace'."
} finally {
    Stop-LocalVaultPortForward $vaultForward
}
