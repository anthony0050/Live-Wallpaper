package com.anthony.redmiwallpaper

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder

/** v0.1.6: direct video output diagnostic. Audio is intentionally muted. */
class VideoWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var player: MediaPlayer? = null
        private var surfaceReady = false
        private var visible = false
        private var prepared = false
        private var destroyed = false
        private var generation = 0
        private var surfaceWidth = 0
        private var surfaceHeight = 0

        private fun record(message: String) {
            Log.i("RedmiWallpaper", message)
            val prefs = getSharedPreferences("wallpaper_diagnostics", Context.MODE_PRIVATE)
            val previous = prefs.getString("history", "") ?: ""
            prefs.edit()
                .putString("history", (previous + "\n" + System.currentTimeMillis() + " " + message).takeLast(6500))
                .putString("last_time", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date()))
                .commit()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceReady = holder.surface.isValid
            record("surfaceCreated valid=$surfaceReady preview=$isPreview")
            // Do not lockCanvas here: decoder must own the wallpaper surface.
            if (surfaceReady) prepareVideo(holder)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            surfaceReady = holder.surface.isValid
            record("surfaceChanged ${width}x${height} valid=$surfaceReady player=${player != null}")
            if (surfaceReady && player == null) prepareVideo(holder)
            updatePlayback()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            this.visible = visible
            record("visibility=$visible preview=$isPreview prepared=$prepared")
            updatePlayback()
        }

        private fun prepareVideo(holder: SurfaceHolder) {
            if (!surfaceReady || destroyed || player != null) return
            val token = ++generation
            record("prepare begin token=$token surface=${holder.surface.isValid} size=${surfaceWidth}x$surfaceHeight")
            try {
                val mp = MediaPlayer()
                player = mp
                val afd = resources.openRawResourceFd(R.raw.wallpaper_video)
                    ?: error("Video resource descriptor unavailable")
                try {
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                } finally {
                    afd.close()
                }
                mp.setDisplay(holder)
                mp.setVolume(0f, 0f)
                mp.isLooping = true
                mp.setOnPreparedListener {
                    if (token != generation || destroyed) return@setOnPreparedListener
                    prepared = true
                    record("prepared duration=${it.duration} dimensions=${it.videoWidth}x${it.videoHeight}")
                    updatePlayback()
                }
                mp.setOnVideoSizeChangedListener { _, w, h -> record("videoSizeChanged ${w}x$h") }
                mp.setOnErrorListener { _, what, extra ->
                    record("PLAYER ERROR what=$what extra=$extra")
                    true
                }
                mp.setOnInfoListener { _, what, extra ->
                    record("playerInfo what=$what extra=$extra")
                    false
                }
                mp.prepareAsync()
                record("prepareAsync submitted")
            } catch (e: Exception) {
                record("PREPARE EXCEPTION ${e.javaClass.simpleName}: ${e.message}")
                releasePlayer()
            }
        }

        private fun updatePlayback() {
            val mp = player ?: return
            if (!prepared) return
            val shouldPlay = surfaceReady && visible && !destroyed
            try {
                if (shouldPlay && !mp.isPlaying) {
                    mp.start()
                    record("START preview=$isPreview")
                } else if (!shouldPlay && mp.isPlaying) {
                    mp.pause()
                    record("PAUSE")
                }
            } catch (e: Exception) {
                record("PLAYBACK EXCEPTION ${e.javaClass.simpleName}: ${e.message}")
            }
        }

        private fun releasePlayer() {
            generation++
            prepared = false
            val old = player
            player = null
            try { old?.release() } catch (_: Exception) {}
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            surfaceReady = false
            releasePlayer()
            record("surfaceDestroyed")
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            destroyed = true
            handler.removeCallbacksAndMessages(null)
            releasePlayer()
            record("engineDestroyed")
            super.onDestroy()
        }
    }
}
