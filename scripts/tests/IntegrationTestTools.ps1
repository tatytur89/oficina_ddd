# Shared process helpers for integration smoke tests. The caller supplies repoRoot and testRoot.
function Start-TestTool([string]$Name, [string[]]$Arguments, [hashtable]$Environment = @{}) {
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

function Invoke-TestTool([string]$Name, [string[]]$Arguments, [int]$TimeoutSeconds = 60, [hashtable]$Environment = @{}) {
    $running = Start-TestTool $Name $Arguments $Environment
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

function Assert-TestTool([string]$Name, [string[]]$Arguments, [int]$TimeoutSeconds = 60) {
    $result = Invoke-TestTool $Name $Arguments $TimeoutSeconds
    if ($result.Code -ne 0) { throw "$Name failed (exit $($result.Code)): $($result.Error)`n$($result.Output)" }
    return $result.Output
}

function Get-TestFileHash([string]$Path) {
    if (!(Test-Path -LiteralPath $Path)) { return '<missing>' }
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
}
