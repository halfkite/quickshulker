param(
    [string[]]$Targets = @(),
    [Parameter(Mandatory)][string]$JavaHome,
    [Parameter(Mandatory)][string]$Archiver,
    [string]$ExistingArtifact,
    [string[]]$GradleArguments = @('--console=plain', '--no-daemon')
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$metadata = Get-Content -LiteralPath (Join-Path $repo 'development.json') -Raw | ConvertFrom-Json
$groups = @($metadata.fabricCompatibilityGroups | Where-Object { $Targets.Count -eq 0 -or $Targets -contains $_.buildTarget })
if ($groups.Count -eq 0) { throw 'No compatibility groups selected' }
if ($ExistingArtifact -and $groups.Count -ne 1) { throw 'ExistingArtifact requires exactly one group' }
$report = Join-Path $repo ('build/compatibility/' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $report | Out-Null
$previousJava = $env:JAVA_HOME
$records = [Collections.Generic.List[object]]::new()
try {
    $env:JAVA_HOME = $JavaHome
    foreach ($group in $groups) {
        $label = $group.minecraftVersions[0] + '-' + $group.minecraftVersions[-1]
        $version = $group.buildTarget -replace '-fabric$', ''
        if ($ExistingArtifact) {
            $artifact = (Resolve-Path -LiteralPath $ExistingArtifact).Path
            $archive = Split-Path $artifact -Parent
        } else {
            Write-Output "BUILD BEGIN $label"
            $buildLog = Join-Path $report "$label-build.log"
            & (Join-Path $repo 'build.ps1') -Version $version -JavaHome $JavaHome -Tasks @('build','printArtifactName') -GradleArguments $GradleArguments 2>&1 | Tee-Object -FilePath $buildLog | Out-Null
            if ($LASTEXITCODE -ne 0) { Get-Content $buildLog -Tail 40; throw "Build failed: $label" }
            $artifactLine = Get-Content $buildLog | Where-Object { $_ -like 'ARTIFACT_FILE=*' } | Select-Object -Last 1
            if (!$artifactLine) { throw 'Build did not identify its installable artifact' }
            $built = $artifactLine.Substring('ARTIFACT_FILE='.Length)
            $argumentText = ($GradleArguments | ForEach-Object { "'" + $_.Replace("'", "''") + "'" }) -join ','
            $command = ".\build.ps1 -Version $version -JavaHome '$JavaHome' -Tasks @('build','printArtifactName') -GradleArguments @($argumentText)"
            $archive = & python $Archiver --artifact $built --output-root (Join-Path $repo 'mod-builds') --mod-name QuickShulkerMore --game-version $label --build-command $command
            if ($LASTEXITCODE -ne 0) { throw "Archiving failed: $label" }
            $archive = [string]($archive | Select-Object -Last 1)
            $artifact = Join-Path $archive ([IO.Path]::GetFileName($built))
            Copy-Item -LiteralPath $buildLog -Destination (Join-Path $archive 'build.log')
            Write-Output "BUILD PASS $label $archive"
        }
        $digest = (Get-FileHash -LiteralPath $artifact -Algorithm SHA256).Hash
        foreach ($game in $group.minecraftVersions) {
            $target = $metadata.targets.PSObject.Properties["$game-fabric"].Value
            $project = [IO.Path]::GetFullPath((Join-Path $repo $target.project))
            $log = Join-Path $report "$game-runtime.log"
            Write-Output "RUNTIME BEGIN $game shared SHA256=$digest"
            & (Join-Path $project 'gradlew.bat') -p $project regressionTest worldRegressionTest runCompatibilityClient -I (Join-Path $repo 'gradle/verify-compatible-jar.init.gradle') "-PcompatibilityJar=$artifact" @GradleArguments 2>&1 | Tee-Object -FilePath $log | Out-Null
            if ($LASTEXITCODE -ne 0) { Get-Content $log -Tail 60; throw "Shared JAR failed on Minecraft $game; see $log" }
            $output = Get-Content -LiteralPath $log -Raw
            if ($output -notmatch 'external artifact verified:' -or $output -notmatch 'shared JAR client smoke passed:') { throw "Missing external JAR/client verification for $game" }
            $xmlPath = Join-Path $project 'build/world-regression-run/reports/world-tests.xml'
            & python (Join-Path $repo 'scripts/python/package_fabric_build.py') verify-runtime --repo-root $repo --target "$game-fabric" --jar $artifact --log $log --world-report $xmlPath
            if ($LASTEXITCODE -ne 0) { throw "Invalid shared-artifact runtime evidence for $game" }
            [xml]$xml = Get-Content -LiteralPath $xmlPath -Raw
            $tests = @($xml.SelectNodes('//testcase')).Count
            if ($tests -eq 0 -or $xml.SelectNodes('//failure|//error').Count -gt 0) { throw "World tests failed for $game" }
            if ((Get-FileHash -LiteralPath $artifact -Algorithm SHA256).Hash -ne $digest) { throw 'Shared artifact changed during validation' }
            Copy-Item -LiteralPath $log -Destination (Join-Path $archive "runtime-$game.log")
            Copy-Item -LiteralPath $xmlPath -Destination (Join-Path $archive "world-tests-$game.xml")
            $records.Add([pscustomobject]@{group=$label;minecraft=$game;artifact=$artifact;sha256=$digest;worldTests=$tests;clientSmoke='passed';status='passed'})
            $records | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $report 'results.json') -Encoding utf8
            Write-Output "RUNTIME PASS $game worldTests=$tests clientSmoke=passed"
        }
        @($records | Where-Object group -eq $label) | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $archive 'compatibility-results.json') -Encoding utf8
    }
} finally {
    $env:JAVA_HOME = $previousJava
}
Write-Output "COMPATIBILITY REPORT $report"
