# Redmi Video Wallpaper v0.1.8 (source)

Build on GitHub Actions as before. Based on working v0.1.7 surface playback.

- Video loops muted only while wallpaper is visible (Preview can play muted).
- A genuine screen-off -> ACTION_USER_PRESENT -> first Home visibility triggers one non-looping audio playback from embedded MP4, unless DND is enabled.
- App -> Home without locking stays silent. Audio stops when Home is hidden, screen turns off, or DND is enabled. Turning DND off never replays skipped audio.
- `WallpaperService` uses `MediaPlayer.setSurface` (not `setDisplay`).
- Debug event history available in main app.

Important: Android/HyperOS may vary in broadcast delivery and wallpaper visibility. This source is not yet compiled or tested on the Redmi Note 14 Pro. Check DND, unlock, screen-off and app-return behaviors on the phone. Some OEM builds may restrict unlock broadcasts or media audio playback from wallpaper services.

Upload ZIP contents to repository root, including hidden `.github` folder, then build via Actions.
