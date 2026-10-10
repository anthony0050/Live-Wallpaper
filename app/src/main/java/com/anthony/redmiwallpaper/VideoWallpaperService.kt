package com.anthony.redmiwallpaper

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder

/** v0.1.8: surface-safe video; one-shot unlock audio, subject to DND. */
class VideoWallpaperService : WallpaperService() {
    private var sawScreenOff = false
    private var unlockPending = false
    private val engines = mutableSetOf<VideoEngine>()

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    sawScreenOff = true
                    unlockPending = false
                    engines.toList().forEach { it.stopForScreenOff() }
                }
                Intent.ACTION_USER_PRESENT -> {
                    if (sawScreenOff) unlockPending = true
                    sawScreenOff = false
                    engines.toList().forEach { it.refreshPlayback() }
                }
                NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> {
                    if (isDndActive()) unlockPending = false
                    engines.toList().forEach { it.onSoundPolicyChanged() }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(stateReceiver, filter)
    }

    override fun onDestroy() {
        try { unregisterReceiver(stateReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun isDndActive(): Boolean {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Conservative fallback: if policy cannot be inspected, remain silent.
        return try { manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL }
        catch (_: Exception) { true }
    }

    private fun keyguardShowing(): Boolean =
        (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked

    override fun onCreateEngine(): Engine = VideoEngine().also { engines.add(it) }

    inner class VideoEngine : Engine() {
        private var video: MediaPlayer? = null
        private var audio: MediaPlayer? = null
        private var surfaceReady = false
        private var visible = false
        private var prepared = false
        private var destroyed = false
        private var generation = 0
        private var width = 0
        private var height = 0
        private var screenOff = false

        private fun record(message: String) {
            Log.i("RedmiWallpaper", message)
            val prefs = getSharedPreferences("wallpaper_diagnostics", Context.MODE_PRIVATE)
            val previous = prefs.getString("history", "") ?: ""
            prefs.edit()
                .putString("history", (previous + "\n" + System.currentTimeMillis() + " " + message).takeLast(6500))
                .putString("last_time", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date()))
                .apply()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceReady = holder.surface.isValid
            record("surfaceCreated valid=$surfaceReady preview=$isPreview")
            if (surfaceReady && width > 0 && height > 0) prepareVideo(holder)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            super.onSurfaceChanged(holder, format, w, h)
            width = w; height = h
            surfaceReady = holder.surface.isValid
            record("surfaceChanged ${w}x${h} valid=$surfaceReady")
            if (surfaceReady && w > 0 && h > 0 && video == null) prepareVideo(holder)
            refreshPlayback()
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            super.onVisibilityChanged(isVisible)
            visible = isVisible
            if (isVisible && !keyguardShowing()) screenOff = false
            record("visibility=$visible preview=$isPreview prepared=$prepared")
            refreshPlayback()
        }

        private fun prepareVideo(holder: SurfaceHolder) {
            if (!surfaceReady || width <= 0 || height <= 0 || destroyed || video != null) return
            val token = ++generation
            try {
                val mp = MediaPlayer()
                video = mp
                val afd = resources.openRawResourceFd(R.raw.wallpaper_video)
                try { mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length) }
                finally { afd.close() }
                // Wallpaper SurfaceHolder.setKeepScreenOn is unsupported: use Surface directly.
                mp.setSurface(holder.surface)
                mp.setVolume(0f, 0f)
                mp.isLooping = true
                mp.setOnPreparedListener {
                    if (destroyed || token != generation) return@setOnPreparedListener
                    prepared = true
                    record("prepared duration=${it.duration} size=${it.videoWidth}x${it.videoHeight}")
                    refreshPlayback()
                }
                mp.setOnErrorListener { _, what, extra ->
                    record("VIDEO ERROR what=$what extra=$extra")
                    true
                }
                mp.prepareAsync()
            } catch (e: Exception) {
                record("PREPARE EXCEPTION ${e.javaClass.simpleName}: ${e.message}")
                releaseVideo()
            }
        }

        private fun homeVisible(): Boolean =
            !isPreview && visible && surfaceReady && !destroyed && !screenOff && !keyguardShowing()

        fun refreshPlayback() {
            val mp = video ?: return
            if (!prepared) return
            val play = surfaceReady && visible && !destroyed && !screenOff && (isPreview || !keyguardShowing())
            try {
                if (play && !mp.isPlaying) { mp.start(); record("VIDEO START preview=$isPreview") }
                else if (!play && mp.isPlaying) { mp.pause(); record("VIDEO PAUSE") }
            } catch (e: Exception) { record("PLAYBACK EXCEPTION ${e.message}") }
            if (!homeVisible()) stopAudio()
            // Consume pending unlock only on real Home, never on Preview or another app.
            if (homeVisible() && unlockPending) {
                unlockPending = false
                if (!isDndActive()) startOneShotAudio() else record("UNLOCK AUDIO SKIPPED DND")
            }
        }

        fun stopForScreenOff() {
            screenOff = true
            stopAudio()
            refreshPlayback()
        }

        fun onSoundPolicyChanged() {
            if (isDndActive()) stopAudio()
            // DND -> normal must not replay missed audio.
        }

        private fun startOneShotAudio() {
            if (!homeVisible() || isDndActive()) return
            stopAudio()
            try {
                val mp = MediaPlayer()
                audio = mp
                mp.setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                val afd = resources.openRawResourceFd(R.raw.wallpaper_video)
                try { mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length) }
                finally { afd.close() }
                mp.isLooping = false
                mp.setOnPreparedListener {
                    if (audio === mp && homeVisible() && !isDndActive()) {
                        try { mp.start(); record("UNLOCK AUDIO START ONCE") }
                        catch (e: Exception) { record("AUDIO START ERROR ${e.message}"); stopAudio() }
                    } else stopAudio()
                }
                mp.setOnCompletionListener { if (audio === mp) { record("UNLOCK AUDIO COMPLETE"); stopAudio() } }
                mp.setOnErrorListener { _, what, extra ->
                    record("AUDIO ERROR what=$what extra=$extra")
                    stopAudio(); true
                }
                mp.prepareAsync()
            } catch (e: Exception) { record("AUDIO PREPARE ERROR ${e.message}"); stopAudio() }
        }

        private fun stopAudio() {
            val old = audio
            audio = null
            try { old?.release() } catch (_: Exception) {}
        }

        private fun releaseVideo() {
            generation++
            prepared = false
            val old = video
            video = null
            try { old?.release() } catch (_: Exception) {}
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            surfaceReady = false
            stopAudio(); releaseVideo()
            record("surfaceDestroyed")
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            destroyed = true
            stopAudio(); releaseVideo()
            engines.remove(this)
            record("engineDestroyed")
            super.onDestroy()
        }
    }
}
