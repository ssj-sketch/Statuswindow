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
}
