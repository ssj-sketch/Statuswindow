package com.ssj.statuswindow.database.dao

import androidx.room.*
import com.ssj.statuswindow.database.entity.BankBalanceEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * 은행 잔고 DAO
 */
@Dao
interface BankBalanceDao {
    
    @Query("SELECT * FROM bank_balances ORDER BY lastTransactionDate DESC")
    fun getAllBankBalances(): Flow<List<BankBalanceEntity>>
    
    @Query("SELECT * FROM bank_balances WHERE id = :id")
    suspend fun getBankBalanceById(id: Long): BankBalanceEntity?
    
    @Query("SELECT * FROM bank_balances WHERE bankName = :bankName")
    suspend fun getBankBalanceByBankName(bankName: String): BankBalanceEntity?
    
    @Query("SELECT * FROM bank_balances WHERE bankName = :bankName AND accountNumber = :accountNumber")
    suspend fun getBankBalanceByBankAndAccount(bankName: String, accountNumber: String): BankBalanceEntity?
    
    @Query("SELECT SUM(balance) FROM bank_balances")
    suspend fun getTotalBankBalance(): Long?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankBalance(bankBalance: BankBalanceEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankBalances(bankBalances: List<BankBalanceEntity>)
    
    @Update
    suspend fun updateBankBalance(bankBalance: BankBalanceEntity)
    
    @Delete
    suspend fun deleteBankBalance(bankBalance: BankBalanceEntity)
    
    @Query("DELETE FROM bank_balances WHERE id = :id")
    suspend fun deleteBankBalanceById(id: Long)
    
    @Query("DELETE FROM bank_balances")
    suspend fun deleteAllBankBalances()
    
    @Query("SELECT COUNT(*) FROM bank_balances")
    suspend fun getBankBalanceCount(): Int
}
