package com.anthony.redmiwallpaper

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class VideoWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {
        private var player: MediaPlayer? = null
        private var holderRef: SurfaceHolder? = null
        private var homeVisible = false
        private var screenOn = true
        private var pendingUnlockAudio = false
        private var audioPlaying = false
        private var receiverRegistered = false
        private val handler = Handler(Looper.getMainLooper())
        private val keyguard by lazy { getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }
        private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
        private val muteAfterFirstLoop = Runnable { stopAudio() }
        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        screenOn = false
                        pendingUnlockAudio = false
                        pausePlayback()
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        screenOn = true
                        pendingUnlockAudio = true
                        // Visibility callbacks can arrive before USER_PRESENT on some launchers.
                        handler.postDelayed({ updatePlayback() }, 250)
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        screenOn = true
                        updatePlayback()
                    }
                    NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> {
                        if (isDndActive()) {
                            pendingUnlockAudio = false
                            stopAudio()
                        }
                    }
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            }
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            else @Suppress("DEPRECATION") registerReceiver(receiver, filter)
            receiverRegistered = true
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            holderRef = holder
            createPlayer(holder)
            updatePlayback()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            pausePlayback()
            releasePlayer()
            holderRef = null
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            homeVisible = visible
            if (!visible) {
                pausePlayback()
            } else {
                updatePlayback()
            }
        }

        private fun isDndActive(): Boolean = try {
            // A lack of policy access must not be interpreted as DND being enabled.
            notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        } catch (_: SecurityException) {
            // Fail closed for audio when the OS prevents checking DND.
            true
        } catch (_: Exception) { true }

        private fun createPlayer(holder: SurfaceHolder) {
            releasePlayer()
            try {
                player = MediaPlayer.create(this@VideoWallpaperService, R.raw.wallpaper_video)?.apply {
                    setSurface(holder.surface)
                    isLooping = true
                    setVolume(0f, 0f)
                }
            } catch (_: Exception) { player = null }
        }

        private fun updatePlayback() {
            if (!homeVisible || !screenOn || keyguard.isKeyguardLocked) {
                pausePlayback()
                return
            }
            if (player == null) holderRef?.let { createPlayer(it) }
            val p = player ?: return
            try {
                if (pendingUnlockAudio) {
                    pendingUnlockAudio = false
                    if (!isDndActive()) {
                        handler.removeCallbacks(muteAfterFirstLoop)
                        p.seekTo(0)
                        p.setVolume(1f, 1f)
                        audioPlaying = true
                        handler.postDelayed(muteAfterFirstLoop, p.duration.toLong().coerceAtLeast(1000L))
                    } else stopAudio()
                } else if (!audioPlaying) p.setVolume(0f, 0f)
                if (!p.isPlaying) p.start()
            } catch (_: Exception) {
                stopAudio()
            }
        }

        private fun stopAudio() {
            handler.removeCallbacks(muteAfterFirstLoop)
            audioPlaying = false
            try { player?.setVolume(0f, 0f) } catch (_: Exception) {}
        }

        private fun pausePlayback() {
            stopAudio()
            try { player?.pause() } catch (_: Exception) {}
        }

        private fun releasePlayer() {
            stopAudio()
            try { player?.release() } catch (_: Exception) {}
            player = null
        }

        override fun onDestroy() {
            if (receiverRegistered) {
                try { unregisterReceiver(receiver) } catch (_: Exception) {}
                receiverRegistered = false
            }
            releasePlayer()
            super.onDestroy()
        }
    }
}
