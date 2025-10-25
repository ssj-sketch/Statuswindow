package com.ssj.statuswindow.database.dao

import androidx.room.*
import com.ssj.statuswindow.database.entity.IncomeTransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * 수입 내역 DAO
 */
@Dao
interface IncomeTransactionDao {
    
    @Query("SELECT * FROM income_transactions ORDER BY transactionDate DESC")
    fun getAllIncomeTransactions(): Flow<List<IncomeTransactionEntity>>
    
    @Query("SELECT * FROM income_transactions WHERE id = :id")
    suspend fun getIncomeTransactionById(id: Long): IncomeTransactionEntity?
    
    @Query("SELECT * FROM income_transactions WHERE bankName = :bankName ORDER BY transactionDate DESC")
    fun getIncomeTransactionsByBank(bankName: String): Flow<List<IncomeTransactionEntity>>
    
    @Query("SELECT * FROM income_transactions WHERE transactionDate BETWEEN :startDate AND :endDate ORDER BY transactionDate DESC")
    fun getIncomeTransactionsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<IncomeTransactionEntity>>
    
    @Query("SELECT * FROM income_transactions WHERE description LIKE '%급여%' ORDER BY transactionDate DESC")
    fun getSalaryTransactions(): Flow<List<IncomeTransactionEntity>>
    
    @Query("SELECT SUM(amount) FROM income_transactions WHERE transactionDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalIncomeByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Long?
    
    @Query("SELECT SUM(amount) FROM income_transactions WHERE description LIKE '%급여%' AND transactionDate BETWEEN :startDate AND :endDate")
    suspend fun getSalaryIncomeByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Long?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeTransaction(incomeTransaction: IncomeTransactionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeTransactions(incomeTransactions: List<IncomeTransactionEntity>)
    
    @Update
    suspend fun updateIncomeTransaction(incomeTransaction: IncomeTransactionEntity)
    
    @Delete
    suspend fun deleteIncomeTransaction(incomeTransaction: IncomeTransactionEntity)
    
    @Query("DELETE FROM income_transactions WHERE id = :id")
    suspend fun deleteIncomeTransactionById(id: Long)
    
    @Query("DELETE FROM income_transactions")
    suspend fun deleteAllIncomeTransactions()

    @Query("SELECT COUNT(*) FROM income_transactions")
    suspend fun getIncomeTransactionCount(): Int
    
    // 중복 체크 메서드 추가
    @Query("SELECT COUNT(*) FROM income_transactions WHERE accountNumber = :accountNumber AND amount = :amount AND transactionDate = :transactionDate")
    suspend fun checkDuplicateIncomeTransaction(accountNumber: String, amount: Long, transactionDate: LocalDateTime): Int
}
