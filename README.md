# Redmi Video Wallpaper v0.1.3 — Video Diagnostic

For Redmi Note 14 Pro (24115RA8EG).

## What changed
- Fixes the likely black-screen cause: the video Surface is attached **before** asynchronous MediaPlayer preparation.
- Handles Surface creation, resize, preview visibility and Home visibility separately.
- Adds Android logcat diagnostics tagged `RedmiWallpaper`.
- Video loops silently in both wallpaper Preview and Home.
- **Audio / unlock / DND logic is intentionally disabled in this diagnostic build.** It will be restored after successful video playback testing.

## Test
1. Upload the extracted project files to the repository root, retaining `.github/workflows`.
2. Build the debug APK with GitHub Actions.
3. Open the app, enter Preview and confirm moving video.
4. Set Home wallpaper and verify silent looping; open another app and return.
5. If black screen persists, obtain logcat filtered by `RedmiWallpaper` (ADB) for diagnosis.

This is a test build, not a final release. No successful device test is claimed.
