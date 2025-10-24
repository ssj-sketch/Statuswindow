package com.ssj.statuswindow.model

import java.time.LocalDateTime

/**
 * 카드 거래 내역을 나타내는 데이터 클래스
 */
data class CardTransaction(
    val cardType: String,           // 카드 종류 (예: 신한카드)
    val cardNumber: String,         // 카드 번호 (예: 1054)
    val transactionType: String,    // 승인/취소구분 (예: 승인)
    val user: String,               // 사용자 (예: 신*진)
    val amount: Long,               // 금액 (예: 42820)
    val installment: String,        // 할부 (예: 일시불)
    val transactionDate: LocalDateTime, // 사용일시
    val merchant: String,           // 가맹점 (예: 이마트)
    val cumulativeAmount: Long,     // 누적사용금액 (예: 1903674)
    val category: String? = null,   // 카테고리 (머신러닝으로 자동분류)
    val memo: String = "",         // 사용자 메모
    val originalText: String        // 원본 SMS 텍스트
)
