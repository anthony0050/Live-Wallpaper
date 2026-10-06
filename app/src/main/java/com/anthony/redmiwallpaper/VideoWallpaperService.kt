package com.anthony.redmiwallpaper

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class VideoWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {
        private var player: MediaPlayer? = null
        private var surface: SurfaceHolder? = null
        private var visible = false
        private var wasLocked = false
        private var unlockAudioTicket = false
        private var audioUsedThisUnlock = false
        private var firstAudibleLoop = false
        private val keyguard by lazy { getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }
        private val nm by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

        override fun onSurfaceCreated(holder: SurfaceHolder) { surface = holder; buildPlayer(holder) }
        override fun onSurfaceDestroyed(holder: SurfaceHolder) { releasePlayer(); surface = null }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            val locked = keyguard.isKeyguardLocked
            if (locked) {
                wasLocked = true
                unlockAudioTicket = false
                audioUsedThisUnlock = false
                firstAudibleLoop = false
                pausePlayer()
                return
            }
            if (wasLocked) {
                wasLocked = false
                unlockAudioTicket = true
            }
            if (!isVisible) { pausePlayer(); return }

            // Wallpaper becomes visible on Home. Only a real lock->unlock grants audio.
            val allowAudio = unlockAudioTicket && !audioUsedThisUnlock && !isDndActive()
            if (unlockAudioTicket) { // consume ticket on first Home, even if DND blocks sound
                unlockAudioTicket = false
                audioUsedThisUnlock = true
            }
            startForHome(allowAudio)
        }

        private fun isDndActive(): Boolean {
            return try {
                if (!nm.isNotificationPolicyAccessGranted) true
                else nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
            } catch (_: Exception) { true }
        }

        private fun buildPlayer(holder: SurfaceHolder) {
            releasePlayer()
            player = MediaPlayer.create(this@VideoWallpaperService, R.raw.wallpaper_video).apply {
                setSurface(holder.surface)
                isLooping = true
                setVolume(0f, 0f)
                setOnCompletionListener {
                    // Defensive: if looping behavior invokes completion, silence all later loops.
                    firstAudibleLoop = false
                    setVolume(0f, 0f)
                }
            }
        }

        private fun startForHome(withAudio: Boolean) {
            val p = player ?: surface?.let { buildPlayer(it); player } ?: return
            try {
                if (withAudio) {
                    p.seekTo(0)
                    p.setVolume(1f, 1f)
                    firstAudibleLoop = true
                    // MediaPlayer looping doesn't reliably expose each loop boundary, so schedule mute at source duration.
                    val duration = p.duration.toLong().coerceAtLeast(18100L)
                    p.start()
                    android.os.Handler(mainLooper).postDelayed({
                        if (firstAudibleLoop) { player?.setVolume(0f,0f); firstAudibleLoop = false }
                    }, duration)
                } else {
                    p.setVolume(0f,0f)
                    if (!p.isPlaying) p.start()
                }
            } catch (_: Exception) { }
        }

        private fun pausePlayer() { try { player?.pause(); player?.setVolume(0f,0f); firstAudibleLoop = false } catch (_: Exception) {} }
        private fun releasePlayer() { try { player?.release() } catch (_: Exception) {}; player = null }
        override fun onDestroy() { releasePlayer(); super.onDestroy() }
    }
}
