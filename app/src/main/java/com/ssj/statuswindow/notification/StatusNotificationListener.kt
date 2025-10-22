package com.ssj.statuswindow.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ssj.statuswindow.model.CardEvent
import com.ssj.statuswindow.repo.CardEventRepository
import com.ssj.statuswindow.util.SmsParser
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class StatusNotificationListener : NotificationListenerService() {

    private val repo by lazy { CardEventRepository.instance(this) }
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun onListenerConnected() {
        super.onListenerConnected()
        // 연결 시 별도 처리 없음
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val big = extras.getCharSequence("android.bigText")?.toString().orEmpty()
        val body = listOf(title, text, big).joinToString("\n").trim()

        // 고급 파서 시도
        val parsed = SmsParser.parse(body).ifEmpty {
            // 파싱 실패 시에도 한 건으로 저장하되 amount=0L로 Long 타입 유지
            listOf(
                CardEvent(
                    id = UUID.randomUUID().toString(),
                    time = LocalDateTime.now().format(fmt),
                    merchant = if (title.isNotBlank()) title else sbn.packageName,
                    amount = 0L,                       // ✅ Long 타입으로 변경
                    sourceApp = sbn.packageName,
                    raw = body
                )
            )
        }

        // sourceApp 비어있으면 패키지명으로 보정
        repo.addAll(parsed.map {
            if (it.sourceApp.isBlank()) it.copy(sourceApp = sbn.packageName) else it
        })
    }
}
