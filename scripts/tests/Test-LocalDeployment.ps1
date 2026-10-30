[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][ValidatePattern('^oficina-ddd:local-[a-z0-9-]+$')][string]$Image,
    [Parameter(Mandatory=$true)][string]$ExpectedCommit
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$tempParent = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$testRoot = Join-Path $tempParent ('oficina-deployment-smoke-' + [Guid]::NewGuid().ToString('N'))
$moduleRoot = Join-Path $testRoot 'infra/local/database'
$namespace = 'oficina-db-test-' + [Guid]::NewGuid().ToString('N').Substring(0,10)
$kubeconfig = [IO.Path]::GetFullPath((Join-Path $repoRoot 'infra/local/.generated/kubeconfig'))
$clusterArgs = @('--kubeconfig',$kubeconfig,'--context','kind-oficina-local')
$nsArgs = $clusterArgs + @('--namespace',$namespace)
$namespaceUid = $null
$applyAttempted = $false
$cleanupSafe = $true
$failure = $null
$cleanupFailure = $null
$portForward = $null
$checks = New-Object System.Collections.Generic.List[string]
. (Join-Path $PSScriptRoot 'IntegrationTestTools.ps1')

function Assert-Check([bool]$Condition,[string]$Message) {
    if (!$Condition) { throw "FAILED: $Message" }
    $checks.Add($Message)
    Write-Host "PASS: $Message"
}

function Get-NamespaceUid {
    $value = Assert-TestTool 'kubectl' ($clusterArgs + @('get','namespace',$namespace,'--ignore-not-found','-o','json'))
    if ([string]::IsNullOrWhiteSpace($value)) { return $null }
    return ($value | ConvertFrom-Json).metadata.uid
}

function Get-ExternalState {
    return [pscustomobject]@{
        DockerContext=(Assert-TestTool 'docker' @('context','show')).Trim()
        DockerConfig=Get-TestFileHash (Join-Path $env:USERPROFILE '.docker/config.json')
        Containers=((Assert-TestTool 'docker' @('ps','-aq','--no-trunc')) -split '\r?\n' | Sort-Object) -join ','
        KubeConfig=Get-TestFileHash (Join-Path $env:USERPROFILE '.kube/config')
        PrivateKubeConfig=Get-TestFileHash $kubeconfig
        KubeContexts=(Assert-TestTool 'kubectl' @('config','get-contexts','-o','name')).Trim()
        Namespaces=(Assert-TestTool 'kubectl' ($clusterArgs + @('get','namespaces','-o','jsonpath={range .items[*]}{.metadata.name}:{.metadata.uid}{"\n"}{end}'))).Trim()
        UserPath=[Environment]::GetEnvironmentVariable('Path','User')
        MachinePath=[Environment]::GetEnvironmentVariable('Path','Machine')
    }
}

function Invoke-DatabaseTerraform([string[]]$Arguments,[int]$TimeoutSeconds=600) {
    return Assert-TestTool 'terraform' (@("-chdir=$moduleRoot")+$Arguments) $TimeoutSeconds
}

function Invoke-Deploy {
    return Invoke-TestTool 'powershell.exe' @('-NoProfile','-ExecutionPolicy','RemoteSigned','-File',
        (Join-Path $repoRoot 'scripts/Deploy-Local.ps1'),'-Namespace',$namespace,'-SecretFile',$secretFile,'-Image',$Image) 600
}

function Invoke-Http([string]$Path,[string]$Method='GET',[object]$Body=$null,[string]$Token='') {
    $arguments=@{Uri=($baseUrl+$Path); Method=$Method; UseBasicParsing=$true; TimeoutSec=60}
    if ($null -ne $Body) { $arguments.ContentType='application/json'; $arguments.Body=($Body|ConvertTo-Json -Depth 12 -Compress) }
    if ($Token) { $arguments.Headers=@{Authorization="Bearer $Token"} }
    try {
        $response=Invoke-WebRequest @arguments
        $code=[int]$response.StatusCode
        $content=$response.Content
    } catch [System.Net.WebException] {
        if ($null -eq $_.Exception.Response) { throw }
        $code=[int]$_.Exception.Response.StatusCode
        $reader=New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())
        try { $content=$reader.ReadToEnd() } finally { $reader.Dispose() }
    }
    # Windows PowerShell may return vendor JSON (Actuator) as bytes rather than text.
    if ($content -is [byte[]]) { $content=[Text.Encoding]::UTF8.GetString($content) }
    $json=$null
    if ($content -match '^\s*[\{\[]') { $json=$content|ConvertFrom-Json }
    return [pscustomobject]@{Code=$code; Json=$json; Content=$content}
}

function Assert-Http([string]$Path,[int]$Status,[string]$Method='GET',[object]$Body=$null,[string]$Token='') {
    $response=Invoke-Http $Path $Method $Body $Token
    Assert-Check ($response.Code -eq $Status) "$Method $Path returns $Status"
    return $response
}

function Wait-Health([int]$Expected,[int]$TimeoutSeconds=90) {
    $watch=[Diagnostics.Stopwatch]::StartNew()
    while ($watch.Elapsed.TotalSeconds -lt $TimeoutSeconds) {
        try { $response=Invoke-Http '/actuator/health/readiness'; if($response.Code -eq $Expected){return $response} } catch {}
        Start-Sleep -Seconds 1
    }
    throw "Readiness did not reach HTTP $Expected."
}

$before=Get-ExternalState
try {
    Assert-Check ($null -eq (Get-NamespaceUid)) 'Deployment smoke namespace does not already exist'
    $created=Assert-TestTool 'kubectl' ($clusterArgs+@('create','namespace',$namespace,'-o','json'))|ConvertFrom-Json
    $namespaceUid=$created.metadata.uid
    New-Item -ItemType Directory -Path $moduleRoot -Force|Out-Null
    foreach($file in Get-ChildItem -LiteralPath (Join-Path $repoRoot 'infra/local/database') -File){
        if($file.Extension -eq '.tf' -or $file.Name -eq '.terraform.lock.hcl'){Copy-Item -LiteralPath $file.FullName -Destination $moduleRoot}
    }
    $migrationRoot=Join-Path $testRoot 'src/main/resources/db/migration'
    New-Item -ItemType Directory -Path $migrationRoot -Force|Out-Null
    Copy-Item -Path (Join-Path $repoRoot 'src/main/resources/db/migration/*.sql') -Destination $migrationRoot
    New-Item -ItemType Directory -Path (Join-Path $testRoot 'database') -Force|Out-Null
    Copy-Item -LiteralPath (Join-Path $repoRoot 'database/migrate.sh') -Destination (Join-Path $testRoot 'database')
    $password='db-smoke-'+[Guid]::NewGuid().ToString('N')
    $admin='admin-smoke'
    $adminPassword='admin-smoke-'+[Guid]::NewGuid().ToString('N')
    $variables=[pscustomobject]@{namespace=$namespace;database_password=$password;kubeconfig_path=$kubeconfig;context='kind-oficina-local'}
    [IO.File]::WriteAllText((Join-Path $moduleRoot 'smoke.auto.tfvars.json'),($variables|ConvertTo-Json))
    $secretFile=Join-Path $testRoot 'application-secret.env'
    [IO.File]::WriteAllText($secretFile,(@(
        'SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/oficina_db',
        'SPRING_DATASOURCE_USERNAME=oficina_user',
        "SPRING_DATASOURCE_PASSWORD=$password",
        ('JWT_SECRET=smoke-jwt-'+[Guid]::NewGuid().ToString('N')),
        "ADMIN_USER=$admin", "ADMIN_PASSWORD=$adminPassword") -join "`n"))
    $preflight=Invoke-Deploy
    Assert-Check ($preflight.Code -ne 0 -and ($preflight.Output+$preflight.Error) -match '(?i)postgres|database|statefulset|migration') 'Deploy refuses to apply the API before its database/schema is ready'
    $absent=Assert-TestTool 'kubectl' ($nsArgs+@('get','deployment','oficina-api','--ignore-not-found','-o','name'))
    Assert-Check ([string]::IsNullOrWhiteSpace($absent)) 'Failed database preflight leaves no application Deployment'
    Invoke-DatabaseTerraform @('init','-input=false','-no-color')|Out-Null
    Invoke-DatabaseTerraform @('validate','-no-color')|Out-Null
    Invoke-DatabaseTerraform @('plan','-input=false','-no-color','-out=smoke.tfplan')|Out-Null
    $applyAttempted=$true
    $cleanupSafe=$false
    Invoke-DatabaseTerraform @('apply','-input=false','-no-color','-auto-approve','smoke.tfplan')|Out-Null
    $deployment=Invoke-Deploy
    if($deployment.Code -ne 0){throw ('Deployment failed: '+($deployment.Output+$deployment.Error).Replace($password,'<redacted>').Replace($adminPassword,'<redacted>'))}
    Assert-Check $true 'Production deployment script applies the prebuilt image and completes rollout'
    $pods=Assert-TestTool 'kubectl' ($nsArgs+@('get','pods','-l','app.kubernetes.io/name=oficina-api','-o','json'))|ConvertFrom-Json
    Assert-Check (@($pods.items).Count -eq 1) 'Deployment runs exactly one local API pod'
    Assert-Check (@($pods.items[0].status.conditions|Where-Object{$_.type -eq 'Ready' -and $_.status -eq 'True'}).Count -eq 1) 'API pod is Ready'
    $configMapName=($pods.items[0].spec.containers[0].envFrom|Where-Object{$_.PSObject.Properties.Name -contains 'configMapRef'}).configMapRef.name
    $config=Assert-TestTool 'kubectl' ($nsArgs+@('get','configmap',$configMapName,'-o','json'))|ConvertFrom-Json
    Assert-Check ($config.data.SPRING_PROFILES_ACTIVE -eq 'lab') 'API uses only the local lab profile'
    Assert-Check ($config.data.MANAGEMENT_ENDPOINT_HEALTH_GROUP_READINESS_INCLUDE -match 'db') 'Readiness group explicitly includes the database'
    Assert-Check ($pods.items[0].spec.containers[0].image -eq $Image -and $pods.items[0].spec.containers[0].imagePullPolicy -eq 'Never') 'API uses the exact prebuilt local image without registry pulls'
    $listener=New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback,0)
    $listener.Start();$port=$listener.LocalEndpoint.Port;$listener.Stop()
    $portForward=Start-TestTool 'kubectl' ($nsArgs+@('port-forward','--address=127.0.0.1','service/oficina-api',"${port}:80"))
    $baseUrl="http://127.0.0.1:$port"
    $ready=Wait-Health 200
    Assert-Check ($ready.Json.status -eq 'UP') 'API readiness is UP over the private port-forward'
    Assert-Check ((Assert-Http '/actuator/health/liveness' 200).Json.status -eq 'UP') 'API liveness is UP'
    Assert-Http '/swagger-ui/index.html' 200|Out-Null
    $info=Assert-Http '/api/v1/info' 200
    Assert-Check ($info.Json.dados.commit -eq $ExpectedCommit) 'Running application reports the expected source commit'
    $internalInfo=Assert-TestTool 'kubectl' ($nsArgs+@('exec','postgres-0','--','wget','-q','-O','-','http://oficina-api:80/api/v1/info'))|ConvertFrom-Json
    Assert-Check ($internalInfo.dados.commit -eq $ExpectedCommit) 'Service DNS and cluster networking route to the expected API image'
    Assert-Http '/api/v1/ordens-servico' 403|Out-Null
    Assert-Http '/api/v1/auth/login' 401 'POST' @{usuario=$admin;senha='incorrect-smoke-password'}|Out-Null
    $token=(Assert-Http '/api/v1/auth/login' 200 'POST' @{usuario=$admin;senha=$adminPassword}).Json.dados.token
    Assert-Check (![string]::IsNullOrWhiteSpace($token)) 'Login issues a usable JWT'
    $cliente=(Assert-Http '/api/v1/clientes' 201 'POST' @{nome='Cliente smoke';documento='12345678909';email='smoke@example.com';telefone='11999999999'} $token).Json.dados
    $veiculo=(Assert-Http '/api/v1/veiculos' 201 'POST' @{marca='Toyota';modelo='Corolla';ano=2022;placa='ABC1D23';clienteId=$cliente.id} $token).Json.dados
    $servico=(Assert-Http '/api/v1/servicos' 201 'POST' @{nome='Revisao smoke';descricao='Revisao';preco=150.25;tipo='REVISAO';tempoEstimadoMinutos=60} $token).Json.dados
    $peca=(Assert-Http '/api/v1/pecas' 201 'POST' @{nome='Filtro';descricao='Filtro smoke';codigo='FSMOKE';preco=45.90;quantidadeEstoque=10;estoqueMinimo=2} $token).Json.dados
    $os=(Assert-Http '/api/v1/ordens-servico' 201 'POST' @{clienteId=$cliente.id;veiculoId=$veiculo.id;servicos=@(@{servicoId=$servico.id;quantidade=2});pecas=@(@{pecaId=$peca.id;quantidade=3})} $token).Json.dados
    Assert-Check ($os.status -eq 'RECEBIDA' -and [decimal]$os.valorTotal -eq [decimal]438.20) 'OS opens RECEBIDA with requested items and the exact budget total'
    $base='/api/v1/ordens-servico/'+$os.id
    Assert-Http ($base+'/status/EM_DIAGNOSTICO') 200 'PATCH' $null $token|Out-Null
    $orcamento=(Assert-Http ($base+'/orcamento') 200 'PATCH' $null $token).Json.dados
    Assert-Http ($base+'/status/EM_EXECUCAO') 400 'PATCH' $null $token|Out-Null
    Assert-Http ($base+'/decisao-orcamento') 200 'POST' @{chave=$os.chaveAcesso;versaoOrcamento=$orcamento.versaoOrcamento;aprovado=$false}|Out-Null
    Assert-Check ((Assert-Http ('/api/v1/pecas/'+$peca.id) 200 'GET' $null $token).Json.dados.quantidadeEstoque -eq 10) 'Budget refusal does not consume stock'
    $novo=(Assert-Http ($base+'/orcamento') 200 'PATCH' $null $token).Json.dados
    Assert-Http ($base+'/decisao-orcamento') 400 'POST' @{chave=$os.chaveAcesso;versaoOrcamento=$orcamento.versaoOrcamento;aprovado=$true}|Out-Null
    $decisao=@{chave=$os.chaveAcesso;versaoOrcamento=$novo.versaoOrcamento;aprovado=$true}
    Assert-Http ($base+'/decisao-orcamento') 200 'POST' $decisao|Out-Null
    Assert-Http ($base+'/decisao-orcamento') 200 'POST' $decisao|Out-Null
    Assert-Check ((Assert-Http ('/api/v1/pecas/'+$peca.id) 200 'GET' $null $token).Json.dados.quantidadeEstoque -eq 7) 'Repeated approval consumes requested stock only once'
    Assert-Http ($base+'/status/FINALIZADA') 200 'PATCH' $null $token|Out-Null
    Assert-Http ($base+'/status/ENTREGUE') 200 'PATCH' $null $token|Out-Null
    $fila=(Assert-Http '/api/v1/ordens-servico' 200 'GET' $null $token).Json.dados
    Assert-Check (@($fila|Where-Object{$_.id -eq $os.id}).Count -eq 0) 'Delivered OS is absent from the operational queue'
    Assert-Check ((Assert-Http $base 200 'GET' $null $token).Json.dados.status -eq 'ENTREGUE') 'Delivered OS remains available in history'
    try {
        Assert-TestTool 'kubectl' ($nsArgs+@('scale','statefulset/postgres','--replicas=0'))|Out-Null
        Assert-TestTool 'kubectl' ($nsArgs+@('wait','--for=delete','pod/postgres-0','--timeout=120s')) 140|Out-Null
        Assert-Check ((Wait-Health 503).Json.status -ne 'UP') 'Readiness fails when the disposable database stops'
        Assert-Check ((Assert-Http '/actuator/health/liveness' 200).Json.status -eq 'UP') 'Liveness remains UP during database downtime'
    } finally {
        Assert-TestTool 'kubectl' ($nsArgs+@('scale','statefulset/postgres','--replicas=1'))|Out-Null
        Assert-TestTool 'kubectl' ($nsArgs+@('rollout','status','statefulset/postgres','--timeout=180s')) 200|Out-Null
    }
    Wait-Health 200|Out-Null
    Assert-Check $true 'API readiness recovers after its database returns'
} catch {$failure=$_} finally {
    if($null -ne $portForward -and !$portForward.Process.HasExited){$portForward.Process.Kill();$portForward.Process.WaitForExit()}
    try {
        if($null -ne $namespaceUid){
            Assert-Check ((Get-NamespaceUid) -eq $namespaceUid) 'Cleanup owns the exact namespace UID created by this test'
            if($applyAttempted){Invoke-DatabaseTerraform @('destroy','-input=false','-no-color','-auto-approve')|Out-Null}
            Assert-TestTool 'kubectl' ($clusterArgs+@('delete','namespace',$namespace,'--wait=true','--timeout=180s')) 200|Out-Null
            Assert-Check ($null -eq (Get-NamespaceUid)) 'Only the disposable application/database namespace was removed'
            $cleanupSafe=$true
        }
    } catch {$cleanupFailure=$_;$cleanupSafe=$false}
    try {
        $after=Get-ExternalState
        foreach($property in $before.PSObject.Properties.Name){Assert-Check ($before.$property -ceq $after.$property) "External state unchanged: $property"}
    } catch {if($null -eq $cleanupFailure){$cleanupFailure=$_}}
    if($cleanupSafe){
        $resolvedRoot=[IO.Path]::GetFullPath($testRoot)
        if(!$resolvedRoot.StartsWith($tempParent,[StringComparison]::OrdinalIgnoreCase) -or !(Split-Path $resolvedRoot -Leaf).StartsWith('oficina-deployment-smoke-')){throw 'Unsafe temporary cleanup target.'}
        if(Test-Path -LiteralPath $resolvedRoot){Remove-Item -LiteralPath $resolvedRoot -Recurse -Force}
    }
}
if($null -ne $cleanupFailure){throw "Deployment cleanup/preservation failed. Recovery files: $testRoot. $cleanupFailure"}
if($null -ne $failure){throw $failure}
Write-Host "Local API deployment checks passed: $($checks.Count). Disposable namespace removed."
