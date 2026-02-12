package com.example.lock30min

import android.content.Context
import android.util.Log

/**
 * 弹窗控制管理器
 * 用于在 Activity、Service 和 HTTP 服务器之间同步控制状态
 */
object LockControlManager {
    
    private const val TAG = "LockControlManager"
    
    /**
     * 控制模式
     */
    enum class ControlMode {
        AUTO,       // 自动模式：按时间段显示（0-5分，30-35分）
        FORCE_LOCK, // 强制锁屏：立即显示弹窗，不受时间限制
        FORCE_UNLOCK // 强制解锁：立即隐藏弹窗，不受时间限制
    }
    
    // 当前控制模式
    @Volatile
    private var currentMode = ControlMode.AUTO
    
    // 强制锁屏时长（分钟），默认5分钟
    @Volatile
    private var forceLockDurationMinutes = 5
    
    // 自定义锁屏消息
    @Volatile
    private var customLockMessage: String? = null
    
    // 状态监听器
    private val listeners = mutableListOf<OnControlStateChangeListener>()
    
    interface OnControlStateChangeListener {
        fun onControlModeChanged(mode: ControlMode, durationMinutes: Int, customMessage: String?)
    }
    
    /**
     * 获取当前模式
     */
    fun getMode(): ControlMode = currentMode
    
    /**
     * 获取强制锁屏时长
     */
    fun getForceLockDuration(): Int = forceLockDurationMinutes
    
    /**
     * 获取自定义锁屏消息
     */
    fun getCustomLockMessage(): String? = customLockMessage
    
    /**
     * 获取状态描述
     */
    fun getStatusText(): String {
        return when (currentMode) {
            ControlMode.AUTO -> "自动（按时段）"
            ControlMode.FORCE_LOCK -> "强制锁屏${forceLockDurationMinutes}分钟"
            ControlMode.FORCE_UNLOCK -> "强制解锁"
        }
    }
    
    /**
     * 设置控制模式
     */
    fun setMode(context: Context, mode: ControlMode, durationMinutes: Int = 5, customMessage: String? = null) {
        Log.d(TAG, "setMode: mode=$mode, duration=$durationMinutes, message=$customMessage")
        val changed = currentMode != mode || forceLockDurationMinutes != durationMinutes || customLockMessage != customMessage
        if (changed) {
            currentMode = mode
            forceLockDurationMinutes = durationMinutes.coerceIn(1, 60) // 限制1-60分钟
            customLockMessage = customMessage?.takeIf { it.isNotBlank() }
            // 通知所有监听器
            listeners.forEach { it.onControlModeChanged(mode, forceLockDurationMinutes, customLockMessage) }
            Log.d(TAG, "Mode changed to: $mode, duration: $forceLockDurationMinutes min, message: $customLockMessage, listeners: ${listeners.size}")
        }
    }
    
    /**
     * 强制立即锁屏（使用默认时长5分钟）
     */
    fun forceLock(context: Context) {
        Log.d(TAG, "forceLock called with default duration")
        setMode(context, ControlMode.FORCE_LOCK, 5)
    }
    
    /**
     * 强制立即锁屏（指定时长和消息）
     */
    fun forceLock(context: Context, durationMinutes: Int, customMessage: String? = null) {
        Log.d(TAG, "forceLock called with duration: $durationMinutes min, message: $customMessage")
        setMode(context, ControlMode.FORCE_LOCK, durationMinutes, customMessage)
    }
    
    /**
     * 强制立即解锁
     */
    fun forceUnlock(context: Context) {
        Log.d(TAG, "forceUnlock called")
        customLockMessage = null // 清除自定义消息
        setMode(context, ControlMode.FORCE_UNLOCK)
    }
    
    /**
     * 恢复自动模式
     */
    fun resetToAuto(context: Context) {
        Log.d(TAG, "resetToAuto called")
        customLockMessage = null // 清除自定义消息
        setMode(context, ControlMode.AUTO)
    }
    
    /**
     * 注册状态监听器
     */
    fun registerListener(listener: OnControlStateChangeListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
            Log.d(TAG, "Listener registered, total: ${listeners.size}")
        }
    }
    
    /**
     * 取消注册状态监听器
     */
    fun unregisterListener(listener: OnControlStateChangeListener) {
        listeners.remove(listener)
        Log.d(TAG, "Listener unregistered, total: ${listeners.size}")
    }
}
