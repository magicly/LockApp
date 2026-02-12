package com.example.lock30min

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import fi.iki.elonen.NanoHTTPD
import java.math.BigInteger
import java.security.MessageDigest

/**
 * HTTP 控制服务器
 * 家长可以通过访问 http://[ip]:34567/admin?key=xxx 来控制弹窗开关
 */
class HttpControlServer(private val context: Context, port: Int) : NanoHTTPD(port) {

    companion object {
        private const val ADMIN_KEY = "d33a560e81699606e5c9d32341cae435"
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val params = session.parms
        val providedKey = params["key"] ?: ""

        // 验证密钥
        if (!verifyKey(providedKey)) {
            return newFixedLengthResponse(
                Response.Status.FORBIDDEN,
                "text/html",
                getErrorPage("访问被拒绝", "密钥无效")
            )
        }

        return when {
            uri == "/" || uri == "/admin" -> {
                // 处理开关操作
                val action = params["action"]
                when (action) {
                    "disable" -> {
                        ForegroundService.isEnabled = false
                        newFixedLengthResponse(
                            Response.Status.OK,
                            "text/html",
                            getSuccessPage("弹窗已临时关闭", "护眼弹窗已暂停，孩子可以自由使用手机。", false)
                        )
                    }
                    "enable" -> {
                        ForegroundService.isEnabled = true
                        newFixedLengthResponse(
                            Response.Status.OK,
                            "text/html",
                            getSuccessPage("弹窗已开启", "护眼弹窗已恢复，将按时提醒休息。", true)
                        )
                    }
                    else -> {
                        newFixedLengthResponse(
                            Response.Status.OK,
                            "text/html",
                            getControlPage(ForegroundService.isEnabled)
                        )
                    }
                }
            }
            uri == "/status" -> {
                val status = if (ForegroundService.isEnabled) "enabled" else "disabled"
                newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    "{\"status\": \"$status\"}"
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
            val ipInt = wifiManager.connectionInfo.ipAddress
            Formatter.formatIpAddress(ipInt)
        } catch (e: Exception) {
            "获取失败"
        }
    }

    private fun getControlPage(isEnabled: Boolean): String {
        val statusText = if (isEnabled) "✅ 运行中" else "❌ 已暂停"
        val statusColor = if (isEnabled) "#4CAF50" else "#F44336"
        val nextAction = if (isEnabled) "disable" else "enable"
        val actionText = if (isEnabled) "临时关闭弹窗" else "恢复弹窗"
        val actionColor = if (isEnabled) "#F44336" else "#4CAF50"
        
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
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
            max-width: 400px;
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
        .info-item:last-child {
            margin-bottom: 0;
        }
        .info-label {
            color: #666;
        }
        .info-value {
            color: #333;
            font-weight: 500;
        }
        .action-button {
            display: block;
            width: 100%;
            padding: 16px;
            border: none;
            border-radius: 12px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s;
            text-decoration: none;
            text-align: center;
            background: $actionColor;
            color: white;
        }
        .action-button:hover {
            opacity: 0.9;
            transform: translateY(-2px);
        }
        .footer {
            margin-top: 25px;
            text-align: center;
            font-size: 12px;
            color: #999;
        }
        .icon {
            font-size: 48px;
            margin-bottom: 15px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="icon">👨‍👩‍👧‍👦</div>
            <h1>家长控制面板</h1>
            <span class="status-badge">$statusText</span>
        </div>
        
        <div class="info-card">
            <div class="info-item">
                <span class="info-label">弹窗时段</span>
                <span class="info-value">每小时的 0-5分, 30-35分</span>
            </div>
            <div class="info-item">
                <span class="info-label">弹窗时长</span>
                <span class="info-value">5分钟</span>
            </div>
            <div class="info-item">
                <span class="info-label">设备IP</span>
                <span class="info-value">${getLocalIpAddress()}</span>
            </div>
        </div>
        
        <a href="/admin?key=$ADMIN_KEY&action=$nextAction" class="action-button">
            $actionText
        </a>
        
        <div class="footer">
            LockApp 护眼提醒 · 保护孩子视力
        </div>
    </div>
</body>
</html>
        """.trimIndent()
    }

    private fun getSuccessPage(title: String, message: String, isEnabled: Boolean): String {
        val icon = if (isEnabled) "✅" else "⏸️"
        val color = if (isEnabled) "#4CAF50" else "#FF9800"
        
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>操作成功</title>
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
            max-width: 360px;
            width: 100%;
            text-align: center;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
        }
        .icon {
            font-size: 64px;
            margin-bottom: 20px;
        }
        h1 {
            color: #333;
            font-size: 22px;
            margin-bottom: 15px;
        }
        p {
            color: #666;
            font-size: 15px;
            margin-bottom: 25px;
            line-height: 1.6;
        }
        .back-button {
            display: inline-block;
            padding: 14px 35px;
            background: $color;
            color: white;
            text-decoration: none;
            border-radius: 10px;
            font-weight: 500;
            transition: opacity 0.3s;
        }
        .back-button:hover {
            opacity: 0.9;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="icon">$icon</div>
        <h1>$title</h1>
        <p>$message</p>
        <a href="/admin?key=$ADMIN_KEY" class="back-button">返回控制面板</a>
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
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>错误</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
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
            width: 100%;
            text-align: center;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
        }
        .icon {
            font-size: 64px;
            margin-bottom: 20px;
        }
        h1 {
            color: #333;
            font-size: 22px;
            margin-bottom: 15px;
        }
        p {
            color: #666;
            font-size: 15px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="icon">⚠️</div>
        <h1>$title</h1>
        <p>$message</p>
    </div>
</body>
</html>
        """.trimIndent()
    }
}
