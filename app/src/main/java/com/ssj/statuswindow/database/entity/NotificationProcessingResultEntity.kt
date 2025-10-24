package com.ssj.statuswindow.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * 알림 처리 결과를 저장하는 엔티티
 * 신뢰도 점수와 처리 상태를 포함
 */
@Entity(tableName = "notification_processing_results")
data class NotificationProcessingResultEntity(
    @PrimaryKey
    val id: String,
    val packageName: String,
    val appName: String,
    val content: String,
    val processedAt: LocalDateTime,
    val confidenceScore: Float,
    val processingStatus: String, // ProcessingStatus enum을 String으로 저장
    val extractedDataJson: String? = null, // ExtractedFinancialData를 JSON으로 저장
    val errorMessage: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
