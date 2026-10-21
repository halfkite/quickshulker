param(
    [Parameter(Mandatory)][string[]]$Versions,
    [Parameter(Mandatory)][string]$Jdk21,
    [Parameter(Mandatory)][string]$Jdk25,
    [switch]$Offline
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$metadata = Get-Content -LiteralPath (Join-Path $repo 'development.json') -Raw | ConvertFrom-Json
$report = Join-Path $repo ('build/runtime-validation/' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $report | Out-Null
$records = [Collections.Generic.List[object]]::new()
$previousJava = $env:JAVA_HOME
try {
    foreach ($version in $Versions) {
        $target = $metadata.targets.PSObject.Properties["$version-fabric"].Value
        if ($null -eq $target) { throw "Unknown Fabric runtime: $version" }
        $project = [IO.Path]::GetFullPath((Join-Path $repo $target.project))
        $modVersion = (Select-String -LiteralPath (Join-Path $project 'gradle.properties') -Pattern '^mod_version=(.+)$').Matches.Groups[1].Value
        $group = $metadata.fabricCompatibilityGroups | Where-Object { $_.minecraftVersions -contains $version } | Select-Object -First 1
        $label = if ($group) { $group.minecraftVersions[0] + '-' + $group.minecraftVersions[-1] } else { $version }
        $pattern = "quickshulkermore-$modVersion-mc$label-fabric-*.jar"
        $artifact = Get-ChildItem -Path (Join-Path $repo "mod-builds/*/$pattern") -File | Sort-Object FullName -Descending | Select-Object -First 1
        if ($null -eq $artifact) { throw "No archived artifact matches $pattern" }
        $archive = $artifact.DirectoryName
        $manifest = Get-Content -LiteralPath (Join-Path $archive 'build-manifest.json') -Raw | ConvertFrom-Json
        $digest = (Get-FileHash -LiteralPath $artifact.FullName -Algorithm SHA256).Hash
        $entry = @($manifest.files | Where-Object path -eq $artifact.Name)
        if ($entry.Count -ne 1 -or $entry[0].sha256 -ne $digest) { throw 'Archived artifact failed its SHA-256 check' }
        $env:JAVA_HOME = if ($target.java -eq 25) { $Jdk25 } else { $Jdk21 }
        $log = Join-Path $report "$version.log"
        Write-Output "ARTIFACT RUNTIME BEGIN $version SHA256=$digest"
        $networkArguments = if ($Offline) { @('--offline') } else { @() }
        & (Join-Path $project 'gradlew.bat') -p $project regressionTest worldRegressionTest runCompatibilityClient -I (Join-Path $repo 'gradle/verify-compatible-jar.init.gradle') "-PcompatibilityJar=$($artifact.FullName)" -PlocalCompatDependencies=true --console=plain --no-daemon @networkArguments 2>&1 | Tee-Object -FilePath $log | Out-Null
        if ($LASTEXITCODE -ne 0) { Get-Content -LiteralPath $log -Tail 50; throw "Runtime validation failed: $version" }
        $worldReport = Join-Path $project 'build/world-regression-run/reports/world-tests.xml'
        $proof = & python (Join-Path $repo 'scripts/python/package_fabric_build.py') verify-runtime --repo-root $repo --target "$version-fabric" --jar $artifact.FullName --log $log --world-report $worldReport
        if ($LASTEXITCODE -ne 0) { throw "Runtime evidence failed validation: $version" }
        if ((Get-FileHash -LiteralPath $artifact.FullName -Algorithm SHA256).Hash -ne $digest) { throw 'Artifact changed during validation' }
        Copy-Item -LiteralPath $log -Destination (Join-Path $archive "runtime-$version.log")
        Copy-Item -LiteralPath $worldReport -Destination (Join-Path $archive "world-tests-$version.xml")
        $records.Add(($proof | ConvertFrom-Json))
        $records | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $report 'results.json') -Encoding utf8
        Write-Output "ARTIFACT RUNTIME PASS $version"
    }
} finally { $env:JAVA_HOME = $previousJava }
Write-Output "ARTIFACT RUNTIME REPORT $report"
