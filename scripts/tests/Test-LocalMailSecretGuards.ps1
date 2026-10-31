[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$tempParent = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$testRoot = Join-Path $tempParent ('oficina-mail-guards-' + [Guid]::NewGuid().ToString('N'))
$powershell = (Get-Command powershell.exe -CommandType Application | Select-Object -First 1).Source
$checks = 0
function Assert-Check([bool]$Condition, [string]$Message) {
    if (!$Condition) { throw "FAILED: $Message" }
    $script:checks++
    Write-Host "PASS: $Message"
}

try {
    New-Item -ItemType Directory -Path (Join-Path $testRoot 'scripts') -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $testRoot 'infra/local/scripts') -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $repoRoot 'scripts/Configure-LocalMail.ps1') -Destination (Join-Path $testRoot 'scripts/Configure-LocalMail.ps1')
    # Every tool operation is a PowerShell fake. No Docker, Kubernetes or SMTP executable runs.
    $mockCommon = @'
function Initialize-LocalPaths([string]$Root, [string]$Name) { $script:Kubeconfig = 'mock-kubeconfig' }
function Invoke-Tool([string]$Tool, [string[]]$Arguments) {
    $operation = if ($Arguments -contains 'namespace') { 'namespace' }
        elseif ($Arguments -contains 'apply') { 'apply' }
        elseif ($Arguments -contains 'create') { 'create' }
        elseif ($Arguments -contains 'secret') { 'get' }
        else { throw 'Unexpected mock operation.' }
    Add-Content -LiteralPath $env:MAIL_GUARD_CALLS -Value $operation
    if ($operation -eq $env:MAIL_GUARD_FAILURE) {
        $raw = 'SYNTHETIC_MAIL_PASSWORD_SENTINEL'
        $encoded = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($raw))
        throw ('mock private details: ' + $raw + ' {"data":{"SMTP_PASSWORD":"' + $encoded + '"}}')
    }
    if (($operation + '-json') -eq $env:MAIL_GUARD_FAILURE) {
        return '{malformed JSON SYNTHETIC_MAIL_PASSWORD_SENTINEL'
    }
    switch ($operation) {
        'namespace' { return '{"metadata":{"uid":"mock-namespace-uid"}}' }
        'get' { return '{"metadata":{"labels":{"app.kubernetes.io/part-of":"oficina-local"}}}' }
        'create' { return '{"apiVersion":"v1","kind":"Secret","metadata":{"name":"oficina-mail-secret"},"data":{"SMTP_PASSWORD":"U1lOVEhFVElD"}}' }
        'apply' { return 'secret/oficina-mail-secret configured' }
    }
}
'@
    [IO.File]::WriteAllText((Join-Path $testRoot 'infra/local/scripts/common.ps1'), $mockCommon)
    [IO.File]::WriteAllText((Join-Path $testRoot 'infra/local/scripts/check.ps1'), '# Deliberately no external operation.')
    $secretFile = Join-Path $testRoot 'mail.env'
    $fixture = @'
APP_NOTIFICATIONS_EMAIL_ENABLED=true
SMTP_HOST=smtp.invalid
SMTP_PORT=465
SMTP_PROTOCOL=smtps
SMTP_SSL_ENABLED=true
SMTP_USERNAME=sender@example.invalid
SMTP_FROM=sender@example.invalid
SMTP_PASSWORD=SYNTHETIC_MAIL_PASSWORD_SENTINEL
'@
    [IO.File]::WriteAllText($secretFile, $fixture)
    $beforeHash = (Get-FileHash -LiteralPath $secretFile -Algorithm SHA256).Hash
    $encodedSentinel = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('SYNTHETIC_MAIL_PASSWORD_SENTINEL'))
    foreach ($mode in @('get', 'create', 'apply', 'get-json', 'create-json', 'none')) {
        $callFile = Join-Path $testRoot ($mode + '-calls.txt')
        $start = New-Object Diagnostics.ProcessStartInfo
        $start.FileName = $powershell
        $start.Arguments = '-NoProfile -ExecutionPolicy RemoteSigned -File "' + (Join-Path $testRoot 'scripts/Configure-LocalMail.ps1') + '" -SecretFile "' + $secretFile + '"'
        $start.UseShellExecute = $false
        $start.RedirectStandardOutput = $true
        $start.RedirectStandardError = $true
        $start.EnvironmentVariables['MAIL_GUARD_FAILURE'] = $mode
        $start.EnvironmentVariables['MAIL_GUARD_CALLS'] = $callFile
        $process = [Diagnostics.Process]::Start($start)
        $outputTask = $process.StandardOutput.ReadToEndAsync()
        $errorTask = $process.StandardError.ReadToEndAsync()
        if (!$process.WaitForExit(30000)) { $process.Kill(); throw 'Mock helper exceeded its deadline.' }
        $captured = $outputTask.Result + $errorTask.Result
        $calls = @(Get-Content -LiteralPath $callFile)
        Assert-Check (!$captured.Contains('SYNTHETIC_MAIL_PASSWORD_SENTINEL') -and !$captured.Contains($encodedSentinel)) "$mode does not expose raw or encoded credentials"
        Assert-Check (!$captured.Contains('mock private details') -and !$captured.Contains('SMTP_PASSWORD')) "$mode does not expose private error payloads"
        if ($mode -eq 'none') {
            Assert-Check ($process.ExitCode -eq 0) 'Successful mocked Secret configuration completes'
            Assert-Check (($calls -join ',') -eq 'namespace,get,create,namespace,apply') 'Success follows namespace checks and all Secret operations'
            Assert-Check ($captured.Contains('secret/oficina-mail-secret configured')) 'Success reports only the resource name'
        } else {
            Assert-Check ($process.ExitCode -ne 0 -and $captured.Contains('SMTP Secret operation failed')) "$mode fails with a sanitized diagnostic"
            Assert-Check ($calls[-1] -eq ($mode -replace '-json$', '')) "$mode failure stops subsequent operations"
        }
        $process.Dispose()
    }
    Assert-Check ((Get-FileHash -LiteralPath $secretFile -Algorithm SHA256).Hash -ceq $beforeHash) 'The credentials fixture remains unchanged'
    Write-Host "Local SMTP Secret guards passed: $checks checks. No real tools or SMTP were used."
} finally {
    if (Test-Path -LiteralPath $testRoot) {
        $resolved = [IO.Path]::GetFullPath($testRoot)
        if (!$resolved.StartsWith($tempParent, [StringComparison]::OrdinalIgnoreCase) -or !(Split-Path $resolved -Leaf).StartsWith('oficina-mail-guards-')) { throw 'Unsafe test cleanup path.' }
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
