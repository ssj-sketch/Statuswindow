package com.ssj.statuswindow.util

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ssj.statuswindow.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 화면별 수정사항 알림을 관리하는 매니저
 */
class ModificationNotificationManager(private val activity: AppCompatActivity) {
    
    private val tracker = ScreenModificationTracker(activity)
    private var notificationView: View? = null
    
    /**
     * 화면에 수정사항 알림 표시
     */
    fun showModificationNotification(
        containerLayout: LinearLayout,
        screenName: String,
        onViewDetails: (() -> Unit)? = null
    ) {
        val latestModification = tracker.getLatestModification(screenName)
        if (latestModification == null || latestModification.isRead) {
            hideNotification(containerLayout)
            return
        }
        
        // 기존 알림 제거
        hideNotification(containerLayout)
        
        // 새 알림 생성
        val inflater = LayoutInflater.from(activity)
        notificationView = inflater.inflate(R.layout.modification_notification, containerLayout, false)
        
        setupNotificationViews(notificationView!!, latestModification, onViewDetails)
        
        // 컨테이너에 추가 (맨 위에)
        containerLayout.addView(notificationView, 0)
        
        // 애니메이션으로 표시
        notificationView?.alpha = 0f
        notificationView?.animate()?.alpha(1f)?.setDuration(300)?.start()
    }
    
    /**
     * 수정사항 알림 숨기기
     */
    fun hideNotification(containerLayout: LinearLayout) {
        notificationView?.let { view ->
            view.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    containerLayout.removeView(view)
                }
                .start()
        }
        notificationView = null
    }
    
    /**
     * 수정사항 등록
     */
    fun registerModification(screenName: String, modification: String, details: String = "") {
        tracker.registerModification(screenName, modification, details)
    }
    
    /**
     * 읽지 않은 수정사항 개수 조회
     */
    fun getUnreadCount(screenName: String): Int {
        return tracker.getUnreadCount(screenName)
    }
    
    private fun setupNotificationViews(
        view: View,
        modification: ModificationData,
        onViewDetails: (() -> Unit)?
    ) {
        val tvModificationTime = view.findViewById<TextView>(R.id.tvModificationTime)
        val tvModificationDetails = view.findViewById<TextView>(R.id.tvModificationDetails)
        val btnViewDetails = view.findViewById<Button>(R.id.btnViewDetails)
        val btnMarkAsRead = view.findViewById<Button>(R.id.btnMarkAsRead)
        val btnCloseNotification = view.findViewById<ImageButton>(R.id.btnCloseNotification)
        
        // 시간 표시
        tvModificationTime.text = formatTimeAgo(modification.timestamp)
        
        // 수정사항 상세 표시
        tvModificationDetails.text = modification.modification
        if (modification.details.isNotEmpty()) {
            tvModificationDetails.text = "${modification.modification}\n${modification.details}"
        }
        
        // 자세히 보기 버튼
        btnViewDetails.setOnClickListener {
            onViewDetails?.invoke()
            hideNotification(view.parent as LinearLayout)
        }
        
        // 확인함 버튼
        btnMarkAsRead.setOnClickListener {
            tracker.markAsRead(modification.screenName)
            hideNotification(view.parent as LinearLayout)
        }
        
        // 닫기 버튼
        btnCloseNotification.setOnClickListener {
            hideNotification(view.parent as LinearLayout)
        }
    }
    
    private fun formatTimeAgo(timestamp: String): String {
        return try {
            val time = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val now = LocalDateTime.now()
            val diff = java.time.Duration.between(time, now)
            
            when {
                diff.toMinutes() < 1 -> "방금 전"
                diff.toMinutes() < 60 -> "${diff.toMinutes()}분 전"
                diff.toHours() < 24 -> "${diff.toHours()}시간 전"
                diff.toDays() < 7 -> "${diff.toDays()}일 전"
                else -> timestamp.substring(0, 10) // 날짜만 표시
            }
        } catch (e: Exception) {
            "방금 전"
        }
    }
}
