[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][ValidatePattern('^oficina-ddd:local-[a-z0-9-]+$')][string]$Image,
    [Parameter(Mandatory=$true)][string]$ExpectedCommit,
    [ValidatePattern('^http://127\.0\.0\.1:[0-9]+$')][string]$IngressUrl='http://127.0.0.1:8090'
)

Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$repoRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$tempParent=[IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$testRoot=Join-Path $tempParent ('oficina-scaling-smoke-'+[Guid]::NewGuid().ToString('N'))
$moduleRoot=Join-Path $testRoot 'infra/local/database'
$id=[Guid]::NewGuid().ToString('N').Substring(0,10)
$namespace='oficina-db-test-'+$id
$ingressHost='smoke-'+$id+'.oficina.local'
$kubeconfig=[IO.Path]::GetFullPath((Join-Path $repoRoot 'infra/local/.generated/kubeconfig'))
$clusterArgs=@('--kubeconfig',$kubeconfig,'--context','kind-oficina-local')
$nsArgs=$clusterArgs+@('--namespace',$namespace)
$namespaceUid=$null
$applyAttempted=$false
$cleanupSafe=$true
$loadCreated=$false
$failure=$null
$cleanupFailure=$null
$checks=New-Object System.Collections.Generic.List[string]
$samples=New-Object System.Collections.Generic.List[object]
$baselineCpu=$null
$workloadCpu=$null
. (Join-Path $PSScriptRoot 'IntegrationTestTools.ps1')

function Assert-Check([bool]$Condition,[string]$Message){
    if(!$Condition){throw "FAILED: $Message"}
    $checks.Add($Message)
    Write-Host "PASS: $Message"
}
function Get-NamespaceUid {
    $value=Assert-TestTool 'kubectl' ($clusterArgs+@('get','namespace',$namespace,'--ignore-not-found','-o','json'))
    if([string]::IsNullOrWhiteSpace($value)){return $null}
    return ($value|ConvertFrom-Json).metadata.uid
}
function Get-ExternalState {
    return [pscustomobject]@{
        DockerContext=(Assert-TestTool 'docker' @('context','show')).Trim()
        DockerConfig=Get-TestFileHash (Join-Path $env:USERPROFILE '.docker/config.json')
        Containers=((Assert-TestTool 'docker' @('ps','-aq','--no-trunc')) -split '\r?\n'|Sort-Object)-join ','
        KubeConfig=Get-TestFileHash (Join-Path $env:USERPROFILE '.kube/config')
        PrivateKubeConfig=Get-TestFileHash $kubeconfig
        KubeContexts=(Assert-TestTool 'kubectl' @('config','get-contexts','-o','name')).Trim()
        Namespaces=(Assert-TestTool 'kubectl' ($clusterArgs+@('get','namespaces','-o','jsonpath={range .items[*]}{.metadata.name}:{.metadata.uid}{"\n"}{end}'))).Trim()
        HostsFile=Get-TestFileHash (Join-Path $env:SystemRoot 'System32/drivers/etc/hosts')
        UserPath=[Environment]::GetEnvironmentVariable('Path','User')
        MachinePath=[Environment]::GetEnvironmentVariable('Path','Machine')
    }
}
function Invoke-DatabaseTerraform([string[]]$Arguments,[int]$TimeoutSeconds=600){
    return Assert-TestTool 'terraform' (@("-chdir=$moduleRoot")+$Arguments) $TimeoutSeconds
}
function Get-OptionalProperty([object]$Object,[string]$Name,[object]$Default=$null){
    if($null -eq $Object -or $Object.PSObject.Properties.Name -notcontains $Name){return $Default}
    return $Object.$Name
}
function Get-ApiPods {
    return (Assert-TestTool 'kubectl' ($nsArgs+@('get','pods','-l','app.kubernetes.io/name=oficina-api','-o','json'))|ConvertFrom-Json).items
}
function Get-ScalingState {
    $hpa=Assert-TestTool 'kubectl' ($nsArgs+@('get','hpa','oficina-api','-o','json'))|ConvertFrom-Json
    $pods=@(Get-ApiPods)
    $ready=@($pods|Where-Object{@((Get-OptionalProperty $_.status 'conditions' @())|Where-Object{$_.type -eq 'Ready' -and $_.status -eq 'True'}).Count -eq 1})
    $cpu=$null
    foreach($metric in @(Get-OptionalProperty $hpa.status 'currentMetrics' @())){
        if((Get-OptionalProperty $metric 'type') -eq 'Resource'){
            $resource=Get-OptionalProperty $metric 'resource'
            if((Get-OptionalProperty $resource 'name') -eq 'cpu'){
                $current=Get-OptionalProperty $resource 'current'
                $cpu=Get-OptionalProperty $current 'averageUtilization'
            }
        }
    }
    return [pscustomobject]@{
        time=[DateTime]::UtcNow.ToString('o')
        current=[int](Get-OptionalProperty $hpa.status 'currentReplicas' 0)
        desired=[int](Get-OptionalProperty $hpa.status 'desiredReplicas' 0)
        ready=$ready.Count
        cpuPercent=$cpu
    }
}
function Convert-CpuMillicores([string]$Quantity){
    if($Quantity -notmatch '^(\d+(?:\.\d+)?)(n|u|m)?$'){throw 'Unexpected real CPU metric unit.'}
    $value=[decimal]::Parse($Matches[1],[Globalization.CultureInfo]::InvariantCulture)
    switch($Matches[2]){'n'{return $value/1000000};'u'{return $value/1000};'m'{return $value};default{return $value*1000}}
}
function Get-RealApiCpu {
    $names=@(Get-ApiPods|ForEach-Object{$_.metadata.name})
    $metrics=Assert-TestTool 'kubectl' ($clusterArgs+@('get',"--raw=/apis/metrics.k8s.io/v1beta1/namespaces/$namespace/pods"))|ConvertFrom-Json
    $apiMetrics=@($metrics.items|Where-Object{$names -contains $_.metadata.name})
    [decimal]$total=0
    foreach($pod in $apiMetrics){foreach($container in $pod.containers){$total+=Convert-CpuMillicores $container.usage.cpu}}
    return [pscustomobject]@{pods=$apiMetrics.Count;millicores=$total}
}
function Wait-Scaling([string]$Phase,[int]$Target,[int]$TimeoutSeconds=600){
    $watch=[Diagnostics.Stopwatch]::StartNew()
    $stable=0
    while($watch.Elapsed.TotalSeconds -lt $TimeoutSeconds){
        $sample=Get-ScalingState
        $sample|Add-Member -NotePropertyName phase -NotePropertyValue $Phase
        $samples.Add($sample)
        Write-Host "HPA $Phase current=$($sample.current) desired=$($sample.desired) ready=$($sample.ready) cpu=$($sample.cpuPercent)%"
        if($Phase -eq 'load'){
            if($sample.current -ge $Target -and $sample.desired -ge $Target -and $sample.ready -ge $Target){return $sample}
        }else{
            if($sample.current -eq $Target -and $sample.desired -eq $Target -and $sample.ready -eq $Target -and
                    $null -ne $sample.cpuPercent -and $sample.cpuPercent -le 50){$stable++}else{$stable=0}
            if($stable -ge 3){return $sample}
        }
        Start-Sleep -Seconds 10
    }
    throw "HPA did not reach expected $Phase state within $TimeoutSeconds seconds."
}
function Invoke-Ingress([string]$Path,[string]$HostHeader=$ingressHost,[string]$Body=''){
    $arguments=@('--silent','--show-error','--noproxy','*','--max-time','15','--header',"Host: $HostHeader",'--write-out',"`n%{http_code}")
    if($Body){$arguments+=@('--header','Content-Type: application/json','--data-binary',$Body)}
    $output=Assert-TestTool 'curl.exe' ($arguments+@($IngressUrl+$Path))
    $cut=$output.LastIndexOf("`n")
    $content=$output.Substring(0,$cut)
    return [pscustomobject]@{code=[int]$output.Substring($cut+1).Trim();content=$content}
}

$before=Get-ExternalState
try{
    $controllerService=Assert-TestTool 'kubectl' ($clusterArgs+@('--namespace','oficina-ingress','get','service','oficina-traefik','-o','json'))|ConvertFrom-Json
    Assert-Check ($controllerService.spec.type -eq 'ClusterIP') 'Traefik uses ClusterIP rather than a pending cloud LoadBalancer'
    Assert-Check (@($controllerService.spec.ports|Where-Object{$_.PSObject.Properties.Name -contains 'nodePort' -and $_.nodePort -gt 0}).Count -eq 0) 'Traefik Service allocates no NodePorts'
    $metricsApi=Assert-TestTool 'kubectl' ($clusterArgs+@('get','apiservice','v1beta1.metrics.k8s.io','-o','json'))|ConvertFrom-Json
    Assert-Check (@($metricsApi.status.conditions|Where-Object{$_.type -eq 'Available' -and $_.status -eq 'True'}).Count -eq 1) 'Metrics APIService is available before the scaling test'
    Assert-Check ($null -eq (Get-NamespaceUid)) 'Scaling smoke namespace does not already exist'
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
    $admin='admin-scaling'
    $adminPassword='admin-smoke-'+[Guid]::NewGuid().ToString('N')
    $variables=[pscustomobject]@{namespace=$namespace;database_password=$password;kubeconfig_path=$kubeconfig;context='kind-oficina-local'}
    [IO.File]::WriteAllText((Join-Path $moduleRoot 'smoke.auto.tfvars.json'),($variables|ConvertTo-Json))
    $secretFile=Join-Path $testRoot 'application-secret.env'
    [IO.File]::WriteAllText($secretFile,(@('SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/oficina_db',
        'SPRING_DATASOURCE_USERNAME=oficina_user',"SPRING_DATASOURCE_PASSWORD=$password",
        ('JWT_SECRET=scaling-smoke-'+[Guid]::NewGuid().ToString('N')),"ADMIN_USER=$admin","ADMIN_PASSWORD=$adminPassword")-join "`n"))
    Invoke-DatabaseTerraform @('init','-input=false','-no-color')|Out-Null
    Invoke-DatabaseTerraform @('validate','-no-color')|Out-Null
    Invoke-DatabaseTerraform @('plan','-input=false','-no-color','-out=smoke.tfplan')|Out-Null
    $applyAttempted=$true;$cleanupSafe=$false
    Invoke-DatabaseTerraform @('apply','-input=false','-no-color','-auto-approve','smoke.tfplan')|Out-Null
    $deployed=Invoke-TestTool 'powershell.exe' @('-NoProfile','-ExecutionPolicy','RemoteSigned','-File',
        (Join-Path $repoRoot 'scripts/Deploy-Local.ps1'),'-Namespace',$namespace,'-SecretFile',$secretFile,'-Image',$Image,'-IngressHost',$ingressHost) 600
    if($deployed.Code -ne 0){throw ('Scaling deployment failed: '+($deployed.Output+$deployed.Error).Replace($password,'<redacted>').Replace($adminPassword,'<redacted>'))}
    Assert-Check $true 'Real deployment completes in the isolated scaling namespace'
    $ingress=Assert-TestTool 'kubectl' ($nsArgs+@('get','ingress','oficina-api','-o','json'))|ConvertFrom-Json
    Assert-Check ($ingress.spec.ingressClassName -eq 'traefik' -and $ingress.spec.rules[0].host -eq $ingressHost) 'Ingress uses Traefik and the exclusive smoke host'
    $info=Invoke-Ingress '/api/v1/info'
    Assert-Check ($info.code -eq 200 -and ($info.content|ConvertFrom-Json).dados.commit -eq $ExpectedCommit) 'Loopback HTTP Ingress reaches the expected application source'
    $ownPods=@(Get-ApiPods|ForEach-Object{$_.metadata.name})
    Assert-Check ($ownPods -contains ($info.content|ConvertFrom-Json).dados.pod) 'Ingress routes to a pod inside the smoke namespace'
    Assert-Check ((Invoke-Ingress '/api/v1/info' ('absent-'+$id+'.oficina.local')).code -eq 404) 'An unmatched Ingress host does not route to another application'
    $loginBody=@{usuario=$admin;senha=$adminPassword}|ConvertTo-Json -Compress
    Assert-Check ((Invoke-Ingress '/api/v1/auth/login' $ingressHost $loginBody).code -eq 200) 'Smoke administrator exists before the BCrypt workload'
    $hpa=Assert-TestTool 'kubectl' ($nsArgs+@('get','hpa','oficina-api','-o','json'))|ConvertFrom-Json
    Assert-Check ($hpa.spec.minReplicas -eq 2 -and $hpa.spec.maxReplicas -eq 5 -and $hpa.spec.metrics[0].resource.name -eq 'cpu' -and $hpa.spec.metrics[0].resource.target.averageUtilization -eq 70) 'HPA is configured for 2-to-5 replicas at 70 percent CPU'
    Wait-Scaling 'baseline' 2|Out-Null
    Assert-Check $true 'Two Ready API replicas are stable before workload starts'
    $cpu=Get-RealApiCpu
    $baselineCpu=$cpu
    Assert-Check ($cpu.pods -ge 2 -and $cpu.millicores -ge 0) 'Metrics API reports real CPU samples for the API pods'
    Write-Host "PodMetrics baseline: pods=$($cpu.pods) totalCpu=$($cpu.millicores)m"
    $postgres=Assert-TestTool 'kubectl' ($nsArgs+@('get','statefulset','postgres','-o','json'))|ConvertFrom-Json
    $loadCommand=@'
payload='{"usuario":"admin-scaling","senha":"incorrect-scaling-password"}'
for worker in 1 2 3 4 5 6 7 8; do
  (while :; do wget -q --header='Content-Type: application/json' --post-data="$payload" -O /dev/null http://oficina-api/api/v1/auth/login >/dev/null 2>&1 || :; done) &
done
wait
'@
    $load=[pscustomobject]@{
        apiVersion='v1';kind='Pod';metadata=@{name='oficina-scaling-load';namespace=$namespace;labels=@{app='oficina-scaling-load'}}
        spec=@{restartPolicy='Never';activeDeadlineSeconds=900;terminationGracePeriodSeconds=1;automountServiceAccountToken=$false;containers=@(@{
            name='load';image=$postgres.spec.template.spec.containers[0].image;imagePullPolicy='IfNotPresent';command=@('sh','-ec',$loadCommand)
            resources=@{requests=@{cpu='50m';memory='32Mi'};limits=@{cpu='500m';memory='96Mi'}}})}
    }
    $loadFile=Join-Path $testRoot 'load.json'
    [IO.File]::WriteAllText($loadFile,($load|ConvertTo-Json -Depth 12))
    Assert-TestTool 'kubectl' ($nsArgs+@('create','-f',$loadFile))|Out-Null
    $loadCreated=$true
    Assert-TestTool 'kubectl' ($nsArgs+@('wait','--for=condition=Ready','pod/oficina-scaling-load','--timeout=90s')) 110|Out-Null
    $peak=Wait-Scaling 'load' 3
    $cpu=Get-RealApiCpu
    $workloadCpu=$cpu
    Assert-Check ($cpu.pods -ge 2 -and $cpu.millicores -gt 0) 'PodMetrics records positive API CPU usage during scale-up'
    Write-Host "PodMetrics workload: pods=$($cpu.pods) totalCpu=$($cpu.millicores)m"
    Assert-Check (@($samples|Where-Object{$_.phase -eq 'load' -and $null -ne $_.cpuPercent -and $_.cpuPercent -gt 70}).Count -gt 0) 'HPA observed real workload CPU utilization above its target'
    Assert-Check ($peak.current -ge 3 -and $peak.desired -ge 3 -and $peak.ready -ge 3) 'HPA scaled from two to at least three Ready replicas under HTTP load'
    Assert-TestTool 'kubectl' ($nsArgs+@('delete','pod','oficina-scaling-load','--wait=true','--timeout=60s')) 80|Out-Null
    $loadCreated=$false
    Wait-Scaling 'cooldown' 2|Out-Null
    Assert-Check $true 'HPA returns automatically to two Ready replicas after workload stops'
    $healthy=Invoke-Ingress '/actuator/health/readiness'
    Assert-Check ($healthy.code -eq 200 -and ($healthy.content|ConvertFrom-Json).status -eq 'UP') 'Ingress remains healthy after scale-up and scale-down'
}catch{$failure=$_}finally{
    try{
        if($null -ne $namespaceUid){
            Assert-Check ((Get-NamespaceUid) -eq $namespaceUid) 'Scaling cleanup owns the exact namespace UID created by this test'
            if($loadCreated){Assert-TestTool 'kubectl' ($nsArgs+@('delete','pod','oficina-scaling-load','--ignore-not-found','--wait=true','--timeout=60s')) 80|Out-Null}
            if($applyAttempted){Invoke-DatabaseTerraform @('destroy','-input=false','-no-color','-auto-approve')|Out-Null}
            Assert-TestTool 'kubectl' ($clusterArgs+@('delete','namespace',$namespace,'--wait=true','--timeout=180s')) 200|Out-Null
            Assert-Check ($null -eq (Get-NamespaceUid)) 'Only the scaling smoke namespace was removed'
            $cleanupSafe=$true
        }
    }catch{$cleanupFailure=$_;$cleanupSafe=$false}
    try{
        $after=Get-ExternalState
        foreach($property in $before.PSObject.Properties.Name){Assert-Check ($before.$property -ceq $after.$property) "External state unchanged: $property"}
    }catch{if($null -eq $cleanupFailure){$cleanupFailure=$_}}
    if($cleanupSafe){
        $resolvedRoot=[IO.Path]::GetFullPath($testRoot)
        if(!$resolvedRoot.StartsWith($tempParent,[StringComparison]::OrdinalIgnoreCase) -or !(Split-Path $resolvedRoot -Leaf).StartsWith('oficina-scaling-smoke-')){throw 'Unsafe temporary cleanup target.'}
        if(Test-Path -LiteralPath $resolvedRoot){Remove-Item -LiteralPath $resolvedRoot -Recurse -Force}
    }
}
$evidenceRoot=Join-Path $repoRoot 'target/local-evidence'
New-Item -ItemType Directory -Path $evidenceRoot -Force|Out-Null
$evidence=[ordered]@{
    passed=($null -eq $failure -and $null -eq $cleanupFailure)
    namespace=$namespace;ingressHost=$ingressHost;image=$Image;source=$ExpectedCommit
    baselinePodMetrics=$baselineCpu;workloadPodMetrics=$workloadCpu;samples=@($samples.ToArray())
}
[IO.File]::WriteAllText((Join-Path $evidenceRoot 'scaling.json'),($evidence|ConvertTo-Json -Depth 10))
Write-Host 'Sanitized scaling evidence: target/local-evidence/scaling.json'
if($null -ne $cleanupFailure){throw "Scaling cleanup/preservation failed. Recovery files: $testRoot. $cleanupFailure"}
if($null -ne $failure){throw $failure}
Write-Host "Local Ingress/metrics/HPA smoke checks passed: $($checks.Count). Disposable namespace removed."
