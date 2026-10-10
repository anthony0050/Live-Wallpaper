package com.anthony.redmiwallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.VideoView

class MainActivity : Activity() {
    private var video: VideoView? = null
    private lateinit var status: TextView
    private lateinit var wallpaperStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
        }
        val scroller = ScrollView(this).apply { addView(root) }
        root.addView(TextView(this).apply {
            text = "Home Video Wallpaper v0.1.5 — surface diagnostics\nAudio intentionally disabled in this test build."
            textSize = 19f
        })
        status = TextView(this).apply { text = "Embedded MP4: not tested"; textSize = 16f }
        root.addView(status)
        video = VideoView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 550)
            setOnPreparedListener { player ->
                player.isLooping = true
                player.setVolume(0f, 0f)
                status.text = "Embedded MP4: PREPARED, ${player.duration} ms, ${player.videoWidth} x ${player.videoHeight}. Playing muted."
                start()
            }
            setOnErrorListener { _, what, extra ->
                status.text = "Embedded MP4: ERROR what=$what extra=$extra"
                true
            }
            setOnCompletionListener { status.text = "Embedded MP4: completed" }
        }.also { root.addView(it) }
        root.addView(Button(this).apply {
            text = "1. TEST EMBEDDED MP4"
            setOnClickListener {
                status.text = "Embedded MP4: loading..."
                video?.setVideoURI(Uri.parse("android.resource://$packageName/${R.raw.wallpaper_video}"))
                video?.start()
            }
        })
        root.addView(Button(this).apply {
            text = "2. OPEN LIVE WALLPAPER PREVIEW"
            setOnClickListener {
                startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, VideoWallpaperService::class.java)))
            }
        })
        wallpaperStatus = TextView(this).apply { textSize = 15f; gravity = Gravity.START }
        root.addView(wallpaperStatus)
        root.addView(Button(this).apply {
            text = "3. REFRESH WALLPAPER DIAGNOSTICS"
            setOnClickListener { refreshStatus() }
        })
        root.addView(TextView(this).apply {
            text = "Instructions: Test embedded MP4 here first. Then open Preview for 15 seconds. Look for a BLUE screen with YELLOW text during the first 4 seconds., Return here and tap Refresh. Screenshot the event history and Preview."
        })
        setContentView(scroller)
        refreshStatus()
    }

    private fun refreshStatus() {
        val prefs = getSharedPreferences("wallpaper_diagnostics", MODE_PRIVATE)
        wallpaperStatus.text = "Wallpaper event history:\n" +
            prefs.getString("history", "No wallpaper engine event recorded yet") +
            "\nEvent time (device): " + prefs.getString("last_time", "unknown")
    }

    override fun onPause() {
        video?.pause()
        super.onPause()
    }
    override fun onDestroy() {
        video?.stopPlayback()
        super.onDestroy()
    }
}
