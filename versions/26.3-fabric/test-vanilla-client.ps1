param([string]$JavaHome = 'C:/Program Files/Java/jdk-25.0.3')
$ErrorActionPreference = 'Stop'
$loomCache = Join-Path $env:USERPROFILE '.gradle/caches/fabric-loom'
$info = Get-Content -LiteralPath (Join-Path $loomCache '26.3/mojang_minecraft_info.json') -Raw | ConvertFrom-Json
$moduleCache = Join-Path $env:USERPROFILE '.gradle/caches/modules-2/files-2.1'
$runtimeFolder = Join-Path $PSScriptRoot 'build/login-test/vanilla'
New-Item -ItemType Directory -Path $runtimeFolder -Force | Out-Null
$classpath = [Collections.Generic.List[string]]::new()
$clientJar = Join-Path $loomCache '26.3/minecraft-client.jar'
if ((Get-FileHash -LiteralPath $clientJar -Algorithm SHA1).Hash -ne $info.downloads.client.sha1) {
    throw 'Official Minecraft client hash mismatch'
}
$classpath.Add($clientJar)
foreach ($library in $info.libraries) {
    $allowed = $null -eq $library.rules
    foreach ($rule in $library.rules) {
        $matchesOs = $null -eq $rule.os -or (($null -eq $rule.os.name -or $rule.os.name -eq 'windows') -and
            ($null -eq $rule.os.arch -or $rule.os.arch -in @('x86_64','amd64')))
        if ($matchesOs) { $allowed = $rule.action -eq 'allow' }
    }
    if (!$allowed -or !$library.downloads.artifact) { continue }
    $parts = $library.name.Split(':')
    $filename = [IO.Path]::GetFileName($library.downloads.artifact.path)
    $module = Join-Path $moduleCache ($parts[0] + '/' + $parts[1] + '/' + $parts[2])
    $file = Get-ChildItem -LiteralPath $module -Recurse -Filter $filename -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -eq $file) {
        $download = Join-Path $runtimeFolder ('libraries/' + $library.downloads.artifact.path)
        New-Item -ItemType Directory -Path (Split-Path $download -Parent) -Force | Out-Null
        if (!(Test-Path -LiteralPath $download)) { Invoke-WebRequest -Uri $library.downloads.artifact.url -OutFile $download }
        $file = Get-Item -LiteralPath $download
    }
    if ((Get-FileHash -LiteralPath $file.FullName -Algorithm SHA1).Hash -ne $library.downloads.artifact.sha1) {
        throw "Official library hash mismatch: $filename"
    }
    $classpath.Add($file.FullName)
}
if (@($classpath | Where-Object { [IO.Path]::GetFileName($_) -match '^(fabric|quickshulker)' }).Count -gt 0) {
    throw 'A mod or loader entered the vanilla client classpath'
}
Write-Output ('VANILLA_CLASSPATH: official Minecraft client plus ' + ($classpath.Count - 1) + ' verified Mojang-manifest libraries; no mod loader')
$readiness = [Diagnostics.Stopwatch]::StartNew()
while (!(Get-NetTCPConnection -LocalPort 25589 -State Listen -ErrorAction SilentlyContinue)) {
    if ($readiness.Elapsed.TotalMinutes -ge 3) { throw 'Login test server did not start listening on port 25589' }
    Start-Sleep -Milliseconds 500
}
$arguments = @('-Xmx2G','-XX:StackShadowPages=32','--enable-native-access=ALL-UNNAMED',
    '--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED','-cp',($classpath -join ';'),
    'net.minecraft.client.main.Main','--version','26.3','--username','VanillaProbe',
    '--uuid','4b87610b4ccc4fbbadbf432e91d2db0b','--accessToken','0',
    '--gameDir',$runtimeFolder,'--assetsDir',(Join-Path $loomCache 'assets'),'--assetIndex',$info.assetIndex.id,
    '--quickPlayMultiplayer','127.0.0.1:25589','--width','854','--height','480')
Push-Location $runtimeFolder
try {
    & (Join-Path $JavaHome 'bin/java.exe') @arguments
    $result = $LASTEXITCODE
} finally { Pop-Location }
exit $result
