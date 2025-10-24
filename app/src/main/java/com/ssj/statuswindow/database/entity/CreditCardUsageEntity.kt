package com.ssj.statuswindow.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * 신용카드 사용내역 Room 엔티티
 * 기존 CardTransactionEntity에서 신용카드 관련 필드만 분리
 */
@Entity(tableName = "credit_card_usage")
data class CreditCardUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 카드 정보
    val cardType: String,           // 카드 종류 (예: 신한카드)
    val cardNumber: String,         // 카드 번호 (예: 1054)
    val cardName: String,          // 카드명 (예: 신한카드1054)
    
    // 거래 정보
    val transactionType: String,    // 승인/취소구분 (예: 승인)
    val amount: Long,              // 금액 (예: 98700)
    val installment: String,       // 할부 (예: 일시불, 2개월, 3개월)
    val installmentMonths: Int,    // 할부 개월수 (예: 1, 2, 3)
    val monthlyPayment: Long,      // 월 납부금액 (할부 첫달)
    
    // 거래 상세
    val transactionDate: LocalDateTime, // 사용일시
    val merchant: String,          // 가맹점 (예: 카톨릭대병원)
    val merchantCategory: String?, // 가맹점 카테고리 (예: 의료)
    
    // 청구 정보
    val billingYear: Int,          // 청구년도 (예: 2025)
    val billingMonth: Int,         // 청구월 (예: 10)
    val billingAmount: Long,       // 해당월 청구금액
    
    // 누적 정보
    val cumulativeAmount: Long,    // 누적사용금액 (예: 1960854)
    val monthlyBillAmount: Long,   // 이번달 청구금액
    
    // 메타 정보
    val user: String,              // 사용자 (예: 신*진)
    val originalText: String,     // 원본 SMS 텍스트
    val createdAt: LocalDateTime = LocalDateTime.now(), // 생성일시
    val updatedAt: LocalDateTime = LocalDateTime.now()  // 수정일시
)
