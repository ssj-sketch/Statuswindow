package com.ssj.statuswindow.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * SMS 처리 로그 Room 엔티티
 * 입력된 SMS와 처리 결과를 저장
 */
@Entity(tableName = "sms_processing_logs")
data class SmsProcessingLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val inputSms: String,              // 입력된 SMS 텍스트
    val processingStatus: String,      // 처리 상태 (SUCCESS, FAILED, PARTIAL)
    val errorMessage: String = "",     // 오류 메시지 (실패 시)
    val parsedEntitiesCount: Int = 0,   // 파싱된 엔티티 개수
    val cardTransactionIds: String = "", // 저장된 카드 거래 ID들 (JSON)
    val incomeTransactionIds: String = "", // 저장된 수입 내역 ID들 (JSON)
    val bankBalanceIds: String = "",    // 저장된 은행 잔고 ID들 (JSON)
    val processingTimeMs: Long = 0,     // 처리 시간 (밀리초)
    val createdAt: LocalDateTime = LocalDateTime.now() // 생성일시
)
