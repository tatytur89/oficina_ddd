[CmdletBinding()]
param([string]$JavaHome = $env:JAVA_HOME)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
if ([string]::IsNullOrWhiteSpace($JavaHome) -or !(Test-Path -LiteralPath (Join-Path $JavaHome 'bin/java.exe'))) {
    throw 'Set JAVA_HOME to Java 21 before running the real PostgreSQL schema test.'
}
$kubeconfig = [IO.Path]::GetFullPath((Join-Path $repoRoot 'infra/local/.generated/kubeconfig'))
$context = 'kind-oficina-local'
$namespace = 'oficina-db-test-' + [Guid]::NewGuid().ToString('N').Substring(0, 10)
$tempParent = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$testRoot = Join-Path $tempParent ('oficina-db-smoke-' + [Guid]::NewGuid().ToString('N'))
$moduleRoot = Join-Path $testRoot 'infra/local/database'
$namespaceUid = $null
$applyAttempted = $false
$databaseReady = $false
$cleanupSafe = $true
$portForward = $null
$failure = $null
$cleanupFailure = $null
$checks = New-Object System.Collections.Generic.List[string]
$clusterArgs = @('--kubeconfig', $kubeconfig, '--context', $context)
$dbArgs = $clusterArgs + @('--namespace', $namespace)

function Assert-Check([bool]$Condition, [string]$Message) {
    if (!$Condition) { throw "FAILED: $Message" }
    $checks.Add($Message)
    Write-Host "PASS: $Message"
}

function Start-Native([string]$Name, [string[]]$Arguments, [hashtable]$Environment = @{}) {
    $start = New-Object Diagnostics.ProcessStartInfo
    $start.FileName = (Get-Command $Name -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
    $start.Arguments = (($Arguments | ForEach-Object { '"' + $_.Replace('"', '\"') + '"' }) -join ' ')
    $start.WorkingDirectory = $repoRoot
    $start.UseShellExecute = $false
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    foreach ($key in $Environment.Keys) { $start.EnvironmentVariables[$key] = [string]$Environment[$key] }
    $process = [Diagnostics.Process]::Start($start)
    return [pscustomobject]@{ Process=$process; Output=$process.StandardOutput.ReadToEndAsync(); Error=$process.StandardError.ReadToEndAsync() }
}

function Invoke-Native([string]$Name, [string[]]$Arguments, [int]$TimeoutSeconds = 60, [hashtable]$Environment = @{}) {
    $running = Start-Native $Name $Arguments $Environment
    $watch = [Diagnostics.Stopwatch]::StartNew()
    $nextUpdate = 30
    while (!$running.Process.WaitForExit(1000)) {
        if ($watch.Elapsed.TotalSeconds -ge $TimeoutSeconds) {
            $running.Process.Kill()
            throw "$Name timed out. Recovery files: $testRoot"
        }
        if ($watch.Elapsed.TotalSeconds -ge $nextUpdate) {
            Write-Host "Waiting for $Name ($([int]$watch.Elapsed.TotalSeconds) seconds)..."
            $nextUpdate += 30
        }
    }
    return [pscustomobject]@{ Code=$running.Process.ExitCode; Output=$running.Output.Result; Error=$running.Error.Result }
}

function Invoke-Success([string]$Name, [string[]]$Arguments, [int]$TimeoutSeconds = 60) {
    $result = Invoke-Native $Name $Arguments $TimeoutSeconds
    if ($result.Code -ne 0) { throw "$Name failed (exit $($result.Code)): $($result.Error)`n$($result.Output)" }
    return $result.Output
}

function Invoke-Terraform([string[]]$Arguments, [int]$TimeoutSeconds = 600) {
    return Invoke-Native 'terraform' (@("-chdir=$moduleRoot") + $Arguments) $TimeoutSeconds
}

function Assert-TerraformSuccess([string[]]$Arguments, [int]$TimeoutSeconds = 600) {
    $result = Invoke-Terraform $Arguments $TimeoutSeconds
    if ($result.Code -ne 0) { throw "Terraform failed (exit $($result.Code)): $($result.Error)`n$($result.Output)" }
    return $result.Output
}

function Get-HashOrMissing([string]$Path) {
    if (!(Test-Path -LiteralPath $Path)) { return '<missing>' }
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
}

function Get-ExternalState {
    return [pscustomobject]@{
        DockerContext = (Invoke-Success 'docker' @('context','show')).Trim()
        DockerConfig = Get-HashOrMissing (Join-Path $env:USERPROFILE '.docker/config.json')
        Containers = ((Invoke-Success 'docker' @('ps','-aq','--no-trunc')) -split '\r?\n' | Sort-Object) -join ','
        KubeConfig = Get-HashOrMissing (Join-Path $env:USERPROFILE '.kube/config')
        PrivateKubeConfig = Get-HashOrMissing $kubeconfig
        KubeContexts = (Invoke-Success 'kubectl' @('config','get-contexts','-o','name')).Trim()
        Namespaces = (Invoke-Success 'kubectl' ($clusterArgs + @('get','namespaces','-o','jsonpath={range .items[*]}{.metadata.name}:{.metadata.uid}{"\n"}{end}'))).Trim()
        UserPath = [Environment]::GetEnvironmentVariable('Path','User')
        MachinePath = [Environment]::GetEnvironmentVariable('Path','Machine')
    }
}

function Invoke-Sql([string]$Sql, [switch]$InvalidPassword) {
    $password = 'PGPASSWORD="$POSTGRES_PASSWORD"'
    if ($InvalidPassword) { $password = 'PGPASSWORD=invalid-smoke-password' }
    # Use the Service address: initdb may trust pod-loopback connections without a password.
    $command = $password + ' exec psql -X -w -h postgres -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At -v ON_ERROR_STOP=1 -c "$1"'
    return Invoke-Native 'kubectl' ($dbArgs + @('exec','postgres-0','--','sh','-ec',$command,'sh',$Sql))
}

function Assert-Sql([string]$Sql) {
    $result = Invoke-Sql $Sql
    if ($result.Code -ne 0) { throw "SQL smoke check failed: $($result.Error)" }
    return $result.Output.Trim()
}

function Get-NamespaceUid {
    $value = Invoke-Success 'kubectl' ($clusterArgs + @('get','namespace',$namespace,'--ignore-not-found','-o','json'))
    if ([string]::IsNullOrWhiteSpace($value)) { return $null }
    return ($value | ConvertFrom-Json).metadata.uid
}

function Wait-NewPostgresPod([string]$PreviousUid) {
    $watch = [Diagnostics.Stopwatch]::StartNew()
    while ($watch.Elapsed.TotalSeconds -lt 180) {
        $result = Invoke-Native 'kubectl' ($dbArgs + @('get','pod','postgres-0','--ignore-not-found','-o','json'))
        if ($result.Code -eq 0 -and ![string]::IsNullOrWhiteSpace($result.Output)) {
            $pod = $result.Output | ConvertFrom-Json
            if ($pod.metadata.uid -ne $PreviousUid) {
                Invoke-Success 'kubectl' ($dbArgs + @('wait','--for=condition=Ready','pod/postgres-0','--timeout=180s')) 200 | Out-Null
                return
            }
        }
        Start-Sleep -Seconds 1
    }
    throw 'PostgreSQL pod was not recreated within the expected time.'
}

$before = Get-ExternalState
try {
    Assert-Check ($null -eq (Get-NamespaceUid)) 'Disposable database namespace does not already exist'
    $created = Invoke-Success 'kubectl' ($clusterArgs + @('create','namespace',$namespace,'-o','json')) | ConvertFrom-Json
    $namespaceUid = $created.metadata.uid
    New-Item -ItemType Directory -Path $moduleRoot -Force | Out-Null
    foreach ($file in Get-ChildItem -LiteralPath (Join-Path $repoRoot 'infra/local/database') -File) {
        if ($file.Extension -eq '.tf' -or $file.Name -eq '.terraform.lock.hcl') {
            Copy-Item -LiteralPath $file.FullName -Destination $moduleRoot
        }
    }
    $migrationRoot = Join-Path $testRoot 'src/main/resources/db/migration'
    New-Item -ItemType Directory -Path $migrationRoot -Force | Out-Null
    Copy-Item -Path (Join-Path $repoRoot 'src/main/resources/db/migration/*.sql') -Destination $migrationRoot
    New-Item -ItemType Directory -Path (Join-Path $testRoot 'database') -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $repoRoot 'database/migrate.sh') -Destination (Join-Path $testRoot 'database')
    $password = 'smoke-' + [Guid]::NewGuid().ToString('N')
    $variables = [pscustomobject]@{namespace=$namespace; database_password=$password; kubeconfig_path=$kubeconfig; context=$context}
    [IO.File]::WriteAllText((Join-Path $moduleRoot 'smoke.auto.tfvars.json'), ($variables | ConvertTo-Json))
    Assert-TerraformSuccess @('fmt','-check','-recursive') | Out-Null
    Assert-TerraformSuccess @('init','-input=false','-no-color') | Out-Null
    Assert-TerraformSuccess @('validate','-no-color') | Out-Null
    Assert-Check $true 'Isolated database Terraform initializes and validates'
    Assert-TerraformSuccess @('plan','-input=false','-no-color','-out=smoke.tfplan') | Out-Null
    $plan = Assert-TerraformSuccess @('show','-json','smoke.tfplan') | ConvertFrom-Json
    foreach ($change in @($plan.resource_changes)) {
        Assert-Check ($change.type -ne 'kubernetes_namespace_v1' -or $change.mode -eq 'data') 'Database state does not manage namespace lifecycle'
        if ($change.mode -eq 'managed') {
            Assert-Check ($change.change.after.metadata[0].namespace -eq $namespace) 'Planned database resource belongs exclusively to the disposable namespace'
        }
    }
    $applyAttempted = $true
    $cleanupSafe = $false
    Assert-TerraformSuccess @('apply','-input=false','-no-color','-auto-approve','smoke.tfplan') | Out-Null
    $databaseReady = $true
    Invoke-Success 'kubectl' ($dbArgs + @('wait','--for=condition=Ready','pod/postgres-0','--timeout=180s')) 200 | Out-Null
    Assert-Check ((Assert-Sql "SELECT current_database() || ':' || current_user") -eq 'oficina_db:oficina_user') 'PostgreSQL accepts the configured credentials over TCP'
    $invalid = Invoke-Sql 'SELECT 1' -InvalidPassword
    Assert-Check ($invalid.Code -ne 0 -and $invalid.Error -match '(?i)password authentication failed') 'PostgreSQL rejects incorrect credentials over TCP'
    $historyQuery = "SELECT version || '|' || filename || '|' || checksum || '|' || installed_at::text FROM public.oficina_schema_history ORDER BY version"
    $history = Assert-Sql $historyQuery
    Assert-Check ($history -match '^1\|V001__initial_schema\.sql\|[a-f0-9]{64}\|') 'Versioned schema migration records its checksum and installation time'
    Assert-TerraformSuccess @('apply','-input=false','-no-color','-auto-approve','-replace=kubernetes_job_v1.schema') | Out-Null
    Assert-Check ((Assert-Sql $historyQuery) -ceq $history) 'Rerunning the migration preserves schema history exactly'

    $initial = Join-Path $migrationRoot 'V001__initial_schema.sql'
    $originalSql = [IO.File]::ReadAllText($initial)
    try {
        [IO.File]::WriteAllText($initial, ($originalSql + "`n-- checksum mutation for disposable namespace test`n"))
        $tampered = Invoke-Terraform @('apply','-input=false','-no-color','-auto-approve')
        Assert-Check ($tampered.Code -ne 0) 'Changed migration checksum prevents a successful Terraform migration job'
        $logs = Invoke-Success 'kubectl' ($dbArgs + @('logs','-l','app=oficina-schema','--tail=100','--all-containers=true'))
        Assert-Check ($logs -match '(?i)checksum.*changed') 'Failed migration reports the checksum mismatch'
        Assert-Check ((Assert-Sql $historyQuery) -ceq $history) 'Checksum rejection leaves previously applied history unchanged'
    } finally { [IO.File]::WriteAllText($initial, $originalSql) }
    Assert-TerraformSuccess @('apply','-input=false','-no-color','-auto-approve') | Out-Null

    $rollbackSql = Join-Path $migrationRoot 'V002__rollback_probe.sql'
    try {
        [IO.File]::WriteAllText($rollbackSql, 'CREATE TABLE public.oficina_rollback_probe (id INTEGER PRIMARY KEY); SELECT 1 / 0;')
        $broken = Invoke-Terraform @('apply','-input=false','-no-color','-auto-approve')
        Assert-Check ($broken.Code -ne 0) 'Invalid SQL aborts the migration job'
        Assert-Check ((Assert-Sql "SELECT to_regclass('public.oficina_rollback_probe') IS NULL") -eq 't') 'Failed migration rolls back its preceding DDL'
        Assert-Check ((Assert-Sql $historyQuery) -ceq $history) 'Failed SQL leaves migration history unchanged'
    } finally { if (Test-Path -LiteralPath $rollbackSql) { Remove-Item -LiteralPath $rollbackSql } }
    Assert-TerraformSuccess @('apply','-input=false','-no-color','-auto-approve') | Out-Null

    $sentinel = [Guid]::NewGuid().ToString('N')
    Assert-Sql "CREATE TABLE public.oficina_persistence_probe (marker TEXT PRIMARY KEY); INSERT INTO public.oficina_persistence_probe VALUES ('$sentinel')" | Out-Null
    $oldPod = Invoke-Success 'kubectl' ($dbArgs + @('get','pod','postgres-0','-o','json')) | ConvertFrom-Json
    Invoke-Success 'kubectl' ($dbArgs + @('delete','pod','postgres-0','--wait=true','--timeout=120s')) 140 | Out-Null
    Wait-NewPostgresPod $oldPod.metadata.uid
    Assert-Check ((Assert-Sql 'SELECT marker FROM public.oficina_persistence_probe') -eq $sentinel) 'Data survives PostgreSQL pod recreation through its PVC'

    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback,0)
    $listener.Start()
    $port = $listener.LocalEndpoint.Port
    $listener.Stop()
    $portForward = Start-Native 'kubectl' ($dbArgs + @('port-forward','--address=127.0.0.1','service/postgres',"${port}:5432"))
    $watch = [Diagnostics.Stopwatch]::StartNew()
    $listening = $false
    while ($watch.Elapsed.TotalSeconds -lt 30 -and !$portForward.Process.HasExited) {
        $client = New-Object Net.Sockets.TcpClient
        try { $client.Connect('127.0.0.1',$port); $listening=$true; break } catch { Start-Sleep -Milliseconds 250 } finally { $client.Dispose() }
    }
    Assert-Check $listening 'Private PostgreSQL port-forward listens only on loopback'
    $mavenEnvironment = @{
        JAVA_HOME=$JavaHome
        OFICINA_MAVEN_WRAPPER=(Join-Path $repoRoot 'mvnw.cmd')
        OFICINA_TEST_DATABASE_URL="jdbc:postgresql://127.0.0.1:$port/oficina_db"
        OFICINA_TEST_DATABASE_USER='oficina_user'
        OFICINA_TEST_DATABASE_PASSWORD=$password
    }
    $maven = Invoke-Native 'powershell.exe' @('-NoProfile','-Command','& $env:OFICINA_MAVEN_WRAPPER test "-Dtest=PostgreSQLSchemaIntegrationTest" -B; exit $LASTEXITCODE') 240 $mavenEnvironment
    [IO.File]::WriteAllText((Join-Path $testRoot 'hibernate-test.log'), ($maven.Output + $maven.Error))
    if ($maven.Code -ne 0) {
        $diagnostic = (($maven.Output + $maven.Error) -split '\r?\n' | Select-Object -Last 45) -join [Environment]::NewLine
        throw ('PostgreSQL Hibernate integration failed: ' + $diagnostic.Replace($password, '<redacted>'))
    }
    Assert-Check ($maven.Code -eq 0 -and $maven.Output -match 'Tests run: 3, Failures: 0, Errors: 0, Skipped: 0') 'Hibernate validates all entities and PostgreSQL round-trip/constraints pass without starting the application'
    $secondPlan = Invoke-Terraform @('plan','-input=false','-no-color','-detailed-exitcode')
    Assert-Check ($secondPlan.Code -eq 0) 'Database Terraform plan is idempotent after migration recovery'
} catch {
    $failure = $_
} finally {
    if ($null -ne $portForward -and !$portForward.Process.HasExited) { $portForward.Process.Kill(); $portForward.Process.WaitForExit() }
    try {
        if ($null -ne $namespaceUid) {
            Assert-Check ((Get-NamespaceUid) -eq $namespaceUid) 'Cleanup still owns the exact namespace UID created by this test'
            if ($applyAttempted) {
                Assert-TerraformSuccess @('destroy','-input=false','-no-color','-auto-approve') | Out-Null
                $remainingState = (Assert-TerraformSuccess @('state','list')) -split '\r?\n' | Where-Object {$_ -and $_ -notmatch '^data\.'}
                Assert-Check (@($remainingState).Count -eq 0) 'Database destroy clears its isolated managed resource state'
                Assert-Check ((Get-NamespaceUid) -eq $namespaceUid) 'Database Terraform destroy preserves the application namespace'
                if ($databaseReady) {
                    $pvc = Invoke-Success 'kubectl' ($dbArgs + @('get','pvc','data-postgres-0','-o','json')) | ConvertFrom-Json
                    Assert-Check ($pvc.status.phase -eq 'Bound') 'Database Terraform destroy retains the PostgreSQL data PVC'
                }
            }
            Invoke-Success 'kubectl' ($clusterArgs + @('delete','namespace',$namespace,'--wait=true','--timeout=180s')) 200 | Out-Null
            Assert-Check ($null -eq (Get-NamespaceUid)) 'Only the disposable namespace created by this test was removed'
            $cleanupSafe = $true
        }
    } catch { $cleanupFailure = $_; $cleanupSafe = $false }
    try {
        $after = Get-ExternalState
        foreach ($property in $before.PSObject.Properties.Name) { Assert-Check ($before.$property -ceq $after.$property) "External state unchanged: $property" }
    } catch { if ($null -eq $cleanupFailure) { $cleanupFailure=$_ } }
    if ($cleanupSafe) {
        $resolvedRoot = [IO.Path]::GetFullPath($testRoot)
        if (!$resolvedRoot.StartsWith($tempParent,[StringComparison]::OrdinalIgnoreCase) -or !(Split-Path $resolvedRoot -Leaf).StartsWith('oficina-db-smoke-')) { throw 'Unsafe temporary cleanup target.' }
        if (Test-Path -LiteralPath $resolvedRoot) { Remove-Item -LiteralPath $resolvedRoot -Recurse -Force }
    }
}
if ($null -ne $cleanupFailure) { throw "Database cleanup/preservation failed. Keep $testRoot for recovery. $cleanupFailure" }
if ($null -ne $failure) { throw $failure }
Write-Host "Local PostgreSQL smoke checks passed: $($checks.Count). Disposable database namespace removed."
