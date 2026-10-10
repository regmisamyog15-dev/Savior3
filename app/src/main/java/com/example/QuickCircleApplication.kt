package com.example

import android.app.Application
import com.example.data.QuickCirclePreferences
import java.io.File

class QuickCircleApplication : Application() {
    lateinit var preferences: QuickCirclePreferences
        private set

    override fun onCreate() {
        super.onCreate()
        preferences = QuickCirclePreferences.getInstance(this)
        ensureWebViewCacheDirectories()
    }

    private fun ensureWebViewCacheDirectories() {
        try {
            val cache = cacheDir
            val paths = listOf(
                "WebView",
                "WebView/Default",
                "WebView/Default/HTTP Cache",
                "WebView/Default/HTTP Cache/Code Cache",
                "WebView/Default/HTTP Cache/Code Cache/js",
                "WebView/Default/HTTP Cache/Code Cache/wasm"
            )
            for (p in paths) {
                val dir = File(cache, p)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            }
        } catch (_: Exception) {}
    }
}
