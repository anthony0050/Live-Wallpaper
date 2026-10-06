# Home Video Wallpaper v0.1
Target test device: Redmi Note 14 Pro (24115RA8EG).

## Behavior
- Video loops while Home live wallpaper is visible.
- A genuine lock -> unlock grants one audio opportunity.
- If Home is first shown after that unlock and DND is off, video restarts and audio plays once.
- Ordinary App -> Home returns are silent.
- DND consumes the unlock opportunity silently; turning DND off does not retroactively play audio.
- Wallpaper pauses when hidden/locked.

## Build on GitHub
Upload the whole project to a GitHub repository. Actions -> Build Android APK -> Run workflow (or push to main). Download the artifact after the build completes.

## First install
Open app -> ALLOW DND STATUS ACCESS -> enable access for Home Video Wallpaper -> return -> SET LIVE WALLPAPER -> apply to Home screen.
