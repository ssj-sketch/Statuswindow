package com.ssj.statuswindow.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * 알림 히스토리 권한 관리 유틸리티
 */
object NotificationHistoryPermissionManager {
    
    /**
     * 알림 히스토리 접근 권한이 있는지 확인
     */
    fun hasNotificationHistoryPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val mode = appOps.unsafeCheckOpNoThrow(
                    "android:read_notification_history",
                    android.os.Process.myUid(),
                    context.packageName
                )
                mode == AppOpsManager.MODE_ALLOWED
            } catch (e: Exception) {
                // API 레벨에 따라 지원되지 않는 경우 false 반환
                false
            }
        } else {
            // Android 13 미만에서는 알림 히스토리 권한이 없음
            false
        }
    }
    
    /**
     * 알림 히스토리 권한 요청을 위한 설정 화면으로 이동
     */
    fun requestNotificationHistoryPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }
    
    /**
     * 알림 접근 권한이 있는지 확인
     */
    fun hasNotificationAccessPermission(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return enabled != null && enabled.contains(context.packageName)
    }
    
    /**
     * 알림 접근 권한 요청을 위한 설정 화면으로 이동
     */
    fun requestNotificationAccessPermission(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
    
    /**
     * 권한 상태 정보를 반환
     */
    fun getPermissionStatus(context: Context): PermissionStatus {
        val hasAccess = hasNotificationAccessPermission(context)
        val hasHistory = hasNotificationHistoryPermission(context)
        
        return PermissionStatus(
            hasNotificationAccess = hasAccess,
            hasNotificationHistory = hasHistory,
            isAndroid13OrHigher = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        )
    }
}

/**
 * 권한 상태 정보
 */
data class PermissionStatus(
    val hasNotificationAccess: Boolean,
    val hasNotificationHistory: Boolean,
    val isAndroid13OrHigher: Boolean
)
