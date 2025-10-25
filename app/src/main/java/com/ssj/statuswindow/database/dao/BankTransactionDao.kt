package com.ssj.statuswindow.database.dao

import androidx.room.*
import com.ssj.statuswindow.database.entity.BankTransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * 입출금내역 DAO
 */
@Dao
interface BankTransactionDao {

    @Query("SELECT * FROM bank_transaction ORDER BY transactionDate DESC")
    fun getAllBankTransactions(): Flow<List<BankTransactionEntity>>

    @Query("SELECT * FROM bank_transaction WHERE id = :id")
    suspend fun getBankTransactionById(id: Long): BankTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankTransaction(bankTransaction: BankTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankTransactionList(bankTransactionList: List<BankTransactionEntity>)

    @Update
    suspend fun updateBankTransaction(bankTransaction: BankTransactionEntity)

    @Delete
    suspend fun deleteBankTransaction(bankTransaction: BankTransactionEntity)

    @Query("DELETE FROM bank_transaction WHERE id = :id")
    suspend fun deleteBankTransactionById(id: Long)

    @Query("DELETE FROM bank_transaction")
    suspend fun deleteAllBankTransactions()

    @Query("SELECT * FROM bank_transaction WHERE bankName = :bankName ORDER BY transactionDate DESC")
    fun getBankTransactionsByBankName(bankName: String): Flow<List<BankTransactionEntity>>

    @Query("SELECT * FROM bank_transaction WHERE transactionType = :transactionType ORDER BY transactionDate DESC")
    fun getBankTransactionsByType(transactionType: String): Flow<List<BankTransactionEntity>>
    
    @Query("SELECT * FROM bank_transaction WHERE transactionDate BETWEEN :startDate AND :endDate ORDER BY transactionDate DESC")
    fun getBankTransactionsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<BankTransactionEntity>>
    
    @Query("SELECT * FROM bank_transaction WHERE transactionDate BETWEEN :startDate AND :endDate ORDER BY transactionDate DESC")
    suspend fun getBankTransactionsByDateRangeList(startDate: LocalDateTime, endDate: LocalDateTime): List<BankTransactionEntity>
    
    @Query("SELECT * FROM bank_transaction WHERE description = :description ORDER BY transactionDate DESC")
    fun getBankTransactionsByDescription(description: String): Flow<List<BankTransactionEntity>>
    
    // 집계 쿼리들
    @Query("SELECT SUM(amount) FROM bank_transaction WHERE transactionType = '입금' AND transactionDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalDepositAmount(startDate: LocalDateTime, endDate: LocalDateTime): Long?
    
    @Query("SELECT SUM(amount) FROM bank_transaction WHERE transactionType = '출금' AND transactionDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalWithdrawalAmount(startDate: LocalDateTime, endDate: LocalDateTime): Long?
    
    @Query("SELECT SUM(CASE WHEN transactionType = '입금' THEN amount ELSE -amount END) FROM bank_transaction WHERE transactionDate BETWEEN :startDate AND :endDate")
    suspend fun getNetAmount(startDate: LocalDateTime, endDate: LocalDateTime): Long?
    
    @Query("SELECT COUNT(*) FROM bank_transaction")
    suspend fun getBankTransactionCount(): Int
    
    @Query("SELECT COUNT(*) FROM bank_transaction WHERE bankName = :bankName")
    suspend fun getBankTransactionCountByBankName(bankName: String): Int
    
    @Query("SELECT COUNT(*) FROM bank_transaction WHERE transactionType = :transactionType")
    suspend fun getBankTransactionCountByType(transactionType: String): Int
    
    // 총 금액 조회 (입금만)
    @Query("SELECT SUM(amount) FROM bank_transaction WHERE transactionType = '입금' AND transactionDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalAmountByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Long?
    
    @Query("SELECT COUNT(*) FROM bank_transaction WHERE transactionDate BETWEEN :startDate AND :endDate")
    suspend fun getTransactionCountByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Int
    
    @Query("SELECT SUM(amount) FROM bank_transaction WHERE transactionType = '입금'")
    suspend fun getTotalAmount(): Long?
    
    @Query("SELECT COUNT(*) FROM bank_transaction WHERE transactionType = '입금'")
    suspend fun getTotalTransactionCount(): Int
    
    // 중복 체크 메서드 추가
    @Query("SELECT COUNT(*) FROM bank_transaction WHERE accountNumber = :accountNumber AND amount = :amount AND transactionDate = :transactionDate AND transactionType = :transactionType")
    suspend fun checkDuplicateBankTransaction(accountNumber: String, amount: Long, transactionDate: LocalDateTime, transactionType: String): Int
}
