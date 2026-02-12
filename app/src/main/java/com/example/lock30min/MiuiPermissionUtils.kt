package com.example.lock30min

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * MIUI/HyperOS 特定的权限设置工具类
 * 由于小米系统对后台应用有严格限制，需要手动开含以下权限：
 * 1. 后台弹出界面 - 允许后台应用弹出窗口
 * 2. 显示悬浮窗 - SYSTEM_ALERT_WINDOW
 * 3. 自启动 - 应用启动时自动运行
 * 4. 省电策略 - 无限制
 */
object MiuiPermissionUtils {

    private const val TAG = "MiuiPermissionUtils"

    // 检测是否是 MIUI/HyperOS
    fun isMiui(): Boolean {
        return !getSystemProperty("ro.miui.ui.version.name").isNullOrEmpty() ||
               !getSystemProperty("ro.miui.ui.version.code").isNullOrEmpty() ||
               !getSystemProperty("ro.miui.version.code_time").isNullOrEmpty()
    }

    // 获取系统属性
    private fun getSystemProperty(key: String): String? {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java)
            method.invoke(null, key) as? String
        } catch (e: Exception) {
            null
        }
    }

    // 打开应用详情页面
    fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "无法打开应用详情", e)
        }
    }

    // 打开后台弹出界面设置 (MIUI/HyperOS 特定)
    fun openBackgroundPopupSettings(context: Context) {
        try {
            // 尝试多种跳转方式
            val intents = listOf(
                // HyperOS 新版路径
                Intent().apply {
                    component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.settings.PermCenterSettings"
                    )
                },
                // MIUI 12+ 路径
                Intent().apply {
                    component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.permissions.PermissionsEditorActivity"
                    )
                    putExtra("extra_package_uid", context.applicationInfo.uid)
                    putExtra("extra_pkgname", context.packageName)
                },
                // 备用路径
                Intent().apply {
                    component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.permissions.AppPermissionsEditor"
                    )
                    putExtra("extra_package_uid", context.applicationInfo.uid)
                    putExtra("extra_pkgname", context.packageName)
                }
            )

            var success = false
            for (intent in intents) {
                try {
                    context.startActivity(intent)
                    success = true
                    break
                } catch (e: Exception) {
                    continue
                }
            }

            if (!success) {
                // 如果上面都失败，打开应用详情
                openAppDetailsSettings(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "无法打开后台弹出界面设置", e)
            openAppDetailsSettings(context)
        }
    }

    // 打开自启动设置
    fun openAutoStartSettings(context: Context) {
        try {
            val intents = listOf(
                // MIUI/HyperOS 自启动设置
                Intent().apply {
                    component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                },
                // 备用路径
                Intent().apply {
                    component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartDetailManagementActivity"
                    )
                }
            )

            var success = false
            for (intent in intents) {
                try {
                    context.startActivity(intent)
                    success = true
                    break
                } catch (e: Exception) {
                    continue
                }
            }

            if (!success) {
                openAppDetailsSettings(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "无法打开自启动设置", e)
            openAppDetailsSettings(context)
        }
    }

    // 打开电池优化设置
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intents = listOf(
                // MIUI/HyperOS 电池设置
                Intent().apply {
                    component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.powercenter.PowerSettings"
                    )
                },
                // 应用智能省电
                Intent().apply {
                    component = ComponentName(
                        "com.miui.powerkeeper",
                        "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                    )
                    putExtra("package_name", context.packageName)
                }
            )

            var success = false
            for (intent in intents) {
                try {
                    context.startActivity(intent)
                    success = true
                    break
                } catch (e: Exception) {
                    continue
                }
            }

            if (!success) {
                // 使用系统默认方法
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    context.startActivity(intent)
                } else {
                    openAppDetailsSettings(context)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "无法打开电池优化设置", e)
            openAppDetailsSettings(context)
        }
    }

    // 打开悬浮窗权限设置
    fun openOverlaySettings(context: Context) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "无法打开悬浮窗设置", e)
            openAppDetailsSettings(context)
        }
    }

    // 获取权限设置指南文本
    fun getPermissionGuideText(): String {
        return """
            小米/HyperOS 设备需要手动开启以下权限：
            
            1. 后台弹出界面
               设置 -> 应用设置 -> 应用管理 -> 本应用 -> 后台弹出界面 -> 允许
            
            2. 显示悬浮窗
               设置 -> 应用设置 -> 应用管理 -> 本应用 -> 显示悬浮窗 -> 始终允许
            
            3. 自启动
               手机管家 -> 应用管理 -> 权限 -> 自启动 -> 允许本应用
            
            4. 省电策略
               设置 -> 电池与性能 -> 省电优化 -> 应用智能省电 -> 本应用 -> 无限制
               
            5. 锁定后台
               多任务界面 -> 长按本应用 -> 锁定
        """.trimIndent()
    }
}
