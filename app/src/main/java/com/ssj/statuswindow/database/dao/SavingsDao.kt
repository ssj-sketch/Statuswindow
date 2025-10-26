package com.ssj.statuswindow.database.dao

import androidx.room.*
import com.ssj.statuswindow.database.entity.SavingsEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * 적금 정보 DAO
 */
@Dao
interface SavingsDao {
    
    @Query("SELECT * FROM savings ORDER BY contractDate DESC")
    fun getAllSavings(): Flow<List<SavingsEntity>>
    
    @Query("SELECT * FROM savings WHERE id = :id")
    suspend fun getSavingsById(id: Long): SavingsEntity?
    
    @Query("SELECT * FROM savings WHERE bankName = :bankName ORDER BY contractDate DESC")
    fun getSavingsByBank(bankName: String): Flow<List<SavingsEntity>>
    
    @Query("SELECT * FROM savings WHERE savingsType = :savingsType ORDER BY contractDate DESC")
    fun getSavingsByType(savingsType: String): Flow<List<SavingsEntity>>
    
    @Query("SELECT * FROM savings WHERE status = :status ORDER BY contractDate DESC")
    fun getSavingsByStatus(status: String): Flow<List<SavingsEntity>>
    
    @Query("SELECT * FROM savings WHERE maturityDate <= :date AND status = '진행중'")
    fun getUpcomingMaturitySavings(date: LocalDateTime): Flow<List<SavingsEntity>>
    
    @Query("SELECT * FROM savings WHERE nextDepositDate <= :date AND status = '진행중'")
    fun getUpcomingDepositSavings(date: LocalDateTime): Flow<List<SavingsEntity>>
    
    @Query("SELECT SUM(currentBalance) FROM savings WHERE status = '진행중'")
    suspend fun getTotalSavingsBalance(): Long?
    
    @Query("SELECT SUM(monthlyDeposit) FROM savings WHERE status = '진행중'")
    suspend fun getTotalMonthlyDeposit(): Long?
    
    @Query("SELECT SUM(earnedInterest) FROM savings WHERE status = '진행중'")
    suspend fun getTotalEarnedInterest(): Long?
    
    @Query("SELECT SUM(expectedInterest) FROM savings WHERE status = '진행중'")
    suspend fun getTotalExpectedInterest(): Long?
    
    @Query("SELECT COUNT(*) FROM savings WHERE status = '진행중'")
    suspend fun getActiveSavingsCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavings(savings: SavingsEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsList(savings: List<SavingsEntity>)
    
    @Update
    suspend fun updateSavings(savings: SavingsEntity)
    
    @Delete
    suspend fun deleteSavings(savings: SavingsEntity)
    
    @Query("DELETE FROM savings WHERE id = :id")
    suspend fun deleteSavingsById(id: Long)
    
    @Query("DELETE FROM savings")
    suspend fun deleteAllSavings()
    
    @Query("SELECT COUNT(*) FROM savings")
    suspend fun getSavingsCount(): Int
    
    // 중복 체크
    @Query("SELECT COUNT(*) FROM savings WHERE bankName = :bankName AND accountNumber = :accountNumber AND savingsName = :savingsName")
    suspend fun checkDuplicateSavings(bankName: String, accountNumber: String, savingsName: String): Int
}
