package com.example.lock30min

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.app.NotificationCompat
import java.util.*

class ForegroundService : Service() {

    companion object {
        private const val TAG = "ForegroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "lock_channel"
        
        // 控制状态
        @Volatile
        var isEnabled = true
        
        // 检查间隔（秒）
        private const val CHECK_INTERVAL = 10000L // 10秒检查一次
    }

    private val handler = Handler(Looper.getMainLooper())
    private val checkRunnable = object : Runnable {
        override fun run() {
            checkAndShowLockScreen()
            handler.postDelayed(this, CHECK_INTERVAL)
        }
    }
    
    private var overlayView: View? = null
    private var countdownTextView: TextView? = null
    private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private var httpServer: HttpControlServer? = null

    override fun onCreate() {
        super.onCreate()
        startHttpServer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        
        // 开始定时检查
        handler.post(checkRunnable)
        
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "护眼提醒服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "定时提醒休息眼睛"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, SettingsActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("护眼提醒运行中")
            .setContentText("每小时0-5分和30-35分会提醒休息")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun checkAndShowLockScreen() {
        // 如果被禁用，不显示弹窗
        if (!isEnabled) {
            hideLockScreen()
            return
        }
        
        // 检查当前时间
        val calendar = Calendar.getInstance()
        val minute = calendar.get(Calendar.MINUTE)
        
        // 检查是否在锁屏时间段：
        // 0-5分钟 或 30-35分钟
        val shouldLock = (minute in 0..5) || (minute in 30..35)
        
        if (shouldLock && overlayView == null) {
            showLockScreen()
        } else if (!shouldLock && overlayView != null) {
            hideLockScreen()
        }
    }

    private fun showLockScreen() {
        try {
            if (overlayView != null) return
            
            // 创建美观的弹窗布局
            val root = createLockScreenLayout()
            
            // 悬浮窗参数
            val params = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                
                type = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    }
                    else -> {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                    }
                }
                
                // 关键标志：全屏、保持屏幕、获取焦点
                flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DIM_BEHIND or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                
                dimAmount = 0.9f
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.CENTER
            }
            
            overlayView = root
            windowManager.addView(root, params)
            
            // 开始倒计时
            startCountdown()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createLockScreenLayout(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#FF6B6B")) // 温暖的红色背景
            setPadding(60, 60, 60, 60)
        }
        
        // 卡片容器
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 60, 50, 60)
            setBackgroundColor(Color.WHITE)
        }
        
        // 圆角背景
        val cardDrawable = GradientDrawable().apply {
            cornerRadius = 40f
            setColor(Color.WHITE)
        }
        cardLayout.background = cardDrawable
        
        // 图标（眼睛）
        val iconText = TextView(this).apply {
            text = "👀"
            textSize = 64f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }
        
        // 主标题
        val titleText = TextView(this).apply {
            text = "休息一下吧~"
            textSize = 32f
            setTextColor(Color.parseColor("#333333"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        
        // 提示信息
        val messageText = TextView(this).apply {
            text = "你已经玩太久了\n去休息放松一下眼睛吧~"
            textSize = 18f
            setTextColor(Color.parseColor("#666666"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }
        
        // 倒计时显示
        countdownTextView = TextView(this).apply {
            text = "05:00"
            textSize = 56f
            setTextColor(Color.parseColor("#FF6B6B"))
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 30)
        }
        
        // 倒计时说明
        val countdownLabel = TextView(this).apply {
            text = "倒计时结束后可继续使用"
            textSize = 14f
            setTextColor(Color.parseColor("#999999"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }
        
        // 装饰线
        val line = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply {
                setMargins(0, 20, 0, 20)
            }
            setBackgroundColor(Color.parseColor("#EEEEEE"))
        }
        
        // 底部小提示
        val tipText = TextView(this).apply {
            text = "💡 保持良好的用眼习惯"
            textSize = 14f
            setTextColor(Color.parseColor("#888888"))
            gravity = Gravity.CENTER
        }
        
        cardLayout.addView(iconText)
        cardLayout.addView(titleText)
        cardLayout.addView(messageText)
        cardLayout.addView(line)
        cardLayout.addView(countdownTextView)
        cardLayout.addView(countdownLabel)
        cardLayout.addView(tipText)
        
        // 设置卡片布局参数
        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(40, 40, 40, 40)
        }
        
        root.addView(cardLayout, cardParams)
        
        return root
    }

    private fun startCountdown() {
        val calendar = Calendar.getInstance()
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        
        // 计算剩余时间（秒）
        // 0-5分钟：结束时间是5分0秒
        // 30-35分钟：结束时间是35分0秒
        val endMinute = if (minute < 30) 5 else 35
        val remainingSeconds = (endMinute - minute) * 60 - second
        
        val countdownRunnable = object : Runnable {
            var remaining = remainingSeconds
            
            override fun run() {
                if (overlayView == null) return
                
                if (remaining > 0) {
                    val mins = remaining / 60
                    val secs = remaining % 60
                    countdownTextView?.text = String.format("%02d:%02d", mins, secs)
                    remaining--
                    handler.postDelayed(this, 1000)
                } else {
                    // 倒计时结束，隐藏弹窗
                    hideLockScreen()
                }
            }
        }
        
        handler.post(countdownRunnable)
    }

    private fun hideLockScreen() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
            countdownTextView = null
        }
    }

    private fun startHttpServer() {
        try {
            httpServer = HttpControlServer(this, 34567)
            httpServer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        hideLockScreen()
        
        try {
            httpServer?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // 重启服务
        val intent = Intent(this, ForegroundService::class.java)
        startService(intent)
    }
}
