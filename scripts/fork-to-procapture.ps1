# Fork HD-Camera -> ProCapture-Studio with package/branding renames
$ErrorActionPreference = "Stop"

$src = "C:\Users\Terafort\StudioProjects\HD-Camera"
$dst = "C:\Users\Terafort\StudioProjects\ProCapture-Studio"

if (Test-Path $dst) {
    Remove-Item -Recurse -Force $dst
}

Write-Host "Copying project..."
robocopy $src $dst /E /XD .git .gradle build .idea "app\build" "cameraview\build" "sticker\build" /XF "*.iml" /NFL /NDL /NJH /NJS /nc /ns /np | Out-Null

# Remove sensitive clone-specific config
Remove-Item "$dst\app\google-services.json" -ErrorAction SilentlyContinue
Remove-Item "$dst\app\src\main\res\layout\temp_layout.xml" -ErrorAction SilentlyContinue

# Move source tree: com/tf/hd/camera/photo/video/selfiecamera -> com/terafort/procapture/studio
$oldJavaRoot = "$dst\app\src\main\java\com\tf\hd\camera\photo\video\selfiecamera"
$newJavaRoot = "$dst\app\src\main\java\com\terafort\procapture\studio"
New-Item -ItemType Directory -Force -Path (Split-Path $newJavaRoot) | Out-Null
Move-Item $oldJavaRoot $newJavaRoot
Remove-Item "$dst\app\src\main\java\com\tf" -Recurse -Force -ErrorAction SilentlyContinue

# Structural renames inside package
Rename-Item "$newJavaRoot\common" "shared"
Rename-Item "$newJavaRoot\features" "features" -ErrorAction SilentlyContinue
if (Test-Path "$newJavaRoot\ui") {
    Rename-Item "$newJavaRoot\ui" "features"
}

# Rename Application class file
if (Test-Path "$newJavaRoot\MyApp.kt") {
    Rename-Item "$newJavaRoot\MyApp.kt" "ProCaptureApp.kt"
}

# Rename drawable branding asset
$hdCamDrawable = "$dst\app\src\main\res\drawable\f_hd_camera.xml"
if (Test-Path $hdCamDrawable) {
    Rename-Item $hdCamDrawable "f_pro_capture.xml"
}

# Move test packages
$oldTest = "$dst\app\src\androidTest\java\com\terafort\hd"
$newTest = "$dst\app\src\androidTest\java\com\terafort\procapture\studio\test"
if (Test-Path $oldTest) {
    New-Item -ItemType Directory -Force -Path (Split-Path $newTest) | Out-Null
    Move-Item "$oldTest\*" $newTest -Force
    Remove-Item "$dst\app\src\androidTest\java\com\terafort\hd" -Recurse -Force -ErrorAction SilentlyContinue
}

$oldUnitTest = "$dst\app\src\test\java\com\terafort\hd"
$newUnitTest = "$dst\app\src\test\java\com\terafort\procapture\studio\test"
if (Test-Path $oldUnitTest) {
    New-Item -ItemType Directory -Force -Path (Split-Path $newUnitTest) | Out-Null
    Move-Item "$oldUnitTest\*" $newUnitTest -Force
    Remove-Item "$dst\app\src\test\java\com\terafort\hd" -Recurse -Force -ErrorAction SilentlyContinue
}

# Text replacements (order: longest/most-specific first)
$replacements = @(
    @{ Old = 'com.tf.hd.camera.photo.video.selfiecamera.common'; New = 'com.terafort.procapture.studio.shared' },
    @{ Old = 'com.tf.hd.camera.photo.video.selfiecamera.ui'; New = 'com.terafort.procapture.studio.features' },
    @{ Old = 'com.tf.hd.camera.photo.video.selfiecamera'; New = 'com.terafort.procapture.studio' },
    @{ Old = 'com/terafort/hd/camera/photo/video/selfiecamera'; New = 'com/terafort/procapture/studio' },
    @{ Old = 'com/terafort/hd'; New = 'com/terafort/procapture/studio/test' },
    @{ Old = 'Base.Theme.HDCamera'; New = 'Base.Theme.ProCapture' },
    @{ Old = 'Theme.HDCamera'; New = 'Theme.ProCapture' },
    @{ Old = 'HD Camera-'; New = 'Pro Capture-' },
    @{ Old = 'HD-Camera'; New = 'ProCapture-Studio' },
    @{ Old = 'HD Camera'; New = 'Pro Capture' },
    @{ Old = 'HDCamera'; New = 'ProCapture' },
    @{ Old = '@drawable/f_hd_camera'; New = '@drawable/f_pro_capture' },
    @{ Old = 'R.drawable.f_hd_camera'; New = 'R.drawable.f_pro_capture' },
    @{ Old = '@string/hd_camera'; New = '@string/pro_capture' },
    @{ Old = 'R.string.hd_camera'; New = 'R.string.pro_capture' },
    @{ Old = 'name="hd_camera"'; New = 'name="pro_capture"' },
    @{ Old = 'const val APP_NAME = "hd_camera"'; New = 'const val APP_NAME = "procapture_studio"' },
    @{ Old = 'VOICE-CHANGER'; New = 'PRO-CAPTURE-STUDIO' },
    @{ Old = 'my_app_preferences'; New = 'procapture_preferences' },
    @{ Old = 'stringPreferencesKey("artora")'; New = 'stringPreferencesKey("procapture")' },
    @{ Old = 'class MyApp'; New = 'class ProCaptureApp' },
    @{ Old = 'android:name=".MyApp"'; New = 'android:name=".ProCaptureApp"' },
    @{ Old = 'MyApp.'; New = 'ProCaptureApp.' },
    @{ Old = 'MyApp::'; New = 'ProCaptureApp::' },
    @{ Old = 'MyApp,'; New = 'ProCaptureApp,' },
    @{ Old = '(MyApp)'; New = '(ProCaptureApp)' },
    @{ Old = ' is MyApp'; New = ' is ProCaptureApp' },
    @{ Old = 'import com.terafort.procapture.studio.MyApp'; New = 'import com.terafort.procapture.studio.ProCaptureApp' },
    @{ Old = 'weekly_pro_new'; New = 'weekly_pro_procapture' },
    @{ Old = '3ziwu8n559og'; New = 'YOUR_ADJUST_APP_TOKEN' },
    @{ Old = 'k9xld7'; New = 'YOUR_AD_IMPRESSION_TOKEN' },
    @{ Old = '1cp13e'; New = 'YOUR_ADJUST_HOME_TOKEN' },
    @{ Old = 'gc8nb0'; New = 'YOUR_ADJUST_SUBSCRIPTION_TOKEN' },
    @{ Old = 'com.terafort.hd_camera'; New = 'com.terafort.procapture.studio' },
    @{ Old = 'package com.terafort.hd'; New = 'package com.terafort.procapture.studio.test' },
    @{ Old = '1550801282668783'; New = 'YOUR_FACEBOOK_APP_ID' },
    @{ Old = '06cd1f3314fc780488833c6303365f07'; New = 'YOUR_FACEBOOK_CLIENT_TOKEN' },
    @{ Old = 'ca-app-pub-5972202469838280~9729162961'; New = 'ca-app-pub-3940256099942544~3347511713' }
)

$textExtensions = @('.kt', '.java', '.xml', '.kts', '.gradle', '.properties', '.toml', '.pro', '.json', '.md', '.name')
$files = Get-ChildItem -Path $dst -Recurse -File | Where-Object {
    $textExtensions -contains $_.Extension.ToLower()
}

foreach ($file in $files) {
    $content = [System.IO.File]::ReadAllText($file.FullName)
    $original = $content
    foreach ($r in $replacements) {
        $content = $content.Replace($r.Old, $r.New)
    }
    if ($content -ne $original) {
        [System.IO.File]::WriteAllText($file.FullName, $content)
    }
}

# Placeholder google-services.json for new package (replace with real Firebase project)
$googleServices = @'
{
  "project_info": {
    "project_number": "000000000000",
    "project_id": "procapture-studio-placeholder",
    "storage_bucket": "procapture-studio-placeholder.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:000000000000:android:0000000000000000000000",
        "android_client_info": {
          "package_name": "com.terafort.procapture.studio"
        }
      },
      "oauth_client": [],
      "api_key": [
        {
          "current_key": "YOUR_FIREBASE_API_KEY"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": []
        }
      }
    }
  ],
  "configuration_version": "1"
}
'@
Set-Content -Path "$dst\app\google-services.json" -Value $googleServices -Encoding UTF8

# Setup notes
$setupNotes = @'
# Pro Capture Studio

Fork of HD-Camera with distinct package, branding, and structure to avoid clone conflicts.

## Identity
- **App name:** Pro Capture
- **Application ID:** com.terafort.procapture.studio
- **Backend app name (X-App-Name):** procapture_studio

## Package structure changes
- `common` -> `shared`
- `ui` -> `features`

## Required setup before release
1. Replace `app/google-services.json` with your Firebase project (package must match).
2. Set AdMob app/unit IDs in `app/build.gradle.kts` release block.
3. Set Adjust tokens in `utils/AdjustConstant.kt`.
4. Set Facebook App ID / Client Token in `AndroidManifest.xml`.
5. Create Play Console IAP product: `weekly_pro_procapture`.
6. Register `procapture_studio` with your backend team if using AI generation APIs.

## Build
```powershell
.\gradlew.bat assembleDebug
```
'@
Set-Content -Path "$dst\SETUP.md" -Value $setupNotes -Encoding UTF8

Write-Host "Done. New project at $dst"
