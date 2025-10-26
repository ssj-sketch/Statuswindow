package com.ssj.statuswindow.database.dao

import androidx.room.*
import com.ssj.statuswindow.database.entity.LoanEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * 대출 정보 DAO
 */
@Dao
interface LoanDao {
    
    @Query("SELECT * FROM loans ORDER BY contractDate DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>
    
    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoanById(id: Long): LoanEntity?
    
    @Query("SELECT * FROM loans WHERE bankName = :bankName ORDER BY contractDate DESC")
    fun getLoansByBank(bankName: String): Flow<List<LoanEntity>>
    
    @Query("SELECT * FROM loans WHERE loanType = :loanType ORDER BY contractDate DESC")
    fun getLoansByType(loanType: String): Flow<List<LoanEntity>>
    
    @Query("SELECT * FROM loans WHERE status = :status ORDER BY contractDate DESC")
    fun getLoansByStatus(status: String): Flow<List<LoanEntity>>
    
    @Query("SELECT * FROM loans WHERE maturityDate <= :date AND status = '진행중'")
    fun getUpcomingMaturityLoans(date: LocalDateTime): Flow<List<LoanEntity>>
    
    @Query("SELECT * FROM loans WHERE nextPaymentDate <= :date AND status = '진행중'")
    fun getUpcomingPaymentLoans(date: LocalDateTime): Flow<List<LoanEntity>>
    
    @Query("SELECT SUM(remainingPrincipal) FROM loans WHERE status = '진행중'")
    suspend fun getTotalRemainingPrincipal(): Long?
    
    @Query("SELECT SUM(monthlyPayment) FROM loans WHERE status = '진행중'")
    suspend fun getTotalMonthlyPayment(): Long?
    
    @Query("SELECT SUM(monthlyInterestPayment) FROM loans WHERE status = '진행중'")
    suspend fun getTotalMonthlyInterest(): Long?
    
    @Query("SELECT COUNT(*) FROM loans WHERE status = '진행중'")
    suspend fun getActiveLoanCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoans(loans: List<LoanEntity>)
    
    @Update
    suspend fun updateLoan(loan: LoanEntity)
    
    /**
     * 계좌번호 + 은행명 + 납입월로 대출 조회
     */
    @Query("SELECT * FROM loans WHERE accountNumber = :accountNumber AND bankName = :bankName AND paymentMonth = :paymentMonth")
    suspend fun getLoanByUniqueKey(accountNumber: String, bankName: String, paymentMonth: String): LoanEntity?
    
    /**
     * 계좌번호 + 은행명 + 대출명 + 납입월로 대출 조회
     */
    @Query("SELECT * FROM loans WHERE accountNumber = :accountNumber AND bankName = :bankName AND loanName = :loanName AND paymentMonth = :paymentMonth")
    suspend fun getLoanByFullKey(accountNumber: String, bankName: String, loanName: String, paymentMonth: String): LoanEntity?
    
    /**
     * 계좌번호 + 은행명 + 대출명으로 대출 정보 조회 (기본 정보, paymentMonth가 빈 문자열)
     */
    @Query("SELECT * FROM loans WHERE accountNumber = :accountNumber AND bankName = :bankName AND loanName = :loanName AND paymentMonth = ''")
    suspend fun getLoanBasicInfo(accountNumber: String, bankName: String, loanName: String): LoanEntity?
    
    @Delete
    suspend fun deleteLoan(loan: LoanEntity)
    
    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteLoanById(id: Long)
    
    @Query("DELETE FROM loans")
    suspend fun deleteAllLoans()
    
    @Query("SELECT COUNT(*) FROM loans")
    suspend fun getLoanCount(): Int
    
    // 중복 체크
    @Query("SELECT COUNT(*) FROM loans WHERE bankName = :bankName AND accountNumber = :accountNumber AND loanName = :loanName")
    suspend fun checkDuplicateLoan(bankName: String, accountNumber: String, loanName: String): Int
    
    // 계좌번호 + 은행명 + 납입월로 중복 체크
    @Query("SELECT COUNT(*) FROM loans WHERE accountNumber = :accountNumber AND bankName = :bankName AND paymentMonth = :paymentMonth")
    suspend fun checkDuplicateByPaymentMonth(accountNumber: String, bankName: String, paymentMonth: String): Int
    
    // 계좌번호 + 은행명 + 대출명 + 납입월로 중복 체크
    @Query("SELECT COUNT(*) FROM loans WHERE accountNumber = :accountNumber AND bankName = :bankName AND loanName = :loanName AND paymentMonth = :paymentMonth")
    suspend fun checkDuplicateByFullKey(accountNumber: String, bankName: String, loanName: String, paymentMonth: String): Int
    
    /**
     * UPSERT: 중복이면 업데이트, 없으면 삽입
     * REPLACE 전략으로 자동 중복 처리
     */
    suspend fun upsertLoan(loan: LoanEntity): Long {
        return insertLoan(loan)
    }
}
