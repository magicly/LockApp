package com.example.lock30min

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.app.NotificationCompat

class ForegroundService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "lock_channel"
    }

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var overlayView: View? = null
    private val windowManager by lazy {
        getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        // 30 分钟后弹出锁屏
        handler.postDelayed({
            showLockScreen()
        }, 3 * 1 * 1000L)

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "计时服务",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("计时中")
            .setContentText("孩子已开启 30 分钟倒计时")
            .setSmallIcon(R.drawable.ic_launcher_foreground)   // 用默认图标即可
            .build()

    private fun showLockScreen() {
        val root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)

        val edit = EditText(this).apply {
            hint = "请输入家长密码"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            textSize = 18f
            gravity = Gravity.CENTER
            inputType = EditorInfo.TYPE_CLASS_NUMBER
        }

        val btn = Button(this).apply {
            text = "解锁"
            setOnClickListener {
                if (edit.text.toString() == "1234") {   // 家长密码
                    hideLockScreen()
                    stopSelf()
                } else {
                    Toast.makeText(this@ForegroundService, "密码错误", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(edit)
            addView(btn)
        }

        root.addView(container)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv(),
            PixelFormat.TRANSLUCENT
        )

        overlayView = root
        windowManager.addView(root, params)
    }

    private fun hideLockScreen() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}