package com.ssj.statuswindow.database.dao

import androidx.room.*
import com.ssj.statuswindow.database.entity.CardTransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * 카드 거래 내역 DAO
 */
@Dao
interface CardTransactionDao {
    
    @Query("SELECT * FROM card_transactions ORDER BY transactionDate DESC")
    fun getAllCardTransactions(): Flow<List<CardTransactionEntity>>
    
    @Query("SELECT * FROM card_transactions WHERE id = :id")
    suspend fun getCardTransactionById(id: Long): CardTransactionEntity?
    
    @Query("SELECT * FROM card_transactions WHERE cardType = :cardType ORDER BY transactionDate DESC")
    fun getCardTransactionsByCardType(cardType: String): Flow<List<CardTransactionEntity>>
    
    @Query("SELECT * FROM card_transactions WHERE transactionDate BETWEEN :startDate AND :endDate ORDER BY transactionDate DESC")
    fun getCardTransactionsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<CardTransactionEntity>>
    
    @Query("SELECT SUM(amount) FROM card_transactions WHERE transactionDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalAmountByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Long?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardTransaction(cardTransaction: CardTransactionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardTransactions(cardTransactions: List<CardTransactionEntity>)
    
    @Update
    suspend fun updateCardTransaction(cardTransaction: CardTransactionEntity)
    
    @Delete
    suspend fun deleteCardTransaction(cardTransaction: CardTransactionEntity)
    
    @Query("DELETE FROM card_transactions WHERE id = :id")
    suspend fun deleteCardTransactionById(id: Long)
    
    @Query("DELETE FROM card_transactions")
    suspend fun deleteAllCardTransactions()

    @Query("SELECT COUNT(*) FROM card_transactions")
    suspend fun getCardTransactionCount(): Int
    
    /**
     * 이번달 청구금액 계산 (일시불 + 할부 첫달)
     * 취소는 음수로, 할부는 첫달만 계산
     */
    @Query("""
        SELECT SUM(
            CASE 
                WHEN transactionType = '취소' THEN -amount
                WHEN installment = '일시불' THEN amount
                WHEN installment LIKE '%개월' THEN 
                    amount / CAST(REPLACE(installment, '개월', '') AS INTEGER)
                ELSE amount
            END
        ) FROM card_transactions 
        WHERE transactionDate BETWEEN :startDate AND :endDate
    """)
    suspend fun getMonthlyBillAmount(startDate: LocalDateTime, endDate: LocalDateTime): Long?
    
    /**
     * 카드사용 총액 계산 (승인/취소 구분)
     */
    @Query("""
        SELECT SUM(
            CASE 
                WHEN transactionType = '취소' THEN -amount
                ELSE amount
            END
        ) FROM card_transactions 
        WHERE transactionDate BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalCardUsageAmount(startDate: LocalDateTime, endDate: LocalDateTime): Long?
}
