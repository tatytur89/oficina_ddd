[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$tempParent = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
$testRoot = Join-Path $tempParent ('oficina-environment-tests-' + [Guid]::NewGuid().ToString('N'))
$checks = New-Object System.Collections.Generic.List[string]

function Assert-Check([bool]$Condition, [string]$Message) {
    if (!$Condition) { throw "FAILED: $Message" }
    $checks.Add($Message)
    Write-Host "PASS: $Message"
}

function Invoke-Native([string]$Name, [string[]]$Arguments) {
    $exe = (Get-Command $Name -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
    $start = New-Object System.Diagnostics.ProcessStartInfo
    $start.FileName = $exe
    $start.Arguments = (($Arguments | ForEach-Object { '"' + $_.Replace('"', '\"') + '"' }) -join ' ')
    $start.WorkingDirectory = $repoRoot
    $start.UseShellExecute = $false
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $process = [System.Diagnostics.Process]::Start($start)
    $output = $process.StandardOutput.ReadToEndAsync()
    $errorOutput = $process.StandardError.ReadToEndAsync()
    if (!$process.WaitForExit(60000)) { $process.Kill(); throw "$Name timed out." }
    return [pscustomobject]@{ Code = $process.ExitCode; Output = $output.Result; Error = $errorOutput.Result }
}

function Invoke-Success([string]$Name, [string[]]$Arguments) {
    $result = Invoke-Native $Name $Arguments
    if ($result.Code -ne 0) { throw "$Name failed (exit $($result.Code)): $($result.Error)" }
    return $result.Output
}

function Get-ConfigHash([string]$Path) {
    if (!(Test-Path -LiteralPath $Path)) { return '<missing>' }
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
}

function Get-ExternalState {
    return [pscustomobject]@{
        DockerContext = (Invoke-Success 'docker' @('context', 'show')).Trim()
        Containers = ((Invoke-Success 'docker' @('ps', '-aq', '--no-trunc')) -split '\r?\n' | Sort-Object) -join ','
        DockerConfig = Get-ConfigHash (Join-Path $env:USERPROFILE '.docker/config.json')
        KubeConfig = Get-ConfigHash (Join-Path $env:USERPROFILE '.kube/config')
        KubeContexts = (Invoke-Success 'kubectl' @('config', 'get-contexts', '-o', 'name')).Trim()
        UserPath = [Environment]::GetEnvironmentVariable('Path', 'User')
        MachinePath = [Environment]::GetEnvironmentVariable('Path', 'Machine')
    }
}

function Get-Documents([string]$Yaml) {
    return @([regex]::Split($Yaml, '(?m)^---[ \t]*\r?$') | Where-Object { $_ -match '(?m)^kind:' })
}

function Get-Kind([string]$Document) {
    return ([regex]::Match($Document, '(?m)^kind:[ \t]*(\S+)')).Groups[1].Value
}

function Get-MetadataField([string]$Document, [string]$Field) {
    $metadata = ([regex]::Match($Document, '(?ms)^metadata:[ \t]*\r?\n(.*?)(?=^\S|\z)')).Groups[1].Value
    return ([regex]::Match($metadata, ('(?m)^  ' + [regex]::Escape($Field) + ':[ \t]*([^\r\n]+)'))).Groups[1].Value.Trim(' ', '"', "'")
}

function Assert-Namespaces([string[]]$Documents, [string]$Namespace) {
    foreach ($doc in $Documents) {
        if ((Get-Kind $doc) -eq 'Namespace') {
            Assert-Check ((Get-MetadataField $doc 'name') -eq $Namespace) "Namespace resource is $Namespace"
        } else {
            Assert-Check ((Get-MetadataField $doc 'namespace') -eq $Namespace) "$(Get-Kind $doc) belongs to $Namespace"
        }
    }
}

function Assert-References([string[]]$Documents, [string]$Deployment, [bool]$GeneratedSecret) {
    $configNames = @($Documents | Where-Object { (Get-Kind $_) -eq 'ConfigMap' } | ForEach-Object { Get-MetadataField $_ 'name' })
    $configRefs = @([regex]::Matches($Deployment, 'configMapRef:[ \t]*\r?\n[ \t]+name:[ \t]*(\S+)'))
    Assert-Check ($configRefs.Count -gt 0) 'Deployment references its generated ConfigMap'
    foreach ($ref in $configRefs) {
        Assert-Check ($configNames -contains $ref.Groups[1].Value) 'Deployment ConfigMap reference resolves in overlay'
    }
    $secretNames = @($Documents | Where-Object { (Get-Kind $_) -eq 'Secret' } | ForEach-Object { Get-MetadataField $_ 'name' })
    $secretRefs = @([regex]::Matches($Deployment, 'secretRef:[ \t]*\r?\n[ \t]+name:[ \t]*(\S+)'))
    Assert-Check ($secretRefs.Count -gt 0) 'Deployment references an application Secret'
    foreach ($ref in $secretRefs) {
        if ($GeneratedSecret) {
            if ($ref.Groups[1].Value -eq 'oficina-mail-secret') {
                Assert-Check ($Deployment -match 'name:[ \t]*oficina-mail-secret\r?\n[ \t]+optional:[ \t]*true') 'Local SMTP Secret is optional and supplied separately at runtime'
            } else {
                Assert-Check ($secretNames -contains $ref.Groups[1].Value) 'Local Secret reference resolves to generated Secret'
            }
        } else {
            Assert-Check ($ref.Groups[1].Value -eq 'oficina-api-secret') 'AWS keeps its externally provisioned Secret reference'
        }
    }
}

$before = Get-ExternalState
try {
    New-Item -ItemType Directory -Path $testRoot | Out-Null
    Copy-Item -LiteralPath (Join-Path $repoRoot 'k8s') -Destination $testRoot -Recurse
    $local = Join-Path $testRoot 'k8s/local'
    $aws = Join-Path $testRoot 'k8s/aws'
    $localSecret = Join-Path $local 'secret.env'
    if (Test-Path -LiteralPath $localSecret) { Remove-Item -LiteralPath $localSecret }

    $withoutSecret = Invoke-Native 'kubectl' @('kustomize', $local)
    Assert-Check ($withoutSecret.Code -ne 0 -and $withoutSecret.Error -match 'secret\.env') 'Local render fails clearly when secret.env is absent'
    Copy-Item -LiteralPath (Join-Path $local 'secret.env.example') -Destination $localSecret
    $localYaml = Invoke-Success 'kubectl' @('kustomize', $local)
    $localDocs = Get-Documents $localYaml
    $localDeployment = @($localDocs | Where-Object { (Get-Kind $_) -eq 'Deployment' })
    Assert-Check ($localDeployment.Count -eq 1) 'Local overlay renders exactly one application Deployment'
    Assert-Namespaces $localDocs 'oficina-local'
    Assert-Check ($localYaml -notmatch '(?i)amazonaws\.com|alb\.ingress|ghcr\.io') 'Local overlay contains no AWS endpoint, ALB annotation or GHCR image'
    $localIngress = @($localDocs | Where-Object { (Get-Kind $_) -eq 'Ingress' })
    $localHpa = @($localDocs | Where-Object { (Get-Kind $_) -eq 'HorizontalPodAutoscaler' })
    Assert-Check ($localIngress.Count -eq 1 -and $localIngress[0] -match 'ingressClassName:[ \t]*traefik' -and $localIngress[0] -match 'host:[ \t]*oficina\.local') 'Local Ingress targets Traefik and its explicit local host'
    Assert-Check ($localHpa.Count -eq 1 -and $localHpa[0] -match 'minReplicas:[ \t]*2' -and $localHpa[0] -match 'maxReplicas:[ \t]*5' -and $localHpa[0] -match 'averageUtilization:[ \t]*70') 'Local HPA scales two to five replicas using actual CPU utilization'
    Assert-Check ($localDeployment[0] -notmatch '(?m)^  replicas:') 'Local Deployment leaves replica ownership to the HPA'
    Assert-Check ($localDeployment[0] -match 'image:[ \t]*oficina-ddd:local') 'Local Deployment uses its loaded local image'
    Assert-Check ($localDeployment[0] -match 'imagePullPolicy:[ \t]*Never') 'Local Deployment never pulls its image from a registry'
    Assert-Check ($localYaml -match 'SPRING_PROFILES_ACTIVE:[ \t]*["'']?lab["'']?\s') 'Local overlay selects the lab Spring profile'
    Assert-References $localDocs $localDeployment[0] $true

    $awsYaml = Invoke-Success 'kubectl' @('kustomize', $aws)
    $awsDocs = Get-Documents $awsYaml
    $awsDeployment = @($awsDocs | Where-Object { (Get-Kind $_) -eq 'Deployment' })
    Assert-Check ($awsDeployment.Count -eq 1) 'AWS overlay renders exactly one application Deployment'
    Assert-Namespaces $awsDocs 'oficina-api'
    Assert-Check ($awsYaml -match 'alb\.ingress\.kubernetes\.io/scheme:[ \t]*internet-facing') 'AWS overlay retains ALB ingress configuration'
    Assert-Check ($awsYaml -match 'minReplicas:[ \t]*2' -and $awsYaml -match 'maxReplicas:[ \t]*5') 'AWS overlay retains the 2-to-5-pod HPA'
    Assert-Check ($awsYaml -match 'SPRING_PROFILES_ACTIVE:[ \t]*["'']?aws["'']?\s') 'AWS overlay selects the aws Spring profile'
    Assert-Check ($awsDeployment[0] -match 'image:[ \t]*ghcr\.io/tatytur89/oficina_ddd:latest') 'AWS Deployment keeps the GHCR image default'
    Assert-Check ($awsDeployment[0] -match 'imagePullPolicy:[ \t]*IfNotPresent') 'AWS image pull policy is not inherited from the local-only override'
    Assert-Check (@($awsDocs | Where-Object { (Get-Kind $_) -eq 'Secret' }).Count -eq 0) 'AWS example secrets are not embedded in its rendered resources'
    Assert-References $awsDocs $awsDeployment[0] $false

    $deploymentFile = Join-Path $testRoot 'aws-deployment.yaml'
    [System.IO.File]::WriteAllText($deploymentFile, $awsDeployment[0])
    $comparisonImage = 'ghcr.io/tatytur89/oficina_ddd:test-runtime-comparison'
    $testedImage = 'ghcr.io/tatytur89/oficina_ddd:sha-environment-isolation-test'
    $original = Invoke-Success 'kubectl' @('set', 'image', '--local=true', '-f', $deploymentFile, "oficina-api=$comparisonImage", '-o', 'json') | ConvertFrom-Json
    $baseDeploymentFile = Join-Path $testRoot 'k8s/base/deployment.yaml'
    $imagePatch = Invoke-Success 'kubectl' @('set', 'image', '--local=true', '-f', $baseDeploymentFile, "oficina-api=$testedImage", '-o', 'yaml')
    [System.IO.File]::WriteAllText((Join-Path $aws 'deployment-image-patch.yaml'), $imagePatch)
    Add-Content -LiteralPath (Join-Path $aws 'kustomization.yaml') -Value "`npatches:`n  - path: deployment-image-patch.yaml" -Encoding ascii
    $patchedYaml = Invoke-Success 'kubectl' @('kustomize', $aws)
    $patchedDocs = Get-Documents $patchedYaml
    $patchedDeployment = @($patchedDocs | Where-Object { (Get-Kind $_) -eq 'Deployment' })
    Assert-Check ($patchedDeployment.Count -eq 1 -and $patchedDeployment[0].Contains("image: $testedImage")) 'Exact CD patch survives AWS Kustomize image transformation with the tested SHA'
    [System.IO.File]::WriteAllText($deploymentFile, $patchedDeployment[0])
    $patched = Invoke-Success 'kubectl' @('set', 'image', '--local=true', '-f', $deploymentFile, "oficina-api=$comparisonImage", '-o', 'json') | ConvertFrom-Json
    Assert-Check (($original.spec | ConvertTo-Json -Depth 40 -Compress) -ceq ($patched.spec | ConvertTo-Json -Depth 40 -Compress)) 'SHA image replacement preserves all remaining rendered Deployment settings'
    Assert-Namespaces $patchedDocs 'oficina-api'

    $rootManifests = @(Get-ChildItem -LiteralPath (Join-Path $repoRoot 'k8s') -File | Where-Object { $_.Extension -in @('.yaml', '.yml') })
    Assert-Check ($rootManifests.Count -eq 0) 'Kubernetes root has no ambiguous default apply target'

    $compose = Invoke-Success 'docker' @('compose', '-f', (Join-Path $repoRoot 'docker-compose.yaml'), 'config', '--format', 'json') | ConvertFrom-Json
    $composeProfile = $compose.services.'oficina-ddd'.environment.SPRING_PROFILES_ACTIVE
    Assert-Check ($composeProfile -eq 'docker') 'Compose selects its isolated development Spring profile'
    $workflow = Get-Content -LiteralPath (Join-Path $repoRoot '.github/workflows/cd-pipeline.yml') -Raw
    Assert-Check ($workflow -match 'k8s/aws' -and $workflow -notmatch 'k8s/local') 'CD targets only the AWS overlay'
    Assert-Check ($workflow -match 'set image' -and $workflow -match 'sha-' -and $workflow -match 'github\.sha|head_sha') 'CD keeps image replacement tied to its selected commit'

    foreach ($path in @('k8s/local/secret.env', 'k8s/aws/secret.yaml', 'infra/local/terraform.tfstate', 'infra/local/terraform.tfvars', 'infra/local/.terraform/providers/placeholder',
            'infra/local/database/terraform.tfstate', 'infra/local/database/terraform.tfvars', 'infra/local/database/.terraform/providers/placeholder', 'infra/local/database/database.tfplan',
            'infra/local/addons/.terraform/providers/placeholder', 'infra/local/addons/terraform.tfvars', 'infra/local/addons/terraform.tfstate', 'infra/local/addons/addons.tfplan')) {
        $ignored = Invoke-Native 'git' @('check-ignore', '--no-index', $path)
        Assert-Check ($ignored.Code -eq 0) "Sensitive or generated path is ignored: $path"
    }
    foreach ($path in @('k8s/local/secret.env.example', 'k8s/local/mail.env', 'k8s/local/mail.env.example', 'k8s/aws/secret.yaml.example', 'infra/local/database/terraform.tfvars.example')) {
        $ignored = Invoke-Native 'git' @('check-ignore', '--no-index', $path)
        Assert-Check ($ignored.Code -eq 1) "Reviewable example remains versionable: $path"
    }
    $localTerraform = @(Get-ChildItem -LiteralPath (Join-Path $repoRoot 'infra/local') -Filter '*.tf' -File)
    Assert-Check ($localTerraform.Count -gt 0) 'Local Terraform configuration has a separate directory'
    $localTerraformContent = ($localTerraform | Get-Content -Raw) -join "`n"
    Assert-Check ($localTerraformContent -match 'backend\s+"local"') 'Local Terraform state uses a local backend'
    foreach ($file in $localTerraform) {
        $hcl = Get-Content -LiteralPath $file.FullName -Raw
        Assert-Check ($hcl -notmatch '(?m)^\s*(provider\s+"aws"|backend\s+"(?:s3|remote)"|cloud\s*\{)' -and
                $hcl -notmatch 'source\s*=\s*"hashicorp/aws"') 'Local Terraform has no AWS provider or remote backend'
        foreach ($resource in [regex]::Matches($hcl, '(?m)^\s*resource\s+"([^"]+)"')) {
            Assert-Check ($resource.Groups[1].Value -in @('kind_cluster', 'terraform_data')) 'Local Terraform resources are restricted to the local cluster lifecycle'
        }
        foreach ($module in [regex]::Matches($hcl, '(?ms)^\s*module\s+"[^"]+"\s*\{(.*?)^\}')) {
            $source = [regex]::Match($module.Groups[1].Value, 'source\s*=\s*"([^"]+)"').Groups[1].Value
            Assert-Check ($source.StartsWith('./') -or $source.StartsWith('../')) 'Local Terraform cannot load remote modules'
        }
    }
} finally {
    $after = Get-ExternalState
    foreach ($property in $before.PSObject.Properties.Name) {
        Assert-Check ($before.$property -ceq $after.$property) "External state unchanged: $property"
    }
    $resolvedTestRoot = [System.IO.Path]::GetFullPath($testRoot)
    if (!$resolvedTestRoot.StartsWith($tempParent, [StringComparison]::OrdinalIgnoreCase) -or
            !(Split-Path $resolvedTestRoot -Leaf).StartsWith('oficina-environment-tests-')) {
        throw 'Unsafe temporary cleanup target.'
    }
    if (Test-Path -LiteralPath $resolvedTestRoot) { Remove-Item -LiteralPath $resolvedTestRoot -Recurse -Force }
}

Write-Host "Environment separation checks passed: $($checks.Count). No manifests were applied."
