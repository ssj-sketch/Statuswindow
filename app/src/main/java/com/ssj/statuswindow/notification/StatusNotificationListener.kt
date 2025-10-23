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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class StatusNotificationListener : NotificationListenerService() {

    private val cardRepo by lazy { CardEventRepository.instance(this) }
    private val notificationRepo by lazy { NotificationLogRepository.instance(this) }
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        // 연결 시 별도 처리 없음
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val big = extras.getCharSequence("android.bigText")?.toString().orEmpty()
        val lines = extras.getCharSequenceArray("android.textLines")
            ?.joinToString("\n") { it.toString() }
            .orEmpty()
        val body = sequenceOf(title, text, big, lines)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString("\n")

        val postedAt = Instant.ofEpochMilli(sbn.postTime)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDateTime()

        persistNotificationLog(sbn, body, postedAt)

        // 고급 파서 시도 (실패 시 카드 이벤트에는 반영하지 않음)
        val parsed = SmsParser.parse(body)

        if (parsed.isNotEmpty()) {
            // sourceApp 비어있으면 패키지명으로 보정
            cardRepo.addAll(parsed.map {
                if (it.sourceApp.isBlank()) it.copy(sourceApp = sbn.packageName) else it
            })
        }
    }

    private fun persistNotificationLog(
        sbn: StatusBarNotification,
        body: String,
        postedAt: LocalDateTime
    ) {
        val pm = packageManager
        val appInfo: ApplicationInfo? = try {
            pm.getApplicationInfo(sbn.packageName, 0)
        } catch (_: Exception) {
            null
        }
        val appName = try {
            if (appInfo != null) pm.getApplicationLabel(appInfo)?.toString().orEmpty()
            else sbn.packageName
        } catch (_: Exception) {
            sbn.packageName
        }
        val category = AppCategoryResolver.resolve(this, appInfo)
        val notificationCategory = sbn.notification.category ?: resolveLegacyCategory(sbn.notification)
        val content = if (body.isBlank()) sbn.notification.tickerText?.toString().orEmpty() else body
        val entry = AppNotificationLog(
            id = buildNotificationId(sbn),
            packageName = sbn.packageName,
            appName = appName.ifBlank { sbn.packageName },
            appCategory = category,
            postedAtIso = postedAt.format(fmt),
            postedAtEpochMillis = sbn.postTime,
            notificationCategory = notificationCategory,
            content = content
        )
        coroutineScope.launch {
            notificationRepo.add(entry)
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
