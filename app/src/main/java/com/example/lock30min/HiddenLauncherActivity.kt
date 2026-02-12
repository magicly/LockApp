package com.example.lock30min

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * 隐藏启动入口 - 通过拨号盘 *#*#123456#*#* 启动
 */
class HiddenLauncherActivity : AppCompatActivity() {

    companion object {
        private const val SECRET_CODE = "123456"
        private const val ACCESS_PASSWORD = "8888"

        fun getLaunchMethods(context: Context): List<Pair<String, String>> {
            val pkg = context.packageName
            return listOf(
                Pair("1. 拨号盘", "打开拨号盘，输入 *#*#123456#*#*"),
                Pair("2. ADB命令", "adb shell am start -n $pkg/.HiddenLauncherActivity"),
                Pair("3. 设置搜索", "设置 → 应用管理 → 搜索 LockApp → 打开")
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 检查是否是拨号盘启动
        if (intent.action == "android.provider.Telephony.SECRET_CODE") {
            val code = intent.data?.host
            if (code == SECRET_CODE) {
                showPasswordDialog()
                return
            } else {
                finish()
                return
            }
        }
        
        // 直接启动也显示密码验证
        showPasswordDialog()
    }

    private fun showPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("家长入口")
        builder.setMessage("请输入访问密码（默认：8888）")

        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "密码"
        }
        
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
            addView(input)
        }
        builder.setView(container)

        builder.setPositiveButton("确定") { _, _ ->
            if (input.text.toString() == ACCESS_PASSWORD) {
                startActivity(Intent(this, AdminActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "密码错误", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        builder.setNegativeButton("取消") { _, _ ->
            finish()
        }

        builder.setCancelable(false)
        builder.show()
    }
}

/**
 * 管理界面 - 需要密码才能进入
 */
class AdminActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_ADMIN = 1001
        private const val CANCEL_ADMIN_PASSWORD = "9999"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.decorView.setBackgroundColor(android.graphics.Color.WHITE)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        // 标题
        val tvTitle = android.widget.TextView(this).apply {
            text = "家长管理控制台"
            textSize = 24f
            setTextColor(android.graphics.Color.BLACK)
            setPadding(0, 0, 0, 30)
        }

        // 状态显示
        val tvStatus = android.widget.TextView(this).apply {
            text = getStatusText()
            textSize = 16f
            setTextColor(android.graphics.Color.DKGRAY)
            setPadding(0, 0, 0, 30)
        }

        // 激活设备管理员按钮
        val btnEnableAdmin = android.widget.Button(this).apply {
            text = "激活设备管理员（防卸载）"
            setOnClickListener { enableDeviceAdmin() }
        }

        // 取消设备管理员按钮
        val btnDisableAdmin = android.widget.Button(this).apply {
            text = "取消设备管理员（需密码）"
            setOnClickListener { showDisablePasswordDialog() }
        }

        // 打开主应用按钮
        val btnOpenMain = android.widget.Button(this).apply {
            text = "启动护眼提醒服务"
            setOnClickListener {
                startActivity(Intent(this@AdminActivity, MainActivity::class.java))
            }
        }

        // 打开设置页面
        val btnOpenSettings = android.widget.Button(this).apply {
            text = "打开权限设置"
            setOnClickListener {
                startActivity(Intent(this@AdminActivity, SettingsActivity::class.java))
            }
        }

        // 应用信息
        val btnAppInfo = android.widget.Button(this).apply {
            text = "打开应用信息（用于卸载）"
            setOnClickListener {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }

        // 显示启动方式
        val tvLaunchMethods = android.widget.TextView(this).apply {
            text = "\n打开此界面的方法：\n\n" +
                    HiddenLauncherActivity.getLaunchMethods(this@AdminActivity)
                        .joinToString("\n\n") { "${it.first}\n${it.second}" }
            textSize = 12f
            setTextColor(android.graphics.Color.GRAY)
            setPadding(0, 30, 0, 0)
        }

        layout.addView(tvTitle)
        layout.addView(tvStatus)
        layout.addView(btnEnableAdmin)
        layout.addView(btnDisableAdmin)
        layout.addView(btnOpenMain)
        layout.addView(btnOpenSettings)
        layout.addView(btnAppInfo)
        layout.addView(tvLaunchMethods)

        setContentView(layout)
    }

    private fun getStatusText(): String {
        val isAdmin = AdminReceiver.isAdminActive(this)
        val isRunning = ForegroundService.isEnabled
        return "设备管理员：${if (isAdmin) "已激活 ✓" else "未激活 ✗"}\n" +
               "弹窗服务：${if (isRunning) "运行中 ✓" else "已暂停 ✗"}\n" +
               "弹窗时段：每小时的 0-5分, 30-35分\n" +
               "控制地址：http://${getLocalIpAddress()}:34567/admin?key=..."
    }

    private fun getLocalIpAddress(): String {
        return try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            val ipInt = wifiManager.connectionInfo.ipAddress
            android.text.format.Formatter.formatIpAddress(ipInt)
        } catch (e: Exception) {
            "未知"
        }
    }

    private fun enableDeviceAdmin() {
        if (AdminReceiver.isAdminActive(this)) {
            Toast.makeText(this, "设备管理员已经激活", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, AdminReceiver.getComponentName(this@AdminActivity))
            putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                "激活设备管理员后，可以防止孩子轻易卸载此应用。卸载前需要先在此界面取消激活。")
        }
        
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQUEST_ADMIN)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开设备管理员设置", Toast.LENGTH_LONG).show()
        }
    }

    private fun showDisablePasswordDialog() {
        if (!AdminReceiver.isAdminActive(this)) {
            Toast.makeText(this, "设备管理员未激活", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("危险操作")
        builder.setMessage("取消设备管理员后，应用可以被卸载。\n请输入取消密码：")

        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        
        val container = LinearLayout(this).apply {
            setPadding(50, 20, 50, 20)
            addView(input)
        }
        builder.setView(container)

        builder.setPositiveButton("确定") { _, _ ->
            if (input.text.toString() == CANCEL_ADMIN_PASSWORD) {
                disableDeviceAdmin()
            } else {
                Toast.makeText(this, "密码错误", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("取消", null)
        builder.show()
    }

    private fun disableDeviceAdmin() {
        try {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
            dpm.removeActiveAdmin(AdminReceiver.getComponentName(this))
            Toast.makeText(this, "设备管理员已取消，现在可以卸载应用", Toast.LENGTH_LONG).show()
            recreate()
        } catch (e: Exception) {
            Toast.makeText(this, "取消失败：${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADMIN) {
            recreate()
        }
    }
}
