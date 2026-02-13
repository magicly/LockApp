package com.example.lock30min

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * 主界面 - 爱眼5分钟
 * 护眼提醒应用主界面
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
        private const val ACCESS_PASSWORD = "235711"

        /**
         * 获取图标显示状态
         */
        fun isIconVisible(context: Context): Boolean {
            val componentName = ComponentName(context, MainActivity::class.java)
            val state = context.packageManager.getComponentEnabledSetting(componentName)
            return state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT ||
                   state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }

        /**
         * 设置图标显示状态
         */
        fun setIconVisible(context: Context, visible: Boolean) {
            val componentName = ComponentName(context, MainActivity::class.java)
            val newState = if (visible) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            context.packageManager.setComponentEnabledSetting(
                componentName,
                newState,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 创建主布局
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            setPadding(60, 80, 60, 80)
            // 渐变背景
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    android.graphics.Color.parseColor("#E8F5E9"),
                    android.graphics.Color.parseColor("#C8E6C9")
                )
            )
        }

        // 应用图标占位（眼睛emoji）
        val iconText = TextView(this).apply {
            text = "👁️"
            textSize = 80f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 40, 0, 20)
        }

        // 标题
        val titleText = TextView(this).apply {
            text = "爱眼5分钟"
            textSize = 36f
            setTextColor(android.graphics.Color.parseColor("#2E7D32"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 10)
        }

        // 副标题
        val subtitleText = TextView(this).apply {
            text = "保护视力，从小做起"
            textSize = 16f
            setTextColor(android.graphics.Color.parseColor("#558B2F"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        // 功能卡片区域
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                cornerRadius = 24f
                setColor(android.graphics.Color.WHITE)
                setStroke(2, android.graphics.Color.parseColor("#E0E0E0"))
            }
        }

        // 说明文字
        val descText = TextView(this).apply {
            text = "⏰ 自动提醒时间\n每小时 0-5分 和 30-35分"
            textSize = 16f
            setTextColor(android.graphics.Color.parseColor("#424242"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }

        // 分割线
        val divider = android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply {
                setMargins(0, 0, 0, 30)
            }
            setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
        }

        // 启动按钮（主按钮）
        val startButton = Button(this).apply {
            text = "🚀 启动护眼提醒"
            textSize = 20f
            setTextColor(android.graphics.Color.WHITE)
            setPadding(40, 24, 40, 24)
            background = GradientDrawable().apply {
                cornerRadius = 50f
                setColor(android.graphics.Color.parseColor("#4CAF50"))
            }
            setOnClickListener {
                checkAndStartService()
            }
        }

        // 家长控制按钮（次级按钮）
        val adminButton = Button(this).apply {
            text = "🔐 家长控制"
            textSize = 16f
            setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            setPadding(32, 18, 32, 18)
            background = GradientDrawable().apply {
                cornerRadius = 50f
                setColor(android.graphics.Color.parseColor("#FFFFFF"))
                setStroke(3, android.graphics.Color.parseColor("#4CAF50"))
            }
            setOnClickListener {
                showPasswordDialog()
            }
        }

        // 底部提示
        val hintText = TextView(this).apply {
            text = "💡 提示：点击「启动护眼提醒」后\n应用将在后台运行"
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#757575"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 40, 0, 0)
        }

        // 组装卡片
        cardLayout.addView(descText)
        cardLayout.addView(divider)
        cardLayout.addView(startButton)
        cardLayout.addView(TextView(this).apply { 
            setPadding(0, 16, 0, 0) 
        })
        cardLayout.addView(adminButton)

        // 组装主布局
        layout.addView(iconText)
        layout.addView(titleText)
        layout.addView(subtitleText)
        layout.addView(cardLayout)
        layout.addView(hintText)

        setContentView(layout)
    }

    private fun checkAndStartService() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
        } else {
            startLockService()
        }
    }

    private fun requestOverlayPermission() {
        AlertDialog.Builder(this)
            .setTitle("需要悬浮窗权限")
            .setMessage("爱眼5分钟需要悬浮窗权限来显示休息提醒弹窗。\n\n请在设置中允许显示悬浮窗。")
            .setPositiveButton("去开启") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
            }
            .setNegativeButton("取消") { _, _ ->
                Toast.makeText(this, "需要悬浮窗权限才能运行", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun startLockService() {
        try {
            val serviceIntent = Intent(this, ForegroundService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            
            Toast.makeText(this, "✅ 护眼提醒服务已启动", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "启动失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 显示密码对话框（家长控制）
     */
    private fun showPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("🔐 家长控制")
        builder.setMessage("请输入访问密码")

        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "密码"
            gravity = android.view.Gravity.CENTER
            textSize = 18f
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 20)
            addView(input)
        }
        builder.setView(container)

        builder.setPositiveButton("确定") { _, _ ->
            if (input.text.toString() == ACCESS_PASSWORD) {
                showAdminPanel()
            } else {
                Toast.makeText(this, "❌ 密码错误", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("取消", null)
        builder.show()
    }

    /**
     * 显示家长控制面板（带二维码）
     */
    private fun showAdminPanel() {
        val mode = LockControlManager.getMode()
        val duration = LockControlManager.getForceLockDuration()
        val customMessage = LockControlManager.getCustomLockMessage()
        val modeText = when (mode) {
            LockControlManager.ControlMode.AUTO -> "🔄 自动模式"
            LockControlManager.ControlMode.FORCE_LOCK -> "🔒 强制锁屏 ${duration}分钟"
            LockControlManager.ControlMode.FORCE_UNLOCK -> "🔓 强制解锁"
        }

        val ipAddress = try {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as android.net.wifi.WifiManager
            android.text.format.Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
        } catch (e: Exception) {
            "获取失败"
        }

        // 生成二维码
        val adminUrl = "http://$ipAddress:34567/admin?key=d33a560e81699606e5c9d32341cae435"
        val qrCodeBitmap = QRCodeGenerator.generateQRCode(adminUrl, 400, 400)

        // 创建布局
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(40, 40, 40, 40)
        }

        // 二维码图片
        val qrImageView = android.widget.ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(400, 400)
            if (qrCodeBitmap != null) {
                setImageBitmap(qrCodeBitmap)
            }
            setPadding(0, 0, 0, 20)
        }

        // 提示文字
        val hintText = TextView(this).apply {
            text = "📱 使用另一台手机扫码访问管理页面"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#666666"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }

        // 状态信息
        val messageText = if (!customMessage.isNullOrBlank()) "\n📝 自定义消息：$customMessage\n" else ""
        val statusText = TextView(this).apply {
            text = "当前模式：$modeText$messageText\n\n" +
                   "🔗 控制地址：\n$adminUrl\n\n" +
                   "🗑️ 如需卸载：设置 → 应用管理 → 爱眼5分钟 → 卸载"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#333333"))
            setPadding(0, 20, 0, 0)
        }

        layout.addView(qrImageView)
        layout.addView(hintText)
        layout.addView(statusText)

        AlertDialog.Builder(this)
            .setTitle("👨‍👩‍👧‍👦 家长控制面板")
            .setView(layout)
            .setPositiveButton("确定", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                startLockService()
            } else {
                Toast.makeText(this, "需要悬浮窗权限才能运行", Toast.LENGTH_LONG).show()
            }
        }
    }
}
