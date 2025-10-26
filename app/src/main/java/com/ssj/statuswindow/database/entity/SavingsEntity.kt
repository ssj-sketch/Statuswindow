package com.ssj.statuswindow.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * 적금 정보 Room 엔티티
 */
@Entity(tableName = "savings")
data class SavingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 적금 정보
    val bankName: String,              // 은행명 (예: 신한은행)
    val savingsName: String,           // 적금 상품명 (예: 신한스마트적금)
    val savingsType: String,           // 적금 유형 (예: 정기적금, 자유적금, 적립식적금)
    val accountNumber: String,         // 계좌번호 (예: 100-***-159993)
    
    // 금액 정보
    val targetAmount: Long,            // 목표 금액
    val currentBalance: Long,          // 현재 잔액
    val monthlyDeposit: Long,          // 월 납입금액
    val interestRate: Double,          // 이자율 (예: 3.5)
    
    // 기간 정보
    val contractDate: LocalDateTime,   // 가입일
    val maturityDate: LocalDateTime,   // 만기일
    val depositPeriod: Int,           // 납입 기간 (월)
    val remainingPeriod: Int,         // 남은 기간 (월)
    
    // 적립 정보
    val totalDepositAmount: Long,     // 총 납입액
    val earnedInterest: Long,         // 현재까지 받은 이자
    val expectedInterest: Long,       // 예상 이자
    val nextDepositDate: LocalDateTime?, // 다음 납입일
    
    // 메타 정보
    val status: String = "진행중",      // 상태 (예: 진행중, 만기, 중도해지)
    val description: String = "",      // 설명
    val memo: String = "",             // 메모
    val originalText: String? = null, // 원본 SMS 텍스트
    val createdAt: LocalDateTime = LocalDateTime.now(), // 생성일시
    val updatedAt: LocalDateTime = LocalDateTime.now() // 업데이트일시
)
