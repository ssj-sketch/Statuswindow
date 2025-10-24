package com.ssj.statuswindow.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * 수입 내역 Room 엔티티
 */
@Entity(tableName = "income_transactions")
data class IncomeTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bankName: String,           // 은행명 (예: 신한)
    val accountNumber: String,      // 계좌번호 (예: 100-***-159993)
    val transactionType: String,    // 거래유형 (예: 입금)
    val description: String,        // 거래설명 (예: 급여)
    val amount: Long,               // 금액 (예: 2500000)
    val balance: Long,              // 잔액 (예: 3265147)
    val transactionDate: LocalDateTime, // 거래일시
    val createdAt: LocalDateTime = LocalDateTime.now() // 생성일시
)
