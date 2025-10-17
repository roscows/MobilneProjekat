package com.example.ironlink

import android.app.Application
import com.cloudinary.android.MediaManager
import java.util.HashMap

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = HashMap<String, String>().apply {
            put("cloud_name", "your_actual_cloud_name")  // Iz local.properties
        }
        MediaManager.init(this, config)
    }
}