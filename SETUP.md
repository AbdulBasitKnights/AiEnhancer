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
