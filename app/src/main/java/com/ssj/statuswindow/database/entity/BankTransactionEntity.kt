package com.ssj.statuswindow.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * 입출금내역 Room 엔티티
 * 은행 계좌 입출금 거래를 저장
 */
@Entity(tableName = "bank_transaction")
data class BankTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 은행 정보
    val bankName: String,           // 은행명 (예: 신한)
    val accountNumber: String,      // 계좌번호 (예: 100-***-159993)
    val accountType: String,       // 계좌유형 (예: 입출금)
    
    // 거래 정보
    val transactionType: String,    // 거래구분 (예: 입금, 출금)
    val amount: Long,              // 거래금액 (예: 2500000)
    val balance: Long,             // 잔액 (예: 3700000)
    val description: String,       // 거래내용 (예: 급여, 신한카드)
    
    // 거래 상세
    val transactionDate: LocalDateTime, // 거래일시
    val memo: String,              // 메모
    val originalText: String,      // 원본 SMS 텍스트
    
    // 메타 정보
    val createdAt: LocalDateTime = LocalDateTime.now(), // 생성일시
    val updatedAt: LocalDateTime = LocalDateTime.now()  // 수정일시
)
