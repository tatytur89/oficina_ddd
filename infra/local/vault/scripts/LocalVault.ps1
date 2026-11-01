Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function ConvertTo-PlainText([Security.SecureString]$Value) {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Value)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
}

function Get-LocalVaultToken {
    if (![string]::IsNullOrWhiteSpace($env:VAULT_TOKEN)) { return $env:VAULT_TOKEN }
    $secure = Read-Host 'Vault token (input hidden)' -AsSecureString
    $token = ConvertTo-PlainText $secure
    if ([string]::IsNullOrWhiteSpace($token)) { throw 'A Vault token is required.' }
    return $token
}

function Assert-LocalVaultContext([string]$Kubeconfig, [string]$Context) {
    $kubectl = (Get-Command kubectl -CommandType Application -ErrorAction Stop).Source
    $output = & $kubectl --kubeconfig $Kubeconfig --context $Context config view --minify -o json 2>$null
    if ($LASTEXITCODE -ne 0) { throw 'Could not verify the selected local Kubernetes context.' }
    $configuration = ($output -join [Environment]::NewLine) | ConvertFrom-Json
    $server = $configuration.clusters[0].cluster.server
    if ($server -notmatch '^https://127\.0\.0\.1:\d+$') {
        throw 'Vault scripts only accept a kind API endpoint on 127.0.0.1.'
    }
}

function Start-LocalVaultPortForward([string]$Kubeconfig, [string]$Context) {
    Assert-LocalVaultContext $Kubeconfig $Context
    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
    $listener.Start()
    $port = $listener.LocalEndpoint.Port
    $listener.Stop()

    $kubectl = (Get-Command kubectl -CommandType Application -ErrorAction Stop).Source
    $start = New-Object Diagnostics.ProcessStartInfo
    $start.FileName = $kubectl
    $start.Arguments = '--kubeconfig "' + $Kubeconfig + '" --context "' + $Context +
        '" --namespace vault-system port-forward --address=127.0.0.1 service/vault ' + $port + ':8200'
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $start.EnvironmentVariables.Remove('VAULT_TOKEN') | Out-Null
    $process = [Diagnostics.Process]::Start($start)
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()

    $connected = $false
    $watch = [Diagnostics.Stopwatch]::StartNew()
    while ($watch.Elapsed.TotalSeconds -lt 30) {
        if ($process.HasExited) { break }
        $client = New-Object Net.Sockets.TcpClient
        try {
            $client.Connect('127.0.0.1', $port)
            $connected = $true
            break
        } catch { Start-Sleep -Milliseconds 250 }
        finally { $client.Dispose() }
    }
    if (!$connected) {
        $message = if ($process.HasExited) { $stderr.Result } else { 'Timed out waiting for the local Vault port-forward.' }
        if (!$process.HasExited) { $process.Kill(); $process.WaitForExit() }
        throw "Could not connect to Vault through kubectl port-forward: $message"
    }
    return [pscustomobject]@{ Process = $process; Port = $port; StandardOutput = $stdout; StandardError = $stderr }
}

function Stop-LocalVaultPortForward($Forward) {
    if ($null -ne $Forward -and !$Forward.Process.HasExited) {
        $Forward.Process.Kill()
        $Forward.Process.WaitForExit()
    }
}

function Invoke-LocalVaultApi([string]$BaseUrl, [string]$Token, [string]$Method, [string]$Path, $Body = $null) {
    $parameters = @{
        Uri     = "$BaseUrl/v1/$Path"
        Method  = $Method
        Headers = @{ 'X-Vault-Token' = $Token }
        TimeoutSec = 20
    }
    if ($null -ne $Body) {
        $parameters.ContentType = 'application/json'
        $parameters.Body = $Body | ConvertTo-Json -Depth 10 -Compress
    }
    return Invoke-RestMethod @parameters
}

function Get-LocalVaultCredentials([string]$BaseUrl, [string]$Token) {
    $response = Invoke-LocalVaultApi $BaseUrl $Token 'GET' 'secret/data/oficina-local'
    $secret = $response.data.data
    foreach ($key in @('database_username', 'database_password', 'admin_user', 'admin_password')) {
        if ($key -notin $secret.PSObject.Properties.Name -or [string]::IsNullOrWhiteSpace([string]$secret.$key)) {
            throw "Vault KV secret is missing required key: $key"
        }
        if ([string]$secret.$key -match '[\r\n]') { throw "Vault key '$key' cannot contain line breaks." }
    }
    if ([string]$secret.database_username -cne 'oficina_user') {
        throw 'The local PostgreSQL username must be oficina_user.'
    }
    if ([string]$secret.database_password -cnotmatch '^.{16,}$') {
        throw 'Vault database_password must contain at least 16 characters.'
    }
    if ([string]$secret.admin_password -cnotmatch '^.{12,}$') {
        throw 'Vault admin_password must contain at least 12 characters.'
    }
    return $secret
}

function Publish-LocalKubernetesSecret(
    [string]$Kubeconfig,
    [string]$Context,
    [string]$Namespace,
    [string]$Name,
    [hashtable]$Values
) {
    Assert-LocalVaultContext $Kubeconfig $Context
    $data = @{}
    foreach ($key in $Values.Keys) {
        $data[$key] = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes([string]$Values[$key]))
    }
    $manifest = @{
        apiVersion = 'v1'
        kind       = 'Secret'
        metadata   = @{ name = $Name; namespace = $Namespace }
        type       = 'Opaque'
        data       = $data
    } | ConvertTo-Json -Depth 10 -Compress

    $kubectl = (Get-Command kubectl -CommandType Application -ErrorAction Stop).Source
    $start = New-Object Diagnostics.ProcessStartInfo
    $start.FileName = $kubectl
    $start.Arguments = '--kubeconfig "' + $Kubeconfig + '" --context "' + $Context +
        '" --namespace "' + $Namespace + '" apply -f -'
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardInput = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $start.EnvironmentVariables.Remove('VAULT_TOKEN') | Out-Null
    $process = [Diagnostics.Process]::Start($start)
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    $process.StandardInput.Write($manifest)
    $process.StandardInput.Close()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "kubectl failed to apply Secret '$Name': $($stderr.Result)" }
}

function Read-LocalSecretFile([string]$Path, [string[]]$AllowedKeys) {
    if (!(Test-Path -LiteralPath $Path -PathType Leaf)) { throw "Secret input file not found: $Path" }
    $values = @{}
    foreach ($line in [IO.File]::ReadAllLines($Path)) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith('#')) { continue }
        if ($line -notmatch '^([A-Z_]+)=(.+)$') { throw 'Invalid secret file entry.' }
        $key = $Matches[1]
        if ($key -notin $AllowedKeys -or $values.ContainsKey($key)) { throw "Unknown or duplicate secret file key: $key" }
        $values[$key] = $Matches[2]
    }
    return $values
}
