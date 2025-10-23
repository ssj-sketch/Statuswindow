package com.ssj.statuswindow.util

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.ssj.statuswindow.data.model.AppNotificationLog
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 알림 히스토리를 읽어오는 유틸리티 클래스
 */
class NotificationHistoryReader(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    /**
     * 알림 히스토리 읽기 권한이 있는지 확인
     */
    fun hasNotificationHistoryPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            true // Android 13 미만에서는 별도 권한 불필요
        }
    }
    
    /**
     * 알림 히스토리 읽기 권한 요청
     */
    fun requestNotificationHistoryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 이상에서는 알림 정책 접근 권한이 필요
            // 이 권한은 사용자가 수동으로 설정해야 함
        }
    }
    
    /**
     * 지정된 기간의 알림 히스토리를 읽어옴
     * @param daysBack 몇 일 전까지의 알림을 가져올지 (기본 30일)
     */
    fun getNotificationHistory(daysBack: Int = 30): List<AppNotificationLog> {
        if (!hasNotificationHistoryPermission()) {
            return emptyList()
        }
        
        // Android 13 미만에서는 알림 히스토리 API가 제한적이므로 빈 리스트 반환
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return emptyList()
        }
        
        val notifications = mutableListOf<AppNotificationLog>()
        val cutoffTime = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000L)
        
        try {
            // Android 13 이상에서만 사용 가능한 API는 현재 제한적이므로
            // 실제 구현은 추후 Android API 업데이트 시 구현 예정
            // 현재는 빈 리스트를 반환
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return notifications.sortedByDescending { it.postedAtEpochMillis }
    }
    
    /**
     * 패키지명으로 앱 이름 가져오기
     */
    private fun getAppName(packageName: String): String {
        return try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
    
    /**
     * 패키지명으로 앱 카테고리 가져오기
     */
    private fun getAppCategory(packageName: String): String {
        return when {
            packageName.contains("bank") || packageName.contains("card") -> "금융"
            packageName.contains("shopping") || packageName.contains("market") -> "쇼핑"
            packageName.contains("social") || packageName.contains("chat") -> "소셜"
            packageName.contains("game") -> "게임"
            packageName.contains("news") || packageName.contains("media") -> "뉴스"
            else -> "기타"
        }
    }
    
    /**
     * 알림에서 텍스트 추출 (현재는 더미 구현)
     */
    private fun extractNotificationText(packageName: String): String {
        return "알림 내용 (API 제한으로 실제 내용 표시 불가)"
    }
}
