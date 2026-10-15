param(
    [string]$Sdk = "$env:LOCALAPPDATA/Android/Sdk",
    [string]$Serial = 'emulator-5580',
    [string]$Jdk = 'C:/Program Files/Java/jdk-21',
    [ValidateSet('1280','1920')][string]$Profile = '1280'
)
$ErrorActionPreference = 'Stop'
$adb = Join-Path $Sdk 'platform-tools/adb.exe'
function Invoke-Adb([string[]]$Arguments) {
    & $adb -s $Serial @Arguments
    if ($LASTEXITCODE -ne 0) { throw "adb failed: $Arguments" }
}
function Invoke-Gradle([string[]]$Arguments) {
    $previousPreference = $ErrorActionPreference
    try {
        # Windows PowerShell turns native stderr warnings into ErrorRecords.
        # Keep the diagnostic, and use Gradle's real exit code as the result.
        $ErrorActionPreference = 'Continue'
        & ./gradlew.bat @Arguments 2>&1 | ForEach-Object { $_.ToString() }
        $gradleExit = $LASTEXITCODE
    } finally { $ErrorActionPreference = $previousPreference }
    if ($gradleExit -ne 0) { throw "Gradle failed ($gradleExit): $Arguments" }
}
# Do not let a stale/default ADB selection mutate any physical device.
if ($Serial -notmatch '^emulator-[0-9]+$') { throw 'Only local Android Emulator serials are supported.' }
$qemu = (& $adb -s $Serial shell getprop ro.kernel.qemu).Trim()
$api = (& $adb -s $Serial shell getprop ro.build.version.sdk).Trim()
if ($qemu -ne '1' -or $api -ne '29') { throw 'Requires a booted API 29 Android Emulator.' }
$env:JAVA_HOME = $Jdk
$env:Path = "$Jdk/bin;$env:Path"
$env:ANDROID_SERIAL = $Serial
if ($Profile -eq '1280') { $size = '1280x720'; $density = '160'; $width = 1280; $height = 720 }
else { $size = '1920x1080'; $density = '320'; $width = 1920; $height = 1080 }
# This AVD's natural display is landscape. API29 clamps forced dimensions to
# twice each physical dimension, so requesting a portrait1920height is incorrect.
Invoke-Adb -Arguments @('shell','wm','size',$size)
Invoke-Adb -Arguments @('shell','wm','density',$density)
Invoke-Adb -Arguments @('shell','settings','put','system','accelerometer_rotation','0')
Invoke-Adb -Arguments @('shell','settings','put','system','user_rotation','0')
# Keep normal platform transitions. A previous interrupted run left zero scales;
# the API29 image then aborted SurfaceFlinger while tearing down input windows.
# App-level reduced motion is verified separately by instrumentation.
foreach ($animationSetting in @('window_animation_scale','transition_animation_scale','animator_duration_scale')) {
    Invoke-Adb -Arguments @('shell','settings','put','global',$animationSetting,'1.0')
    if ((& $adb -s $Serial shell settings get global $animationSetting).Trim() -ne '1.0') {
        throw "Could not establish normal emulator transition setting: $animationSetting"
    }
}
$viewportConfirmed = $false
for ($attempt = 0; $attempt -lt 20; $attempt++) {
    $display = (& $adb -s $Serial shell dumpsys display) -join "`n"
    if ($display -match "mOverrideDisplayInfo=DisplayInfo\{[^\r\n]*real $width x $height,[^\r\n]*density $density ") {
        $viewportConfirmed = $true; break
    }
    Start-Sleep -Milliseconds 150
}
if (-not $viewportConfirmed) { throw "Actual landscape viewport is not ${width}x${height}/$density; refusing a mislabeled profile." }
Write-Output "Confirmed actual landscape viewport: ${width}x${height}, ${density}dpi."
Push-Location (Split-Path $PSScriptRoot)
try {
    Invoke-Gradle -Arguments @(':app:assembleDebug',':app:testDebugUnitTest',':app:lintDebug','--console=plain')
    Invoke-Adb -Arguments @('install','-r','app/build/outputs/apk/debug/app-debug.apk')
    Invoke-Adb -Arguments @('shell','cmd','package','set-home-activity','vn.tnlauncher.app/.ui.HomeActivity')
    Invoke-Gradle -Arguments @(':app:connectedDebugAndroidTest','--console=plain')
} finally { Pop-Location }
