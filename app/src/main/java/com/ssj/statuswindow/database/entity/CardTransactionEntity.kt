package com.ssj.statuswindow.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * 카드 거래 내역 Room 엔티티
 */
@Entity(tableName = "card_transactions")
data class CardTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardType: String,           // 카드 종류 (예: 신한카드)
    val cardNumber: String,         // 카드 번호 (예: 1054)
    val transactionType: String,    // 승인/취소구분 (예: 승인)
    val user: String,               // 사용자 (예: 신*진)
    val amount: Long,               // 금액 (예: 98700)
    val installment: String,        // 할부 (예: 일시불)
    val transactionDate: LocalDateTime, // 사용일시
    val merchant: String,           // 가맹점 (예: 가톨릭대병원)
    val cumulativeAmount: Long,     // 누적사용금액 (예: 1960854)
    val category: String? = null,   // 카테고리 (머신러닝으로 자동분류)
    val memo: String = "",         // 사용자 메모
    val originalText: String,       // 원본 SMS 텍스트
    val createdAt: LocalDateTime = LocalDateTime.now() // 생성일시
)
