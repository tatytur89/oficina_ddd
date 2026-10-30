[CmdletBinding()]
param(
    [string]$Namespace = 'oficina-local',
    [string]$SecretFile,
    [string]$Image,
    [string]$IngressHost
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if ($Namespace -notmatch '^(oficina-local|oficina-db-test-[a-z0-9]+)$') { throw 'Invalid local namespace.' }
if (!$IngressHost) { $IngressHost = if ($Namespace -eq 'oficina-local') { 'oficina.local' } else { "$Namespace.oficina.local" } }
if ($IngressHost -notmatch '^(oficina\.local|[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.oficina\.local)$') {
    throw 'Use oficina.local or a dedicated <name>.oficina.local host.'
}
if (!$SecretFile) { $SecretFile = Join-Path $repoRoot 'k8s/local/secret.env' }
$SecretFile = [IO.Path]::GetFullPath($SecretFile)
if (!(Test-Path -LiteralPath $SecretFile -PathType Leaf)) { throw 'Create the local secret.env from its example before deploying.' }

# These helpers pin this child process to the recorded local Docker Engine and provider.
. (Join-Path $repoRoot 'infra/local/scripts/common.ps1')
Initialize-LocalPaths ([IO.Path]::GetFullPath((Join-Path $repoRoot 'infra/local'))) 'oficina-local'
& (Join-Path $repoRoot 'infra/local/scripts/check.ps1')
$clusterArgs = @('--kubeconfig', $script:Kubeconfig, '--context', 'kind-oficina-local')
$namespaceArgs = $clusterArgs + @('--namespace', $Namespace)
$namespaceInfo = Invoke-Tool 'kubectl' ($clusterArgs + @('get', 'namespace', $Namespace, '-o', 'json')) | ConvertFrom-Json
$namespaceUid = $namespaceInfo.metadata.uid

$deployLock = New-Object Threading.Mutex($false, "Local\OficinaDeploy-$Namespace")
try { $locked = $deployLock.WaitOne(0) } catch [Threading.AbandonedMutexException] { $locked = $true }
if (!$locked) { $deployLock.Dispose(); throw 'Another deployment is managing this namespace.' }
$hostLock = New-Object Threading.Mutex($false, "Local\OficinaIngress-$IngressHost")
try { $hostLocked = $hostLock.WaitOne(0) } catch [Threading.AbandonedMutexException] { $hostLocked = $true }
if (!$hostLocked) {
    $hostLock.Dispose()
    $deployLock.ReleaseMutex()
    $deployLock.Dispose()
    throw 'Another deployment is managing this Ingress host.'
}
$tempParent = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$tempRoot = Join-Path $tempParent ('oficina-deploy-' + [Guid]::NewGuid().ToString('N'))
try {
    $requiredKeys = @('SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME', 'SPRING_DATASOURCE_PASSWORD', 'JWT_SECRET', 'ADMIN_USER', 'ADMIN_PASSWORD')
    $values = @{}
    foreach ($line in [IO.File]::ReadAllLines($SecretFile)) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith('#')) { continue }
        if ($line -notmatch '^([A-Z_]+)=(.+)$') { throw 'Invalid local secret.env entry.' }
        $key = $Matches[1]
        $value = $Matches[2]
        if ([string]::IsNullOrWhiteSpace($value)) { throw 'Local secret.env values cannot be blank.' }
        if ($key -notin $requiredKeys -or $values.ContainsKey($key)) { throw 'Unknown or duplicate local secret.env key.' }
        $values[$key] = $value
    }
    foreach ($key in $requiredKeys) { if (!$values.ContainsKey($key)) { throw "Missing local secret.env key: $key" } }
    if ($values.SPRING_DATASOURCE_URL -cne 'jdbc:postgresql://postgres:5432/oficina_db' -or
        $values.SPRING_DATASOURCE_USERNAME -cne 'oficina_user') { throw 'The local deployment requires the PostgreSQL Service and user provisioned by this project.' }
    if ($values.JWT_SECRET.Length -lt 32 -or $values.ADMIN_PASSWORD.Length -lt 12) { throw 'Use at least 32 characters for JWT_SECRET and 12 for ADMIN_PASSWORD.' }

    Invoke-Tool 'kubectl' ($namespaceArgs + @('rollout', 'status', 'statefulset/postgres', '--timeout=180s')) | Write-Host
    $jobs = Invoke-Tool 'kubectl' ($namespaceArgs + @('get', 'jobs', '-o', 'json')) | ConvertFrom-Json
    $schemaJobs = @($jobs.items | Where-Object { $_.metadata.name.StartsWith('oficina-schema-') })
    if ($schemaJobs.Count -eq 0) { throw 'Provision the database and complete its migration Job before deploying.' }
    foreach ($job in $schemaJobs) {
        $complete = @($job.status.conditions | Where-Object { $_.type -eq 'Complete' -and $_.status -eq 'True' })
        if ($complete.Count -eq 0) { throw 'A schema migration Job has not completed successfully.' }
    }
    Invoke-Tool 'kubectl' ($clusterArgs + @('wait', '--for=condition=Available', 'apiservice/v1beta1.metrics.k8s.io', '--timeout=120s')) | Write-Host
    $ingressClass = Invoke-Tool 'kubectl' ($clusterArgs + @('get', 'ingressclass', 'traefik', '-o', 'json')) | ConvertFrom-Json
    if ($ingressClass.spec.controller -cne 'traefik.io/ingress-controller') { throw 'Expected the local Traefik IngressClass.' }
    Invoke-Tool 'kubectl' ($clusterArgs + @('-n', 'oficina-ingress', 'rollout', 'status', 'deployment/oficina-traefik', '--timeout=180s')) | Write-Host
    $ingressService = Invoke-Tool 'kubectl' ($clusterArgs + @('-n', 'oficina-ingress', 'get', 'service', 'oficina-traefik', '-o', 'json')) | ConvertFrom-Json
    if ($ingressService.spec.type -cne 'ClusterIP') { throw 'Expected an internal Traefik Service; local access must use the reserved hostPorts.' }
    $ingresses = Invoke-Tool 'kubectl' ($clusterArgs + @('get', 'ingress', '--all-namespaces', '-o', 'json')) | ConvertFrom-Json
    foreach ($ingress in $ingresses.items) {
        if ($ingress.spec.PSObject.Properties.Name -notcontains 'rules') { continue }
        foreach ($rule in $ingress.spec.rules) {
            if ($rule.PSObject.Properties.Name -notcontains 'host') { continue }
            if ($rule.host -ceq $IngressHost -and ($ingress.metadata.namespace -cne $Namespace -or $ingress.metadata.name -cne 'oficina-api')) {
                throw 'The requested Ingress host is already owned by another resource.'
            }
        }
    }
    $databaseSecret = Invoke-Tool 'kubectl' ($namespaceArgs + @('get', 'secret', 'postgres-secret', '-o', 'json')) | ConvertFrom-Json
    $databasePassword = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($databaseSecret.data.POSTGRES_PASSWORD))
    if ($values.SPRING_DATASOURCE_PASSWORD -cne $databasePassword) { throw 'Application and PostgreSQL Secret passwords differ.' }

    if (!$Image) {
        $commit = (Invoke-Tool 'git' @('-C', $repoRoot, 'rev-parse', 'HEAD')).Trim()
        $source = $commit
        $dirty = Invoke-Tool 'git' @('-C', $repoRoot, 'status', '--porcelain')
        if (![string]::IsNullOrWhiteSpace($dirty)) { $source += '-dirty' }
        $Image = 'oficina-ddd:local-' + $commit.Substring(0,12) + '-' + [Guid]::NewGuid().ToString('N').Substring(0,12)
        Invoke-Tool 'docker' @('build', '--build-arg', "APP_COMMIT_SHA=$source", '--tag', $Image, $repoRoot) | Write-Host
    }
    if ($Image -notmatch '^oficina-ddd:local-[a-z0-9-]+$') { throw 'Use a unique local oficina-ddd:local-<id> image tag.' }
    $imageId = (Invoke-Tool 'docker' @('image', 'inspect', $Image, '--format', '{{.Id}}')).Trim()
    $imageEnvironment = Invoke-Tool 'docker' @('image', 'inspect', $Image, '--format', '{{json .Config.Env}}') | ConvertFrom-Json
    $sourceEntry = @($imageEnvironment | Where-Object { $_.StartsWith('APP_COMMIT_SHA=') })
    if ($sourceEntry.Count -ne 1) { throw 'Image source metadata is missing.' }
    $source = $sourceEntry[0].Substring('APP_COMMIT_SHA='.Length)
    if ($source -notmatch '^[a-f0-9]{40}(-dirty)?$' -or $imageId -notmatch '^sha256:[a-f0-9]{64}$') {
        throw 'Use an image built from this repository with Git source metadata.'
    }
    # Docker's containerd store may expose an OCI index ID, while CRI exposes the
    # image configuration ID. Resolve the latter from the exact archive we load.
    Get-Command tar -CommandType Application -ErrorAction Stop | Out-Null
    New-Item -ItemType Directory -Path $tempRoot -Force | Out-Null
    $archive = Join-Path $tempRoot 'image.tar'
    Invoke-Tool 'docker' @('image', 'save', '--output', $archive, $Image) | Out-Null
    $archiveManifest = Invoke-Tool 'tar' @('-xOf', $archive, 'manifest.json') | ConvertFrom-Json
    if (@($archiveManifest).Count -ne 1) { throw 'Expected a single runnable image in the Docker archive.' }
    $configPath = $archiveManifest[0].Config
    if ($configPath -match '^(?:blobs/sha256/)?([a-f0-9]{64})(?:\.json)?$') {
        $configurationId = 'sha256:' + $Matches[1]
    } else { throw 'Unexpected image configuration path in Docker archive.' }
    Invoke-Tool 'kind' @('load', 'image-archive', $archive, '--name', 'oficina-local') | Write-Host

    $owner = Read-Owner
    foreach ($nodeId in $owner.node_ids) {
        $loaded = Invoke-Tool 'docker' @('exec', $nodeId, 'crictl', 'inspecti', $Image) | ConvertFrom-Json
        if ($loaded.status.id -cne $configurationId) { throw 'A kind node has a different image than the local build.' }
    }

    $overlay = Join-Path $tempRoot 'k8s/local'
    New-Item -ItemType Directory -Path $overlay -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $repoRoot 'k8s/base') -Destination (Join-Path $tempRoot 'k8s/base') -Recurse
    foreach ($file in Get-ChildItem -LiteralPath (Join-Path $repoRoot 'k8s/local') -Filter '*.yaml' -File) {
        Copy-Item -LiteralPath $file.FullName -Destination $overlay
    }
    Copy-Item -LiteralPath $SecretFile -Destination (Join-Path $overlay 'secret.env')
    $kustomizationPath = Join-Path $overlay 'kustomization.yaml'
    $kustomization = [IO.File]::ReadAllText($kustomizationPath)
    $kustomization = $kustomization -replace '(?m)^namespace:.*$', "namespace: $Namespace"
    $kustomization = $kustomization -replace '(?m)^  - namespace\.yaml\r?\n', ''
    $kustomization = $kustomization -replace '(?m)^    newTag:.*$', ('    newTag: ' + $Image.Split(':')[1])
    [IO.File]::WriteAllText($kustomizationPath, $kustomization)
    $patchPath = Join-Path $overlay 'deployment-patch.yaml'
    $patch = [IO.File]::ReadAllText($patchPath)
    $patch = $patch -replace '(?m)^  template:\r?\n', ("  template:`n    metadata:`n      annotations:`n        oficina.fiap.local/image-id: '$imageId'`n        oficina.fiap.local/source: '$source'`n")
    [IO.File]::WriteAllText($patchPath, $patch)
    $ingressPath = Join-Path $overlay 'ingress.yaml'
    $ingressYaml = [IO.File]::ReadAllText($ingressPath) -replace '(?m)^    - host:.*$', ("    - host: $IngressHost")
    [IO.File]::WriteAllText($ingressPath, $ingressYaml)
    $currentNamespace = Invoke-Tool 'kubectl' ($clusterArgs + @('get', 'namespace', $Namespace, '-o', 'json')) | ConvertFrom-Json
    if ($currentNamespace.metadata.uid -cne $namespaceUid) { throw 'Namespace changed during deployment; refusing to apply.' }
    Invoke-Tool 'kubectl' ($namespaceArgs + @('apply', '-k', $overlay)) | Write-Host
    Invoke-Tool 'kubectl' ($namespaceArgs + @('rollout', 'status', 'deployment/oficina-api', '--timeout=300s')) | Write-Host
    $autoscaler = Invoke-Tool 'kubectl' ($namespaceArgs + @('get', 'hpa', 'oficina-api', '-o', 'json')) | ConvertFrom-Json
    $readyDeadline = [DateTime]::UtcNow.AddSeconds(180)
    do {
        $deployment = Invoke-Tool 'kubectl' ($namespaceArgs + @('get', 'deployment', 'oficina-api', '-o', 'json')) | ConvertFrom-Json
        $available = if ($deployment.status.PSObject.Properties.Name -contains 'availableReplicas') { $deployment.status.availableReplicas } else { 0 }
        if ($available -ge $autoscaler.spec.minReplicas) { break }
        Start-Sleep -Seconds 3
    } while ([DateTime]::UtcNow -lt $readyDeadline)
    if ($deployment.spec.template.spec.containers[0].image -cne $Image -or $available -lt $autoscaler.spec.minReplicas) {
        throw 'The expected image is not available in the local deployment.'
    }
    if ($deployment.spec.template.metadata.annotations.'oficina.fiap.local/image-id' -cne $imageId) {
        throw 'Deployment image identity annotation differs from the loaded artifact.'
    }
    $configName = $deployment.spec.template.spec.containers[0].envFrom[0].configMapRef.name
    $config = Invoke-Tool 'kubectl' ($namespaceArgs + @('get', 'configmap', $configName, '-o', 'json')) | ConvertFrom-Json
    if ($config.data.SPRING_PROFILES_ACTIVE -cne 'lab') { throw 'Expected the lab Spring profile.' }
    Write-Host "Deployment ready: namespace=$Namespace image=$Image source=$source"
    Write-Host "Ingress host: $IngressHost (use the cluster HTTP port with this Host header). HPA: $($autoscaler.spec.minReplicas)..$($autoscaler.spec.maxReplicas)."
    Write-Host "Access: kubectl --kubeconfig infra/local/.generated/kubeconfig --context kind-oficina-local -n $Namespace port-forward --address=127.0.0.1 service/oficina-api 8089:80"
} finally {
    if (Test-Path -LiteralPath $tempRoot) {
        $resolved = [IO.Path]::GetFullPath($tempRoot)
        if (!$resolved.StartsWith($tempParent, [StringComparison]::OrdinalIgnoreCase) -or
            !(Split-Path $resolved -Leaf).StartsWith('oficina-deploy-')) { throw 'Unsafe temporary cleanup target.' }
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
    $deployLock.ReleaseMutex()
    $deployLock.Dispose()
    $hostLock.ReleaseMutex()
    $hostLock.Dispose()
}
