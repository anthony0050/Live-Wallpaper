# Redmi Video Wallpaper v0.1.5 — Surface diagnostic

Diagnostic only: no audio. Embedded MP4 unchanged.

1. Open app and test embedded MP4 (should play silently).
2. Open live wallpaper preview and watch first 4 seconds. A BLUE screen with YELLOW "WALLPAPER SURFACE OK" text should appear before video preparation begins.
3. Wait another 15 seconds. Return to app and refresh event history; screenshot it and the Preview result.

The test distinguishes whether wallpaper canvas can render at all from MediaPlayer video rendering. It does not claim to fix black video. Do not use this build as a final wallpaper.

Upload extracted files to the repository root, preserving `.github/workflows/build-apk.yml`. Commit to trigger GitHub Actions.
