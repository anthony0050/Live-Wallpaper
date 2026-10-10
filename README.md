# Redmi Video Wallpaper v0.1.4 — diagnostic build

Target: Redmi Note 14 Pro (24115RA8EG).

This is a **muted diagnostic build**. It does not implement unlock-once audio yet.

## Test
1. Install APK from GitHub Actions artifact.
2. Open app, tap **TEST EMBEDDED MP4**. Observe moving video or error text.
3. Tap **OPEN LIVE WALLPAPER PREVIEW**, wait 10–30 seconds, return to app.
4. Tap **REFRESH WALLPAPER DIAGNOSTICS**, screenshot the displayed event.

The embedded original `app/src/main/res/raw/wallpaper_video.mp4` is unchanged.

## Planned after video works
Video loops only when Home visible, pauses when hidden/screen off. Audio plays once after genuine lock/unlock only when Home first appears; ordinary app-to-Home is silent; DND always silent and exiting DND never retroactively plays sound. These are NOT implemented in this diagnostic build.

## GitHub upload
Upload project contents to repository root, preserving `.github/workflows/build-apk.yml` (hidden `.github` directory). Existing matching files should be overwritten.
