package com.example.lock30min

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * 权限设置引导页面
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        // 标题
        val tvTitle = TextView(this).apply {
            text = "权限设置"
            textSize = 24f
            setPadding(0, 0, 0, 30)
        }

        // 说明文字
        val tvDesc = TextView(this).apply {
            text = "护眼提醒只需要悬浮窗权限即可工作。\n\n如需防止孩子卸载应用，请激活设备管理员。"
            textSize = 14f
            setPadding(0, 0, 0, 30)
        }

        layout.addView(tvTitle)
        layout.addView(tvDesc)

        // 按钮列表
        val buttons = listOf(
            Triple("1️⃣ 悬浮窗权限", "允许应用显示休息提醒弹窗", ::openOverlaySettings),
            Triple("2️⃣ 激活设备管理员", "防止应用被轻易卸载", ::openAdminSettings),
            Triple("3️⃣ 应用详情", "其他设置", ::openAppDetails)
        )

        for ((title, desc, action) in buttons) {
            val btn = Button(this).apply {
                text = "$title\n$desc"
                textSize = 14f
                setPadding(20, 30, 20, 30)
                setOnClickListener { action() }
            }
            layout.addView(btn)
            layout.addView(TextView(this).apply { 
                setPadding(0, 10, 0, 10) 
            })
        }

        // 打开管理控制台
        val btnAdmin = Button(this).apply {
            text = "打开家长管理控制台"
            setOnClickListener {
                startActivity(Intent(this@SettingsActivity, HiddenLauncherActivity::class.java))
            }
        }

        // 启动主功能
        val btnStart = Button(this).apply {
            text = "完成设置，启动护眼提醒"
            setOnClickListener {
                if (Settings.canDrawOverlays(this@SettingsActivity)) {
                    startActivity(Intent(this@SettingsActivity, MainActivity::class.java))
                } else {
                    Toast.makeText(this@SettingsActivity, "请先开启悬浮窗权限", Toast.LENGTH_SHORT).show()
                }
            }
        }

        layout.addView(btnAdmin)
        layout.addView(TextView(this).apply { setPadding(0, 20, 0, 0) })
        layout.addView(btnStart)

        scrollView.addView(layout)
        setContentView(scrollView)
    }

    private fun openOverlaySettings() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAdminSettings() {
        if (AdminReceiver.isAdminActive(this)) {
            Toast.makeText(this, "设备管理员已激活", Toast.LENGTH_SHORT).show()
            return
        }
        
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, AdminReceiver.getComponentName(this@SettingsActivity))
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                "激活设备管理员后，可以防止孩子轻易卸载此应用。卸载前需要先取消激活。")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开设备管理员设置", Toast.LENGTH_LONG).show()
        }
    }

    private fun openAppDetails() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开应用详情", Toast.LENGTH_SHORT).show()
        }
    }
}
