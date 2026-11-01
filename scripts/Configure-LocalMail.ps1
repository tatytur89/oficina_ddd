[CmdletBinding()]
param(
    [string]$Namespace = 'oficina-local',
    [string]$SecretFile,
    [switch]$Restart
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if ($Namespace -notmatch '^(oficina-local|oficina-db-test-[a-z0-9]+)$') { throw 'Invalid local namespace.' }
if (!$SecretFile) { $SecretFile = Join-Path $repoRoot 'k8s/local/mail.env' }
$SecretFile = [IO.Path]::GetFullPath($SecretFile)
if (!(Test-Path -LiteralPath $SecretFile -PathType Leaf)) { throw 'Configure mail.env from its example first.' }
. (Join-Path $repoRoot 'infra/local/scripts/common.ps1')
Initialize-LocalPaths ([IO.Path]::GetFullPath((Join-Path $repoRoot 'infra/local'))) 'oficina-local'
& (Join-Path $repoRoot 'infra/local/scripts/check.ps1')
$clusterArgs = @('--kubeconfig', $script:Kubeconfig, '--context', 'kind-oficina-local')
$nsArgs = $clusterArgs + @('--namespace', $Namespace)
function Invoke-MailSecretTool([string[]]$Arguments, [switch]$Json) {
    try {
        $output = Invoke-Tool 'kubectl' $Arguments
        if ($Json) {
            if ([string]::IsNullOrWhiteSpace($output)) { return $null }
            return $output | ConvertFrom-Json
        }
        return $output
    } catch {
        # Kubernetes errors can echo Secret patches, including encoded credentials.
        throw 'SMTP Secret operation failed; private response details were suppressed.'
    }
}
$namespaceInfo = Invoke-Tool 'kubectl' ($clusterArgs + @('get','namespace',$Namespace,'-o','json')) | ConvertFrom-Json
$namespaceUid = $namespaceInfo.metadata.uid
$required = @('APP_NOTIFICATIONS_EMAIL_ENABLED','SMTP_HOST','SMTP_PORT','SMTP_PROTOCOL','SMTP_SSL_ENABLED','SMTP_USERNAME','SMTP_FROM','SMTP_PASSWORD')
$values = @{}
foreach ($line in [IO.File]::ReadAllLines($SecretFile)) {
    if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith('#')) { continue }
    if ($line -notmatch '^([A-Z_]+)=(.+)$') { throw 'Invalid mail.env entry.' }
    $key = $Matches[1]
    $value = $Matches[2]
    if ($key -notin $required -or $values.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($value)) { throw 'Unknown, duplicate or blank mail.env entry.' }
    $values[$key] = $value
}
foreach ($key in $required) { if (!$values.ContainsKey($key)) { throw "Missing mail.env key: $key" } }
if ($values.APP_NOTIFICATIONS_EMAIL_ENABLED -notin @('true','false') -or $values.SMTP_SSL_ENABLED -notin @('true','false')) { throw 'Notification and SSL flags must be true or false.' }
if ($values.SMTP_PORT -notmatch '^\d+$' -or [int]$values.SMTP_PORT -lt 1 -or [int]$values.SMTP_PORT -gt 65535) { throw 'Invalid SMTP port.' }
if ($values.SMTP_PROTOCOL -notin @('smtp','smtps')) { throw 'Invalid SMTP protocol.' }
if ($values.SMTP_PASSWORD.StartsWith('REPLACE_')) { throw 'Replace the app password placeholder locally before configuring SMTP.' }
$existingSecret = Invoke-MailSecretTool ($nsArgs + @('get','secret','oficina-mail-secret','--ignore-not-found','-o','json')) -Json
if ($null -ne $existingSecret) {
    if ($existingSecret.metadata.PSObject.Properties.Name -notcontains 'labels' -or
        $existingSecret.metadata.labels.PSObject.Properties.Name -notcontains 'app.kubernetes.io/part-of' -or
        $existingSecret.metadata.labels.'app.kubernetes.io/part-of' -cne 'oficina-local') {
        throw 'Existing mail Secret is not owned by this project; refusing to replace it.'
    }
}
$tempParent = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$tempRoot = Join-Path $tempParent ('oficina-mail-' + [Guid]::NewGuid().ToString('N'))
try {
    New-Item -ItemType Directory -Path $tempRoot -Force | Out-Null
    $secret = Invoke-MailSecretTool ($nsArgs + @('create','secret','generic','oficina-mail-secret',"--from-env-file=$SecretFile",'--dry-run=client','-o','json')) -Json
    $secret.metadata | Add-Member -NotePropertyName labels -NotePropertyValue @{'app.kubernetes.io/part-of'='oficina-local'} -Force
    $manifest = Join-Path $tempRoot 'mail-secret.json'
    [IO.File]::WriteAllText($manifest, ($secret | ConvertTo-Json -Depth 12))
    $current = Invoke-Tool 'kubectl' ($clusterArgs + @('get','namespace',$Namespace,'-o','json')) | ConvertFrom-Json
    if ($current.metadata.uid -cne $namespaceUid) { throw 'Namespace changed during configuration.' }
    # Output contains only the resource name; never print the rendered Secret.
    Invoke-MailSecretTool ($nsArgs + @('apply','-f',$manifest,'-o','name')) | Write-Host
    if ($Restart) {
        Invoke-Tool 'kubectl' ($nsArgs + @('rollout','restart','deployment/oficina-api')) | Write-Host
        Invoke-Tool 'kubectl' ($nsArgs + @('rollout','status','deployment/oficina-api','--timeout=300s')) | Write-Host
    }
    Write-Host "SMTP runtime Secret configured in namespace $Namespace. Credentials were not written to source files."
} finally {
    if (Test-Path -LiteralPath $tempRoot) {
        $resolved = [IO.Path]::GetFullPath($tempRoot)
        if (!$resolved.StartsWith($tempParent,[StringComparison]::OrdinalIgnoreCase) -or !(Split-Path $resolved -Leaf).StartsWith('oficina-mail-')) { throw 'Unsafe temporary cleanup target.' }
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
