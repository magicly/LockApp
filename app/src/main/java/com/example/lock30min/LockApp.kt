package com.example.lock30min

import android.app.Application
import android.util.Log

/**
 * 应用全局类
 * 用于提供全局上下文
 */
class LockApp : Application() {

    companion object {
        private const val TAG = "LockApp"
        var instance: LockApp? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "LockApp initialized")
    }
}
