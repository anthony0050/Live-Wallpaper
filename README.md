# Redmi Video Wallpaper v0.1.2 (test build)

For Redmi Note 14 Pro. Includes the original MP4 and adaptive icon.

Changes: Java/Kotlin JVM 17; use ACTION_USER_PRESENT for unlock tickets, SCREEN_OFF to clear tickets, and HOME visibility to pause video; check DND interruption filter without treating missing policy access as active DND. Mute after one video duration.

**Testing caveats:** HyperOS may reorder USER_PRESENT and wallpaper visibility callbacks; this version is not yet tested on-device. DND status access can vary with OS permissions; if unavailable, audio fails silent. If HOME is visible when USER_PRESENT fires, the wallpaper may restart audio there. Use GitHub Actions to build the APK.
