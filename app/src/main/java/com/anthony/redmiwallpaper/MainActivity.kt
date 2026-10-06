package com.anthony.redmiwallpaper

import android.app.Activity
import android.app.NotificationManager
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(48,48,48,48) }
        box.addView(TextView(this).apply { text = "Home Video Wallpaper v0.1\n\nRedmi Note 14 Pro test build\n\n• Home: video loops\n• Unlock → first Home: audio once\n• App → Home: silent\n• DND: always silent\n• Screen off/Home hidden: pause"; textSize = 18f })
        box.addView(Button(this).apply { text = "SET LIVE WALLPAPER"; setOnClickListener {
            startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this@MainActivity, VideoWallpaperService::class.java)))
        }})
        box.addView(Button(this).apply { text = "ALLOW DND STATUS ACCESS"; setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) } })
        setContentView(box)
    }
}
