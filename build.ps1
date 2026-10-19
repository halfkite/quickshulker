param(
    [ValidateSet('26.3', '1.21.1')][string]$Version = '26.3',
    [ValidateSet('fabric', 'neoforge')][string]$Loader = 'fabric',
    [string]$JavaHome = $env:JAVA_HOME,
    [string[]]$Tasks = @('build'),
    [string[]]$GradleArguments = @('--console=plain', '--no-daemon')
)
$ErrorActionPreference = 'Stop'
$metadata = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'development.json') -Raw | ConvertFrom-Json
$key = "$Version-$Loader"
$target = $metadata.targets.PSObject.Properties[$key]
if ($null -eq $target) { throw "Unsupported target: $key" }
$project = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot $target.Value.project))
$wrapper = Join-Path $project 'gradlew.bat'
if (-not (Test-Path -LiteralPath $wrapper)) { throw "Missing wrapper: $wrapper" }
$previousJavaHome = $env:JAVA_HOME
try {
    if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
    $java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
    $versionOutput = (& $java -version 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or $versionOutput -notmatch 'version "(\d+)') { throw 'Cannot determine Java version.' }
    $major = [int]$Matches[1]
    if ($major -ne $target.Value.java) { throw "Target $key requires JDK $($target.Value.java); found $major. Pass -JavaHome." }
    Write-Host "Building $key (primary version: $($metadata.primaryMinecraftVersion))"
    & $wrapper '-p' $project @Tasks @GradleArguments
    $result = $LASTEXITCODE
} finally {
    $env:JAVA_HOME = $previousJavaHome
}
exit $result
