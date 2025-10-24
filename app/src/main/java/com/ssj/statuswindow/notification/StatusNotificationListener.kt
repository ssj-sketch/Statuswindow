package com.ssj.statuswindow.notification

import android.app.Notification
import android.content.pm.ApplicationInfo
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ssj.statuswindow.repo.CardEventRepository
import com.ssj.statuswindow.repo.NotificationLogRepository
import com.ssj.statuswindow.data.model.AppNotificationLog
import com.ssj.statuswindow.util.AppCategoryResolver
import com.ssj.statuswindow.util.SmsParser
import com.ssj.statuswindow.service.NotificationProcessingService
import com.ssj.statuswindow.model.NotificationProcessingResult
import com.ssj.statuswindow.model.ProcessingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.util.Log

class StatusNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "StatusNotificationListener"
    }

    private val cardRepo by lazy { CardEventRepository.instance(this) }
    private val notificationRepo by lazy { NotificationLogRepository.instance(this) }
    private val processingService by lazy { NotificationProcessingService(this) }
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        // 연결 시 별도 처리 없음
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            // 알림 내용 추출
            val notificationContent = extractNotificationContent(sbn)
            val postedAt = Instant.ofEpochMilli(sbn.postTime)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime()
            
            // 앱 정보 추출
            val appInfo = getAppInfo(sbn.packageName)
            val appName = appInfo.first
            val appCategory = appInfo.second
            
            // 개선된 알림 처리 서비스 사용
            val processingResult = processingService.processNotification(
                packageName = sbn.packageName,
                appName = appName,
                content = notificationContent,
                postedAt = postedAt
            )
            
            // 처리 결과에 따른 후속 작업
            handleProcessingResult(processingResult, sbn, appCategory)
            
            Log.d(TAG, "Notification processed: ${processingResult.appName}, " +
                    "confidence: ${processingResult.confidenceScore}, " +
                    "status: ${processingResult.processingStatus}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification", e)
        }
    }
    
    /**
     * 알림 내용 추출
     */
    private fun extractNotificationContent(sbn: StatusBarNotification): String {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val big = extras.getCharSequence("android.bigText")?.toString().orEmpty()
        val lines = extras.getCharSequenceArray("android.textLines")
            ?.joinToString("\n") { it.toString() }
            .orEmpty()
        
        return sequenceOf(title, text, big, lines)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString("\n")
    }
    
    /**
     * 앱 정보 추출
     */
    private fun getAppInfo(packageName: String): Pair<String, String> {
        val pm = packageManager
        val appInfo: ApplicationInfo? = try {
            pm.getApplicationInfo(packageName, 0)
        } catch (_: Exception) {
            null
        }
        
        val appName = try {
            if (appInfo != null) pm.getApplicationLabel(appInfo)?.toString().orEmpty()
            else packageName
        } catch (_: Exception) {
            packageName
        }
        
        val category = AppCategoryResolver.resolve(this, appInfo)
        
        return Pair(appName, category)
    }
    
    /**
     * 처리 결과에 따른 후속 작업
     */
    private fun handleProcessingResult(
        result: NotificationProcessingResult,
        sbn: StatusBarNotification,
        appCategory: String
    ) {
        coroutineScope.launch {
            try {
                // 1. 알림 로그 저장 (모든 알림)
                persistNotificationLog(result, sbn, appCategory)
                
                // 2. 성공적으로 파싱된 경우에만 카드 이벤트 저장
                if (result.processingStatus == ProcessingStatus.SUCCESS && result.extractedData != null) {
                    handleSuccessfulParsing(result)
                }
                
                // 3. 부분 성공 또는 실패한 경우 로그만 남김
                if (result.processingStatus == ProcessingStatus.PARTIAL_SUCCESS ||
                    result.processingStatus == ProcessingStatus.FAILED) {
                    Log.w(TAG, "Low confidence parsing for ${result.appName}: ${result.errorMessage}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling processing result", e)
            }
        }
    }
    
    /**
     * 성공적으로 파싱된 경우 카드 이벤트 저장
     */
    private fun handleSuccessfulParsing(result: NotificationProcessingResult) {
        try {
            val extractedData = result.extractedData!!
            
            // 기존 SMS 파서를 사용하여 CardEvent 생성
            val parsed = SmsParser.parse(result.content)
            
            if (parsed.isNotEmpty()) {
                // 신뢰도 점수를 고려하여 저장
                val adjustedEvents = parsed.map { event ->
                    event.copy(
                        sourceApp = result.packageName,
                        raw = result.content
                    )
                }
                
                cardRepo.addAll(adjustedEvents)
                
                Log.d(TAG, "Successfully saved ${adjustedEvents.size} card events from ${result.appName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving card events", e)
        }
    }

    /**
     * 알림 로그 저장 (개선된 버전)
     */
    private fun persistNotificationLog(
        result: NotificationProcessingResult,
        sbn: StatusBarNotification,
        appCategory: String
    ) {
        val notificationCategory = sbn.notification.category ?: resolveLegacyCategory(sbn.notification)
        val content = if (result.content.isBlank()) sbn.notification.tickerText?.toString().orEmpty() else result.content
        
        val entry = AppNotificationLog(
            id = buildNotificationId(sbn),
            packageName = result.packageName,
            appName = result.appName.ifBlank { result.packageName },
            appCategory = appCategory,
            postedAtIso = result.processedAt.format(fmt),
            postedAtEpochMillis = sbn.postTime,
            notificationCategory = notificationCategory,
            content = content
        )
        
        coroutineScope.launch {
            try {
                notificationRepo.add(entry)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving notification log", e)
            }
        }
    }

    private fun buildNotificationId(sbn: StatusBarNotification): String {
        val tag = sbn.tag ?: ""
        return listOf(sbn.packageName, tag, sbn.id.toString(), sbn.postTime.toString())
            .joinToString(":")
    }

    private fun resolveLegacyCategory(notification: Notification): String? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            notification.category
        } else {
            null
        }
    }
}
