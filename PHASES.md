# Ai Face Aging — Phase Tracker

Forked from ProCapture-Studio (**source untouched**).  
Internal Kotlin package still `com.terafort.procapture.studio` for fast port.  
`applicationId` = `com.terafort.aifaceaging`.

## Done
- [x] **Phase 0 scaffold** — project at `StudioProjects/Ai-Face-Aging`
- [x] Dropped `:cameraview` module + camera feature package
- [x] Rebrand: app name, `APP_NAME=ai_face_aging`, version `1.0.0`
- [x] Camera entry points → Aging
- [x] **Phase 3 shell** — bottom nav, Tools grid, Nanobanana stub, Settings card
- [x] **Phase 5 Photo Blender**
  - `BgMaskRepository` — ML Kit subject segmentation (character mask)
  - `BlenderRepository` — MediaStore save
  - `PhotoBlenderViewModel` — MVVM state machine
  - UI: Background → Character → mask → drag/pinch → Save → result
  - `ResultSource.PHOTO_BLENDER`
  - `compileDebugKotlin` **SUCCESS**

## Next
- [ ] Phase 2: Aging Figma screens
- [ ] Phase 4: Editors restyle / picker polish
- [ ] Phase 6: AI Nanobanana full catalog
- [ ] Phase 7: Firebase / AdMob / IAP for new app id
- [ ] Phase 8: Polish + ship

## Open in Android Studio
1. **File → Open** → `C:\Users\Terafort\StudioProjects\Ai-Face-Aging`
2. Sync Gradle → Run

## Notes
- `google-services.json` package patched locally — create real Firebase Android app for `com.terafort.aifaceaging` before production.
- ProCapture-Studio was **not** modified.
