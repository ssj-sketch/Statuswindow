package com.ssj.statuswindow.model

import java.time.LocalDateTime

/**
 * 알림 처리 결과를 나타내는 데이터 클래스
 * 신뢰도 점수와 처리 상태를 포함
 */
data class NotificationProcessingResult(
    val notificationId: String,
    val packageName: String,
    val appName: String,
    val content: String,
    val processedAt: LocalDateTime,
    val confidenceScore: Float, // 0.0 ~ 1.0 사이의 신뢰도 점수
    val processingStatus: ProcessingStatus,
    val extractedData: ExtractedFinancialData? = null,
    val errorMessage: String? = null
)

/**
 * 처리 상태 열거형
 */
enum class ProcessingStatus {
    SUCCESS,           // 성공적으로 파싱됨
    PARTIAL_SUCCESS,   // 부분적으로 파싱됨 (신뢰도 낮음)
    FAILED,           // 파싱 실패
    DUPLICATE,        // 중복 알림
    INVALID_FORMAT    // 잘못된 형식
}

/**
 * 추출된 금융 데이터
 */
data class ExtractedFinancialData(
    val transactionType: TransactionType,
    val amount: Long? = null,
    val currency: String = "KRW",
    val merchant: String? = null,
    val accountInfo: String? = null,
    val timestamp: LocalDateTime? = null,
    val rawText: String
)

/**
 * 거래 유형
 */
enum class TransactionType {
    CARD_PAYMENT,      // 카드 결제
    CARD_CANCELLATION, // 카드 취소
    BANK_DEPOSIT,      // 은행 입금
    BANK_WITHDRAWAL,   // 은행 출금
    SALARY,           // 급여
    AUTO_TRANSFER,    // 자동이체
    INVESTMENT,       // 투자
    UNKNOWN          // 알 수 없음
}
