param(
    [Parameter(Mandatory)][string[]]$Versions,
    [Parameter(Mandatory)][string]$Jdk21,
    [Parameter(Mandatory)][string]$Jdk25,
    [Parameter(Mandatory)][string]$Archiver
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$metadata = Get-Content -LiteralPath (Join-Path $root 'development.json') -Raw | ConvertFrom-Json
$reportDir = Join-Path $root ('build/port-queue/' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $reportDir | Out-Null
$records = [Collections.Generic.List[object]]::new()
Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach ($version in $Versions) {
    $target = $metadata.targets.PSObject.Properties["$version-fabric"].Value
    if ($null -eq $target) { throw "Unknown Fabric target: $version" }
    $project = [IO.Path]::GetFullPath((Join-Path $root $target.project))
    $jdk = if ($target.java -eq 25) { $Jdk25 } else { $Jdk21 }
    $log = Join-Path $reportDir "$version.log"
    $command = ".\build.ps1 -Version $version -JavaHome '$jdk' -Tasks @('build','printArtifactName') -GradleArguments @('-PlocalCompatDependencies=true','--offline','--console=plain','--no-daemon')"
    Write-Output "QUEUE BEGIN $version"
    try {
        & (Join-Path $root 'build.ps1') -Version $version -JavaHome $jdk -Tasks @('build','printArtifactName') -GradleArguments @('-PlocalCompatDependencies=true','--offline','--console=plain','--no-daemon') 2>&1 | Tee-Object -FilePath $log
        if ($LASTEXITCODE -ne 0) { throw "Build failed with exit code $LASTEXITCODE; see $log" }
        $artifactLine = Get-Content -LiteralPath $log | Where-Object { $_ -like 'ARTIFACT_FILE=*' } | Select-Object -Last 1
        if (!$artifactLine) { throw 'Build did not report an installable artifact' }
        $artifact = $artifactLine.Substring('ARTIFACT_FILE='.Length)
        $archive = & python $Archiver --artifact $artifact --output-root (Join-Path $root 'mod-builds') --mod-name QuickShulkerMore --game-version $version --build-command $command
        if ($LASTEXITCODE -ne 0) { throw 'Artifact archiving failed' }
        $archive = [string]($archive | Select-Object -Last 1)
        Copy-Item -LiteralPath $log -Destination (Join-Path $archive 'build.log')
        $xmlPath = Join-Path $project 'build/world-regression-run/reports/world-tests.xml'
        Copy-Item -LiteralPath $xmlPath -Destination (Join-Path $archive 'world-tests.xml')
        Copy-Item -LiteralPath (Join-Path $project 'build/regression-run/logs/latest.log') -Destination (Join-Path $archive 'component-tests.log')
        [xml]$xml = Get-Content -LiteralPath $xmlPath -Raw
        $cases = @($xml.SelectNodes('//testcase'))
        if ($cases.Count -eq 0 -or $xml.SelectNodes('//failure|//error').Count -ne 0) { throw 'World test report is empty or contains failures' }
        if (@($cases | Where-Object { $_.name -match 'same_?type_?container_?nesting' }).Count -ne 1) { throw 'Same-type nesting regression did not run' }
        $zip = [IO.Compression.ZipFile]::OpenRead($artifact)
        try {
            $reader = [IO.StreamReader]::new($zip.GetEntry('fabric.mod.json').Open())
            try { $mod = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
            if ($mod.name -ne 'QuickShulkerMore' -or $mod.version -ne "1.0.0-$version") { throw 'Artifact metadata does not match target version' }
            if (@($zip.Entries | Where-Object { $_.FullName -match '(ContainerRegressionTest|PortableWorldTests|MixedWireRegression|upstreamwire/|carpetamsaddition/CarpetAMSAdditionSettings)' }).Count -gt 0) { throw 'Test or optional API class leaked into artifact' }
        } finally { $zip.Dispose() }
        $hash = (Get-FileHash -LiteralPath $artifact -Algorithm SHA256).Hash
        $componentLog = Get-Content -LiteralPath (Join-Path $archive 'component-tests.log')
        $checks = $componentLog | Select-String -Pattern 'regression checks passed: \d+' | Select-Object -Last 1
        $records.Add([pscustomobject]@{version=$version;status='passed';archive=$archive;artifact=[IO.Path]::GetFileName($artifact);sha256=$hash;worldTests=$cases.Count;componentChecks=[string]$checks})
        Write-Output "QUEUE PASS $version $archive"
    } catch {
        $records.Add([pscustomobject]@{version=$version;status='failed';error=$_.Exception.Message;log=$log})
        Write-Output "QUEUE FAIL $version $($_.Exception.Message)"
    }
    $records | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $reportDir 'summary.json') -Encoding utf8
}
Write-Output "QUEUE SUMMARY $(Join-Path $reportDir 'summary.json')"
if (@($records | Where-Object status -eq 'failed').Count -gt 0) { exit 1 }
