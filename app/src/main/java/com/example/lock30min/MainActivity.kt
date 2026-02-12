package com.example.lock30min

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

/**
 * 主界面 - 护眼提醒
 * 默认显示图标，可以手动隐藏
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
        private const val ACCESS_PASSWORD = "8888"

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

    private var isAdminMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(60, 60, 60, 60)
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        // 标题
        val titleText = TextView(this).apply {
            text = "护眼提醒"
            textSize = 32f
            setTextColor(android.graphics.Color.parseColor("#333333"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 10)
        }

        // 说明文字
        val descText = TextView(this).apply {
            text = "每小时 0-5分 和 30-35分\n自动提醒休息眼睛"
            textSize = 16f
            setTextColor(android.graphics.Color.parseColor("#666666"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        // 启动按钮
        val startButton = Button(this).apply {
            text = "启动护眼"
            textSize = 18f
            setPadding(30, 20, 30, 20)
            setOnClickListener {
                checkAndStartService()
            }
        }

        // 隐藏图标按钮
        val hideIconButton = Button(this).apply {
            text = "隐藏桌面图标"
            textSize = 14f
            setPadding(20, 15, 20, 15)
            setOnClickListener {
                hideLauncherIcon()
            }
        }

        // 家长控制按钮
        val adminButton = Button(this).apply {
            text = "家长控制"
            textSize = 14f
            setPadding(20, 15, 20, 15)
            setOnClickListener {
                showPasswordDialog()
            }
        }

        // 状态信息（默认隐藏，家长控制模式显示）
        val statusText = TextView(this).apply {
            text = ""
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#888888"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 30, 0, 0)
            visibility = android.view.View.GONE
        }

        layout.addView(titleText)
        layout.addView(descText)
        layout.addView(startButton)
        layout.addView(TextView(this).apply { setPadding(0, 20, 0, 0) })
        layout.addView(hideIconButton)
        layout.addView(TextView(this).apply { setPadding(0, 10, 0, 0) })
        layout.addView(adminButton)
        layout.addView(statusText)

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
            .setMessage("护眼提醒需要悬浮窗权限来显示休息提醒弹窗。\n\n请在设置中允许显示悬浮窗。")
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
            
            Toast.makeText(this, "护眼提醒服务已启动", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "启动失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 隐藏桌面图标
     */
    private fun hideLauncherIcon() {
        AlertDialog.Builder(this)
            .setTitle("隐藏图标")
            .setMessage("隐藏后，您可以通过以下方式打开：\n\n" +
                    "设置 → 应用管理 → 搜索 LockApp\n\n" +
                    "注意：隐藏后可能无法从桌面打开，" +
                    "建议先测试 HTTP 控制功能是否正常。\n\n" +
                    "确定要隐藏吗？")
            .setPositiveButton("隐藏") { _, _ ->
                // 隐藏图标
                val componentName = ComponentName(this, MainActivity::class.java)
                packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                Toast.makeText(this, "图标已隐藏", Toast.LENGTH_LONG).show()
                finish()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示密码对话框（家长控制）
     */
    private fun showPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("家长控制")
        builder.setMessage("请输入访问密码（默认：8888）")

        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "密码"
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
            addView(input)
        }
        builder.setView(container)

        builder.setPositiveButton("确定") { _, _ ->
            if (input.text.toString() == ACCESS_PASSWORD) {
                showAdminPanel()
            } else {
                Toast.makeText(this, "密码错误", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("取消", null)
        builder.show()
    }

    /**
     * 显示家长控制面板
     */
    private fun showAdminPanel() {
        val mode = LockControlManager.getMode()
        val duration = LockControlManager.getForceLockDuration()
        val modeText = when (mode) {
            LockControlManager.ControlMode.AUTO -> "自动模式"
            LockControlManager.ControlMode.FORCE_LOCK -> "强制锁屏 ${duration}分钟"
            LockControlManager.ControlMode.FORCE_UNLOCK -> "强制解锁"
        }

        val ipAddress = try {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as android.net.wifi.WifiManager
            android.text.format.Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
        } catch (e: Exception) {
            "获取失败"
        }

        val message = "当前模式：$modeText\n\n" +
                "控制地址：\nhttp://$ipAddress:34567/admin?key=...\n\n" +
                "如需卸载：设置 → 应用管理 → LockApp → 卸载"

        AlertDialog.Builder(this)
            .setTitle("家长控制面板")
            .setMessage(message)
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
