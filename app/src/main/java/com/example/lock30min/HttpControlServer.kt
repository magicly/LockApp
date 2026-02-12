package com.example.lock30min

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import fi.iki.elonen.NanoHTTPD

class HttpControlServer(private val context: Context, port: Int) : NanoHTTPD(port) {

    companion object {
        private const val TAG = "HttpControlServer"
        private const val ADMIN_KEY = "d33a560e81699606e5c9d32341cae435"
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val params = session.parms
        val providedKey = params["key"] ?: ""

        if (!verifyKey(providedKey)) {
            return newFixedLengthResponse(
                Response.Status.FORBIDDEN,
                "text/html",
                getErrorPage("访问被拒绝", "密钥无效")
            )
        }

        return when {
            uri == "/" || uri == "/admin" -> {
                val action = params["action"]
                val appContext = LockApp.instance?.applicationContext
                
                when (action) {
                    "force_lock" -> {
                        val duration = params["duration"]?.toIntOrNull()?.coerceIn(1, 60) ?: 5
                        if (appContext != null) {
                            LockControlManager.forceLock(appContext, duration)
                        }
                        newFixedLengthResponse(
                            Response.Status.OK,
                            "text/html",
                            getSuccessPage("已强制锁屏", "弹窗已立即显示，将锁定 $duration 分钟。", "lock")
                        )
                    }
                    "force_unlock" -> {
                        if (appContext != null) {
                            LockControlManager.forceUnlock(appContext)
                        }
                        newFixedLengthResponse(
                            Response.Status.OK,
                            "text/html",
                            getSuccessPage("已强制解锁", "弹窗已立即关闭，孩子可以继续使用。", "unlock")
                        )
                    }
                    "auto" -> {
                        if (appContext != null) {
                            LockControlManager.resetToAuto(appContext)
                        }
                        newFixedLengthResponse(
                            Response.Status.OK,
                            "text/html",
                            getSuccessPage("已恢复自动模式", "将按时间段自动提醒休息。", "auto")
                        )
                    }
                    "show_icon" -> {
                        if (appContext != null) {
                            MainActivity.setIconVisible(appContext, true)
                        }
                        newFixedLengthResponse(
                            Response.Status.OK,
                            "text/html",
                            getSuccessPage("图标已显示", "应用图标已显示在桌面。", "icon")
                        )
                    }
                    "hide_icon" -> {
                        if (appContext != null) {
                            MainActivity.setIconVisible(appContext, false)
                        }
                        newFixedLengthResponse(
                            Response.Status.OK,
                            "text/html",
                            getSuccessPage("图标已隐藏", "应用图标已从桌面隐藏。", "icon")
                        )
                    }
                    else -> {
                        newFixedLengthResponse(
                            Response.Status.OK,
                            "text/html",
                            getControlPage(
                                LockControlManager.getMode(),
                                LockControlManager.getForceLockDuration(),
                                MainActivity.isIconVisible(context)
                            )
                        )
                    }
                }
            }
            uri == "/status" -> {
                val mode = LockControlManager.getMode()
                val duration = LockControlManager.getForceLockDuration()
                val iconVisible = MainActivity.isIconVisible(context)
                newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    "{\"mode\": \"$mode\", \"duration\": $duration, \"icon_visible\": $iconVisible}"
                )
            }
            else -> {
                newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    "text/html",
                    getErrorPage("页面不存在", "请求的页面未找到")
                )
            }
        }
    }

    private fun verifyKey(key: String): Boolean {
        return key == ADMIN_KEY
    }

    private fun getLocalIpAddress(): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
        } catch (e: Exception) {
            "获取失败"
        }
    }

    private fun getControlPage(mode: LockControlManager.ControlMode, defaultDuration: Int, iconVisible: Boolean): String {
        val statusText = when (mode) {
            LockControlManager.ControlMode.AUTO -> "自动模式"
            LockControlManager.ControlMode.FORCE_LOCK -> "强制锁屏中"
            LockControlManager.ControlMode.FORCE_UNLOCK -> "强制解锁中"
        }
        val statusColor = when (mode) {
            LockControlManager.ControlMode.AUTO -> "#4CAF50"
            LockControlManager.ControlMode.FORCE_LOCK -> "#F44336"
            LockControlManager.ControlMode.FORCE_UNLOCK -> "#2196F3"
        }
        
        val iconStatusText = if (iconVisible) "显示中" else "已隐藏"
        val iconStatusColor = if (iconVisible) "#4CAF50" else "#FF9800"
        val nextIconAction = if (iconVisible) "hide_icon" else "show_icon"
        val nextIconText = if (iconVisible) "隐藏图标" else "显示图标"
        val nextIconColor = if (iconVisible) "#FF9800" else "#4CAF50"
        
        val ipAddress = getLocalIpAddress()
        
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="Cache-Control" content="no-cache">
    <title>家长控制面板</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 20px;
        }
        .container {
            background: white;
            border-radius: 20px;
            padding: 40px;
            max-width: 420px;
            width: 100%;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
        }
        .header {
            text-align: center;
            margin-bottom: 30px;
        }
        .header h1 {
            color: #333;
            font-size: 24px;
            margin-bottom: 10px;
        }
        .status-badge {
            display: inline-block;
            padding: 8px 20px;
            border-radius: 20px;
            font-size: 14px;
            font-weight: 500;
            background: ${statusColor}20;
            color: $statusColor;
            margin-bottom: 8px;
        }
        .icon-status-badge {
            display: inline-block;
            padding: 6px 16px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 500;
            background: ${iconStatusColor}20;
            color: $iconStatusColor;
        }
        .info-card {
            background: #f8f9fa;
            border-radius: 12px;
            padding: 20px;
            margin-bottom: 25px;
        }
        .info-item {
            display: flex;
            justify-content: space-between;
            margin-bottom: 12px;
            font-size: 14px;
        }
        .info-label { color: #666; }
        .info-value { color: #333; font-weight: 500; }
        .action-button {
            display: block;
            width: 100%;
            padding: 14px;
            border: none;
            border-radius: 10px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            text-decoration: none;
            text-align: center;
            color: white;
            margin-bottom: 10px;
        }
        .btn-lock { background: #F44336; }
        .btn-unlock { background: #2196F3; }
        .btn-auto { background: #4CAF50; }
        .btn-icon { background: $nextIconColor; }
        .refresh-button {
            display: block;
            width: 100%;
            padding: 12px;
            border: 1px solid #ddd;
            border-radius: 10px;
            font-size: 14px;
            text-align: center;
            background: white;
            color: #666;
            text-decoration: none;
        }
        .section-title {
            font-size: 12px;
            color: #999;
            margin: 20px 0 10px 0;
            text-transform: uppercase;
        }
        input[type="number"] {
            width: 60px;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 8px;
            text-align: center;
            font-size: 16px;
        }
        .lock-form {
            background: #fff5f5;
            border-radius: 12px;
            padding: 15px;
            margin-bottom: 10px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>家长控制面板</h1>
            <div><span class="status-badge">$statusText</span></div>
            <div style="margin-top: 8px;"><span class="icon-status-badge">图标$iconStatusText</span></div>
        </div>
        
        <div class="info-card">
            <div class="info-item">
                <span class="info-label">自动弹窗时段</span>
                <span class="info-value">每小时 0-5分, 30-35分</span>
            </div>
            <div class="info-item">
                <span class="info-label">设备IP</span>
                <span class="info-value">$ipAddress</span>
            </div>
        </div>
        
        <div class="section-title">弹窗控制</div>
        
        <form class="lock-form" action="/admin" method="GET">
            <input type="hidden" name="key" value="$ADMIN_KEY">
            <input type="hidden" name="action" value="force_lock">
            <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 10px;">
                <span>锁屏时长：</span>
                <input type="number" name="duration" value="$defaultDuration" min="1" max="60">
                <span>分钟</span>
            </div>
            <button type="submit" class="action-button btn-lock">立即锁屏</button>
        </form>
        
        <a href="/admin?key=$ADMIN_KEY&action=force_unlock" class="action-button btn-unlock">立即解锁</a>
        <a href="/admin?key=$ADMIN_KEY&action=auto" class="action-button btn-auto">恢复自动模式</a>
        
        <div class="section-title">图标控制</div>
        <a href="/admin?key=$ADMIN_KEY&action=$nextIconAction" class="action-button btn-icon">$nextIconText</a>
        
        <div style="margin-top: 20px;">
            <a href="/admin?key=$ADMIN_KEY" class="refresh-button">刷新状态</a>
        </div>
    </div>
</body>
</html>
        """.trimIndent()
    }

    private fun getSuccessPage(title: String, message: String, type: String): String {
        val color = when (type) {
            "lock" -> "#F44336"
            "unlock" -> "#2196F3"
            "icon" -> "#FF9800"
            else -> "#4CAF50"
        }
        
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta http-equiv="refresh" content="2;url=/admin?key=$ADMIN_KEY">
    <title>操作成功</title>
    <style>
        body {
            font-family: sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 20px;
        }
        .container {
            background: white;
            border-radius: 20px;
            padding: 40px;
            max-width: 360px;
            text-align: center;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
        }
        h1 { color: #333; font-size: 22px; margin-bottom: 15px; }
        p { color: #666; margin-bottom: 25px; }
        .btn {
            display: inline-block;
            padding: 14px 35px;
            background: $color;
            color: white;
            text-decoration: none;
            border-radius: 10px;
        }
        .note { margin-top: 15px; font-size: 12px; color: #999; }
    </style>
</head>
<body>
    <div class="container">
        <h1>$title</h1>
        <p>$message</p>
        <a href="/admin?key=$ADMIN_KEY" class="btn">返回控制面板</a>
        <p class="note">2秒后自动返回...</p>
    </div>
</body>
</html>
        """.trimIndent()
    }

    private fun getErrorPage(title: String, message: String): String {
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>错误</title>
    <style>
        body {
            font-family: sans-serif;
            background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 20px;
        }
        .container {
            background: white;
            border-radius: 20px;
            padding: 40px;
            max-width: 360px;
            text-align: center;
        }
        h1 { color: #333; }
        p { color: #666; }
    </style>
</head>
<body>
    <div class="container">
        <h1>$title</h1>
        <p>$message</p>
    </div>
</body>
</html>
        """.trimIndent()
    }
}
