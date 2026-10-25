param(
    [ValidateSet('orchestrate','server','quiet','loud','plain')][string]$Mode='orchestrate',
    [Parameter(Mandatory)][string]$Jar,
    [Parameter(Mandatory)][string]$JavaHome
)
$ErrorActionPreference='Stop'
$project=$PSScriptRoot
$Jar=(Resolve-Path -LiteralPath $Jar).Path
$env:JAVA_HOME=$JavaHome
if($Mode -ne 'orchestrate'){
    $arguments=@('-p',$project,'runSoundTest','-I',(Join-Path $project 'sound-test.init.gradle'),"-PsoundMode=$Mode","-PsoundArtifact=$Jar",'--console=plain','--no-daemon','--offline')
    & (Join-Path $project 'gradlew.bat') @arguments
    exit $LASTEXITCODE
}
$reports=Join-Path $project ('build/sound-test/reports/'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $reports | Out-Null
$children=@{}
function Start-Test([string]$kind){
    $arguments=@('-NoProfile','-File',('"'+$PSCommandPath+'"'),'-Mode',$kind,'-Jar',('"'+$Jar+'"'),'-JavaHome',('"'+$JavaHome+'"'))
    $process=Start-Process -FilePath (Join-Path $PSHOME 'pwsh.exe') -ArgumentList $arguments -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $reports "$kind.log") -RedirectStandardError (Join-Path $reports "$kind.err.log")
    $children[$kind]=$process
    Write-Output "SOUND START $kind PID=$($process.Id)"
}
function Wait-Marker([string]$kind,[string]$pattern){
    $deadline=(Get-Date).AddMinutes(4)
    while((Get-Date) -lt $deadline){
        $path=Join-Path $reports "$kind.log"
        if((Test-Path -LiteralPath $path) -and (Select-String -LiteralPath $path -Pattern $pattern -Quiet)){return}
        foreach($entry in $children.GetEnumerator()){
            if($entry.Value.HasExited -and $entry.Value.ExitCode -ne 0){
                Get-Content (Join-Path $reports "$($entry.Key).log") -Tail 20
                Get-Content (Join-Path $reports "$($entry.Key).err.log") -Tail 15
                throw "Sound test child failed: $($entry.Key)"
            }
        }
        if($children[$kind].HasExited){Get-Content $path -Tail 25;Get-Content (Join-Path $reports "$kind.err.log") -Tail 15;throw "Sound test exited early: $kind"}
        Start-Sleep -Milliseconds 500
    }
    throw "Sound test marker timeout: $kind $pattern"
}
try{
    Start-Test 'server'
    Wait-Marker 'server' 'Done \('
    foreach($kind in @('quiet','loud','plain')){
        Start-Test $kind
        Wait-Marker 'server' "Sound_$kind.*logged in"
    }
    Wait-Marker 'server' 'SOUND_SERVER_SUCCESS:'
    foreach($kind in @('quiet','loud','plain')){
        Wait-Marker $kind 'SOUND_CLIENT_SUCCESS:'
        $children[$kind].WaitForExit(30000) | Out-Null
        if(!$children[$kind].HasExited -or $children[$kind].ExitCode -ne 0){throw "Sound client failed: $kind"}
    }
    $children['server'].WaitForExit(30000) | Out-Null
    if(!$children['server'].HasExited -or $children['server'].ExitCode -ne 0){throw 'Sound server failed'}
    Write-Output "SOUND TEST PASSED: $reports"
    foreach($kind in @('quiet','loud','plain')){Select-String -LiteralPath (Join-Path $reports "$kind.log") -Pattern 'SOUND_CLIENT_PASS:|SOUND_CLIENT_SUCCESS:' | ForEach-Object {$_.Line}}
}finally{
    # Stop only children created by this test. Never touch unrelated game/Gradle processes.
    foreach($process in $children.Values){if(!$process.HasExited){& taskkill.exe /PID $process.Id /T /F | Out-Null}}
}
