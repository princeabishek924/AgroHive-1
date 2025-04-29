package com.example.agrohive_1

import android.app.Application
import android.util.Log

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("UncaughtException", "Uncaught exception in thread ${thread.name}", throwable)
            // Optionally, you can show a toast or dialog here
            // For now, just log the crash and let the app continue
        }
    }
}