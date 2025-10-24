package com.ssj.statuswindow.database.dao

import androidx.room.*
import com.ssj.statuswindow.database.entity.CreditCardUsageEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * 신용카드 사용내역 DAO
 */
@Dao
interface CreditCardUsageDao {
    
    @Query("SELECT * FROM credit_card_usage ORDER BY transactionDate DESC")
    fun getAllCreditCardUsage(): Flow<List<CreditCardUsageEntity>>
    
    @Query("SELECT * FROM credit_card_usage WHERE id = :id")
    suspend fun getCreditCardUsageById(id: Long): CreditCardUsageEntity?
    
    @Query("SELECT * FROM credit_card_usage WHERE cardType = :cardType ORDER BY transactionDate DESC")
    fun getCreditCardUsageByCardType(cardType: String): Flow<List<CreditCardUsageEntity>>
    
    @Query("SELECT * FROM credit_card_usage WHERE cardNumber = :cardNumber ORDER BY transactionDate DESC")
    fun getCreditCardUsageByCardNumber(cardNumber: String): Flow<List<CreditCardUsageEntity>>
    
    @Query("SELECT * FROM credit_card_usage WHERE transactionDate BETWEEN :startDate AND :endDate ORDER BY transactionDate DESC")
    fun getCreditCardUsageByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<CreditCardUsageEntity>>
    
    @Query("SELECT * FROM credit_card_usage WHERE merchant = :merchant ORDER BY transactionDate DESC")
    fun getCreditCardUsageByMerchant(merchant: String): Flow<List<CreditCardUsageEntity>>
    
    // 청구월별 조회
    @Query("SELECT * FROM credit_card_usage WHERE billingYear = :year AND billingMonth = :month ORDER BY transactionDate DESC")
    fun getCreditCardUsageByBillingMonth(year: Int, month: Int): Flow<List<CreditCardUsageEntity>>
    
    @Query("SELECT * FROM credit_card_usage WHERE billingYear = :year ORDER BY billingMonth DESC, transactionDate DESC")
    fun getCreditCardUsageByBillingYear(year: Int): Flow<List<CreditCardUsageEntity>>
    
    // 카테고리별 조회
    @Query("SELECT * FROM credit_card_usage WHERE merchantCategory = :category ORDER BY transactionDate DESC")
    fun getCreditCardUsageByCategory(category: String): Flow<List<CreditCardUsageEntity>>
    
    @Query("SELECT * FROM credit_card_usage WHERE billingYear = :year AND billingMonth = :month AND merchantCategory = :category ORDER BY transactionDate DESC")
    fun getCreditCardUsageByBillingMonthAndCategory(year: Int, month: Int, category: String): Flow<List<CreditCardUsageEntity>>
    
    // 집계 쿼리들
    @Query("SELECT SUM(amount) FROM credit_card_usage WHERE transactionDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalAmountByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Long?
    
    @Query("SELECT SUM(monthlyPayment) FROM credit_card_usage WHERE transactionDate BETWEEN :startDate AND :endDate")
    suspend fun getMonthlyBillAmountByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Long?
    
    // 청구월별 집계
    @Query("SELECT SUM(billingAmount) FROM credit_card_usage WHERE billingYear = :year AND billingMonth = :month")
    suspend fun getBillingAmountByMonth(year: Int, month: Int): Long?
    
    @Query("SELECT SUM(amount) FROM credit_card_usage WHERE billingYear = :year AND billingMonth = :month")
    suspend fun getTotalAmountByBillingMonth(year: Int, month: Int): Long?
    
    // 카테고리별 집계
    @Query("SELECT SUM(amount) FROM credit_card_usage WHERE merchantCategory = :category")
    suspend fun getTotalAmountByCategory(category: String): Long?
    
    @Query("SELECT SUM(billingAmount) FROM credit_card_usage WHERE billingYear = :year AND billingMonth = :month AND merchantCategory = :category")
    suspend fun getBillingAmountByMonthAndCategory(year: Int, month: Int, category: String): Long?
    
    @Query("SELECT COUNT(*) FROM credit_card_usage")
    suspend fun getCreditCardUsageCount(): Int
    
    @Query("SELECT COUNT(*) FROM credit_card_usage WHERE cardType = :cardType")
    suspend fun getCreditCardUsageCountByCardType(cardType: String): Int
    
    // 카드별 통계
    @Query("""
        SELECT cardType, cardNumber, COUNT(*) as usageCount, 
               SUM(amount) as totalAmount, 
               SUM(monthlyPayment) as totalMonthlyPayment
        FROM credit_card_usage 
        WHERE transactionDate BETWEEN :startDate AND :endDate
        GROUP BY cardType, cardNumber
        ORDER BY totalAmount DESC
    """)
    suspend fun getCardUsageStatistics(startDate: LocalDateTime, endDate: LocalDateTime): List<CardUsageStatistics>
    
    // 가맹점별 통계
    @Query("""
        SELECT merchant, COUNT(*) as usageCount, 
               SUM(amount) as totalAmount
        FROM credit_card_usage 
        WHERE transactionDate BETWEEN :startDate AND :endDate
        GROUP BY merchant
        ORDER BY totalAmount DESC
        LIMIT 10
    """)
    suspend fun getMerchantUsageStatistics(startDate: LocalDateTime, endDate: LocalDateTime): List<MerchantUsageStatistics>
    
    // CRUD 작업
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditCardUsage(creditCardUsage: CreditCardUsageEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditCardUsageList(creditCardUsageList: List<CreditCardUsageEntity>)
    
    @Update
    suspend fun updateCreditCardUsage(creditCardUsage: CreditCardUsageEntity)
    
    @Delete
    suspend fun deleteCreditCardUsage(creditCardUsage: CreditCardUsageEntity)
    
    @Query("DELETE FROM credit_card_usage WHERE id = :id")
    suspend fun deleteCreditCardUsageById(id: Long)
    
    @Query("DELETE FROM credit_card_usage")
    suspend fun deleteAllCreditCardUsage()
}

/**
 * 카드 사용 통계 데이터 클래스
 */
data class CardUsageStatistics(
    val cardType: String,
    val cardNumber: String,
    val usageCount: Int,
    val totalAmount: Long,
    val totalMonthlyPayment: Long
)

/**
 * 가맹점 사용 통계 데이터 클래스
 */
data class MerchantUsageStatistics(
    val merchant: String,
    val usageCount: Int,
    val totalAmount: Long
)
