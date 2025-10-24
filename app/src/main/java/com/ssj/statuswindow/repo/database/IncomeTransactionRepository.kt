package com.ssj.statuswindow.repo.database

import com.ssj.statuswindow.database.StatusWindowDatabase
import com.ssj.statuswindow.database.entity.IncomeTransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * 수입 내역 Room 데이터베이스 레포지토리
 */
class IncomeTransactionRepository(private val database: StatusWindowDatabase) {
    
    private val incomeTransactionDao = database.incomeTransactionDao()
    
    /**
     * 모든 수입 내역 조회
     */
    fun getAllIncomeTransactions(): Flow<List<IncomeTransactionEntity>> {
        return incomeTransactionDao.getAllIncomeTransactions()
    }
    
    /**
     * 특정 수입 내역 조회
     */
    suspend fun getIncomeTransactionById(id: Long): IncomeTransactionEntity? {
        return incomeTransactionDao.getIncomeTransactionById(id)
    }
    
    /**
     * 은행별 수입 내역 조회
     */
    fun getIncomeTransactionsByBank(bankName: String): Flow<List<IncomeTransactionEntity>> {
        return incomeTransactionDao.getIncomeTransactionsByBank(bankName)
    }
    
    /**
     * 날짜 범위별 수입 내역 조회
     */
    fun getIncomeTransactionsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<IncomeTransactionEntity>> {
        return incomeTransactionDao.getIncomeTransactionsByDateRange(startDate, endDate)
    }
    
    /**
     * 급여 내역만 조회
     */
    fun getSalaryTransactions(): Flow<List<IncomeTransactionEntity>> {
        return incomeTransactionDao.getSalaryTransactions()
    }
    
    /**
     * 날짜 범위별 총 수입 조회
     */
    suspend fun getTotalIncomeByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Long {
        return incomeTransactionDao.getTotalIncomeByDateRange(startDate, endDate) ?: 0L
    }
    
    /**
     * 날짜 범위별 급여 수입 조회
     */
    suspend fun getSalaryIncomeByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Long {
        return incomeTransactionDao.getSalaryIncomeByDateRange(startDate, endDate) ?: 0L
    }
    
    /**
     * 수입 내역 저장
     */
    suspend fun insertIncomeTransaction(incomeTransaction: IncomeTransactionEntity): Long {
        return incomeTransactionDao.insertIncomeTransaction(incomeTransaction)
    }
    
    /**
     * 수입 내역 여러 건 저장
     */
    suspend fun insertIncomeTransactions(incomeTransactions: List<IncomeTransactionEntity>) {
        incomeTransactionDao.insertIncomeTransactions(incomeTransactions)
    }
    
    /**
     * 수입 내역 수정
     */
    suspend fun updateIncomeTransaction(incomeTransaction: IncomeTransactionEntity) {
        incomeTransactionDao.updateIncomeTransaction(incomeTransaction)
    }
    
    /**
     * 수입 내역 삭제
     */
    suspend fun deleteIncomeTransaction(incomeTransaction: IncomeTransactionEntity) {
        incomeTransactionDao.deleteIncomeTransaction(incomeTransaction)
    }
    
    /**
     * 수입 내역 ID로 삭제
     */
    suspend fun deleteIncomeTransactionById(id: Long) {
        incomeTransactionDao.deleteIncomeTransactionById(id)
    }
    
    /**
     * 모든 수입 내역 삭제
     */
    suspend fun deleteAllIncomeTransactions() {
        incomeTransactionDao.deleteAllIncomeTransactions()
    }
    
    suspend fun getIncomeTransactionCount(): Int {
        return incomeTransactionDao.getIncomeTransactionCount()
    }
}
