# Redmi Video Wallpaper v0.1.1

Test build for Redmi Note 14 Pro (24115RA8EG).

## v0.1.1 fixes
- Fixes startup crash by enabling the Kotlin Android Gradle plugin so MainActivity and VideoWallpaperService are packaged into the APK.
- Adds a custom adaptive Live Wallpaper app icon.
- Keeps the agreed playback behavior: Home-only video playback, one audible pass after a real lock/unlock cycle, ordinary App→Home returns silent, DND always silent, and pause while Home/screen is hidden.
