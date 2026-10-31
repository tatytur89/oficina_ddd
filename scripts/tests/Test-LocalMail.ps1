[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$Recipient,
    [string]$Namespace = 'oficina-local',
    [ValidatePattern('^http://127\.0\.0\.1:[0-9]+$')][string]$BaseUrl = 'http://127.0.0.1:8090',
    [string]$IngressHost,
    [string]$ApiSecretFile
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$testRoot = [IO.Path]::GetTempPath()
. (Join-Path $PSScriptRoot 'IntegrationTestTools.ps1')
if ($Namespace -notmatch '^(oficina-local|oficina-db-test-[a-z0-9]+)$') { throw 'Invalid local namespace.' }
if (!$IngressHost) { $IngressHost=if($Namespace -eq 'oficina-local'){'oficina.local'}else{"$Namespace.oficina.local"} }
if ($IngressHost -notmatch '^(oficina\.local|[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.oficina\.local)$') { throw 'Invalid local Ingress host.' }
try { $mailAddress = New-Object Net.Mail.MailAddress($Recipient) } catch { throw 'Invalid recipient address.' }
if ($mailAddress.Address -cne $Recipient) { throw 'Provide a single plain recipient address.' }
if (!$ApiSecretFile) { $ApiSecretFile = Join-Path $repoRoot 'k8s/local/secret.env' }
$credentials = @{}
foreach ($line in [IO.File]::ReadAllLines([IO.Path]::GetFullPath($ApiSecretFile))) {
    if ($line -match '^(ADMIN_USER|ADMIN_PASSWORD)=(.+)$') { $credentials[$Matches[1]] = $Matches[2] }
}
if (!$credentials.ContainsKey('ADMIN_USER') -or !$credentials.ContainsKey('ADMIN_PASSWORD')) { throw 'API credentials are missing from the local Secret file.' }
$headers = @{Host=$IngressHost}
function Invoke-MailTestApi([string]$Path, [string]$Method='GET', [object]$Body=$null) {
    $arguments = @{Uri=($BaseUrl+$Path);Method=$Method;Headers=$headers;TimeoutSec=45}
    if ($null -ne $Body) { $arguments.ContentType='application/json';$arguments.Body=($Body|ConvertTo-Json -Depth 6 -Compress) }
    try { return Invoke-RestMethod @arguments } catch { throw "Mail test API request failed: $Method $Path. Private response details were suppressed." }
}
$clusterArgs = @('--kubeconfig',(Join-Path $repoRoot 'infra/local/.generated/kubeconfig'),'--context','kind-oficina-local','-n',$Namespace)
$info = Invoke-MailTestApi '/api/v1/info'
$podNamespace = Invoke-TestTool 'kubectl' ($clusterArgs+@('get','pod',$info.dados.pod,'-o','jsonpath={.metadata.namespace}'))
if ($podNamespace.Code -ne 0 -or $podNamespace.Output.Trim() -cne $Namespace) { throw 'The Ingress host did not route to the selected namespace.' }
$enabled = Invoke-TestTool 'kubectl' ($clusterArgs+@('get','secret','oficina-mail-secret','-o','jsonpath={.data.APP_NOTIFICATIONS_EMAIL_ENABLED}'))
if ($enabled.Code -ne 0 -or [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($enabled.Output.Trim())) -cne 'true') {
    throw 'Configure and enable the runtime SMTP Secret before sending a real email test.'
}
$login = Invoke-MailTestApi '/api/v1/auth/login' 'POST' @{usuario=$credentials.ADMIN_USER;senha=$credentials.ADMIN_PASSWORD}
$headers.Authorization = 'Bearer '+$login.dados.token

# Generate valid, unique CPF/plate fixtures; never change an existing customer.
$random = New-Object Random
$digits = @();for($index=0;$index -lt 9;$index++){ $digits += $random.Next(0,10) }
foreach($length in @(9,10)) {
    $sum=0;for($index=0;$index -lt $length;$index++){ $sum += $digits[$index]*($length+1-$index) }
    $digit=($sum*10)%11;if($digit -eq 10){$digit=0};$digits += $digit
}
$document = $digits -join ''
$letters='ABCDEFGHIJKLMNOPQRSTUVWXYZ'
$plate = ''
foreach($index in 0..2){ $plate += $letters[$random.Next(0,26)] }
$plate += $random.Next(0,10).ToString()+$letters[$random.Next(0,26)]+$random.Next(10,100).ToString()
$customer = Invoke-MailTestApi '/api/v1/clientes' 'POST' @{nome='Teste de notificacao FIAP';documento=$document;email=$Recipient;telefone='11999999999'}
$vehicle = Invoke-MailTestApi '/api/v1/veiculos' 'POST' @{marca='FIAP';modelo='Teste SMTP';ano=2026;placa=$plate;clienteId=$customer.dados.id}
$order = Invoke-MailTestApi '/api/v1/ordens-servico' 'POST' @{clienteId=$customer.dados.id;veiculoId=$vehicle.dados.id;observacoes='Teste autorizado de notificacao SMTP';servicos=@();pecas=@()}
$orderId = [long]$order.dados.id
$saved = Invoke-MailTestApi ("/api/v1/ordens-servico/$orderId")
if ($saved.dados.status -cne 'RECEBIDA') { throw 'The test OS was not persisted in the expected status.' }
$logs = Invoke-TestTool 'kubectl' ($clusterArgs+@('logs','-l','app.kubernetes.io/name=oficina-api','--all-containers=true','--since=3m','--tail=-1'))
if ($logs.Code -ne 0 -or $logs.Output -notmatch "status_os_email_sent osId=$orderId status=RECEBIDA") {
    throw "OS $orderId was saved, but SMTP acceptance was not confirmed. No automatic resend was attempted."
}
Write-Host "PASS: OS $orderId persisted as RECEBIDA and notification accepted by SMTP for $Recipient."
Write-Host 'The test creates new customer/vehicle/OS records and preserves them for evaluation; it does not modify existing OS records.'
