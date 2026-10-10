# Redmi Video Wallpaper v0.1.7

Fixes wallpaper preview PREPARE EXCEPTION (Wallpapers do not support keep screen on) by using MediaPlayer.setSurface(holder.surface) instead of setDisplay(holder). Initialization waits for a valid, non-zero-size wallpaper surface.

Diagnostic test build: embedded MP4 plays muted and loops only while wallpaper engine is visible. Unlock-only audio and DND handling are not yet implemented.

Upload the **contents** of this ZIP to the root of your GitHub repository, preserving `.github/workflows/build-apk.yml`. Run the GitHub Actions APK build, install it, open Live Wallpaper Preview for 15 seconds, then return to the app and tap Refresh Wallpaper Diagnostics. Confirm video appears and provide diagnostic screenshot if it does not.

Not yet compiled or device-tested.
