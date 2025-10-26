package com.ssj.statuswindow.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * 대출 정보 Room 엔티티
 * 계좌번호 + 은행명 + 대출명 + 납입월으로 중복 방지
 * - paymentMonth가 빈 문자열("")이면 대출 기본 정보 (금리 재산정 등)
 * - paymentMonth가 "YYYY-MM" 형식이면 월별 이자 납부 정보
 */
@Entity(
    tableName = "loans",
    indices = [
        Index(value = ["accountNumber", "bankName", "loanName", "paymentMonth"], unique = true)
    ]
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 대출 정보
    val bankName: String,              // 은행명 (예: 신한은행)
    val loanName: String,             // 대출명 (예: 신용대출, 주택담보대출)
    val loanType: String,             // 대출 유형 (예: 신용대출, 주택담보, 전세자금대출)
    val accountNumber: String,         // 계좌번호 (예: 100-***-159993)
    val paymentMonth: String = "",    // 납입월 (예: 2025-10) - 중복 방지용
    
    // 금액 정보
    val loanAmount: Long = 0,             // 대출 원금
    val remainingPrincipal: Long = 0,     // 잔액 (남은 원금)
    val monthlyPayment: Long = 0,          // 월 상환금액
    val interestRate: Double = 0.0,          // 이자율 (예: 3.5)
    
    // 기간 정보
    val contractDate: LocalDateTime = LocalDateTime.now(),   // 계약일
    val maturityDate: LocalDateTime = LocalDateTime.now(),   // 만기일
    val repaymentPeriod: Int = 0,         // 상환 기간 (월)
    val remainingPeriod: Int = 0,         // 남은 기간 (월)
    
    // 상환 정보
    val repaymentMethod: String = "",       // 상환 방식 (예: 원리금균등상환, 원금균등상환, 만기일시상환)
    val monthlyInterestPayment: Long = 0,  // 월 이자 납입금액
    val totalPaidAmount: Long = 0,         // 총 납입액
    val nextPaymentDate: LocalDateTime? = null, // 다음 상환일
    
    // 메타 정보
    val status: String = "진행중",      // 상태 (예: 진행중, 완료, 연체)
    val description: String = "",      // 설명
    val memo: String = "",             // 메모
    val originalText: String? = null, // 원본 SMS 텍스트
    val createdAt: LocalDateTime = LocalDateTime.now(), // 생성일시
    val updatedAt: LocalDateTime = LocalDateTime.now() // 업데이트일시
)
