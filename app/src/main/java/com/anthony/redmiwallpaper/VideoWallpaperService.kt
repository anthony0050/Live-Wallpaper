package com.anthony.redmiwallpaper

import android.app.KeyguardManager
import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder

/**
 * v0.1.4: video-only diagnostic build.
 * Audio is intentionally disabled until video rendering is verified on-device.
 */
class VideoWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {
        private val tag = "RedmiWallpaper"
        private val main = Handler(Looper.getMainLooper())
        private fun record(message: String) {
            Log.i(tag, message)
            getSharedPreferences("wallpaper_diagnostics", Context.MODE_PRIVATE).edit()
                .putString("last_event", message)
                .putString("last_time", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date()))
                .apply()
        }
        private val keyguard by lazy { getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }
        private var player: MediaPlayer? = null
        private var surfaceReady = false
        private var visibleNow = false
        private var prepared = false
        private var destroyed = false
        private var generation = 0

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            record("surfaceCreated valid=${holder.surface.isValid}")
            surfaceReady = true
            startPreparing(holder)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            record("surfaceChanged ${width}x${height}")
            if (surfaceReady && player == null) startPreparing(holder)
            updatePlayback()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            visibleNow = visible
            record("visibility=$visible preview=$isPreview")
            updatePlayback()
        }

        private fun startPreparing(holder: SurfaceHolder) {
            if (!holder.surface.isValid || destroyed) {
                record("Surface not valid yet")
                return
            }
            releasePlayer()
            val token = ++generation
            try {
                // Configure the display BEFORE preparing. MediaPlayer.create()
                // prepares immediately and can fail to render on some wallpaper surfaces.
                val p = MediaPlayer()
                player = p
                val afd = resources.openRawResourceFd(R.raw.wallpaper_video)
                    ?: error("MP4 raw resource descriptor unavailable")
                try {
                    p.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                } finally {
                    afd.close()
                }
                p.setDisplay(holder)
                p.setVolume(0f, 0f)
                p.isLooping = true
                p.setOnPreparedListener {
                    if (token != generation || destroyed) return@setOnPreparedListener
                    prepared = true
                    record("prepared duration=${it.duration} video=${it.videoWidth}x${it.videoHeight}")
                    updatePlayback()
                }
                p.setOnErrorListener { _, what, extra ->
                    record("MediaPlayer ERROR what=$what extra=$extra")
                    true
                }
                p.setOnInfoListener { _, what, extra ->
                    record("MediaPlayer info what=$what extra=$extra")
                    false
                }
                p.prepareAsync()
            } catch (e: Exception) {
                record("prepare failed: ${e.javaClass.simpleName}: ${e.message}")
                Log.e(tag, "prepare failed", e)
                releasePlayer()
            }
        }

        private fun updatePlayback() {
            val p = player ?: return
            if (!prepared) return
            // Preview is rendered in a separate wallpaper picker activity.
            val canPlay = visibleNow && surfaceReady &&
                (isPreview || !keyguard.isKeyguardLocked)
            try {
                if (canPlay) {
                    if (!p.isPlaying) {
                        record("play")
                        p.start()
                    }
                } else if (p.isPlaying) {
                    record("pause")
                    p.pause()
                }
            } catch (e: Exception) {
                record("playback transition failed: ${e.message}")
                Log.e(tag, "playback transition failed", e)
            }
        }

        private fun releasePlayer() {
            ++generation
            prepared = false
            val p = player
            player = null
            try { p?.reset() } catch (_: Exception) {}
            try { p?.release() } catch (_: Exception) {}
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            surfaceReady = false
            releasePlayer()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            destroyed = true
            main.removeCallbacksAndMessages(null)
            releasePlayer()
            super.onDestroy()
        }
    }
}
