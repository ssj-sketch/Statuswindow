package com.ssj.statuswindow.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * 은행 잔고 Room 엔티티 (자산내역 > 은행장고)
 */
@Entity(tableName = "bank_balances")
data class BankBalanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bankName: String,           // 은행명 (예: 신한)
    val accountNumber: String,      // 계좌번호 (예: 100-***-159993)
    val balance: Long,              // 잔액 (예: 3265147)
    val lastTransactionDate: LocalDateTime, // 마지막 거래일시
    val createdAt: LocalDateTime = LocalDateTime.now(), // 생성일시
    val updatedAt: LocalDateTime = LocalDateTime.now() // 업데이트일시
)
