package com.ssj.statuswindow.repo.database

import com.ssj.statuswindow.database.StatusWindowDatabase
import com.ssj.statuswindow.database.entity.BankBalanceEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * 은행 잔고 Room 데이터베이스 레포지토리
 */
class BankBalanceRepository(private val database: StatusWindowDatabase) {
    
    private val bankBalanceDao = database.bankBalanceDao()
    
    /**
     * 모든 은행 잔고 조회
     */
    fun getAllBankBalances(): Flow<List<BankBalanceEntity>> {
        return bankBalanceDao.getAllBankBalances()
    }
    
    /**
     * 특정 은행 잔고 조회
     */
    suspend fun getBankBalanceById(id: Long): BankBalanceEntity? {
        return bankBalanceDao.getBankBalanceById(id)
    }
    
    /**
     * 은행명으로 잔고 조회
     */
    suspend fun getBankBalanceByBankName(bankName: String): BankBalanceEntity? {
        return bankBalanceDao.getBankBalanceByBankName(bankName)
    }
    
    /**
     * 은행명과 계좌번호로 잔고 조회
     */
    suspend fun getBankBalanceByAccount(bankName: String, accountNumber: String): BankBalanceEntity? {
        return bankBalanceDao.getBankBalanceByBankAndAccount(bankName, accountNumber)
    }
    
    /**
     * 전체 은행 잔고 합계 조회
     */
    suspend fun getTotalBankBalance(): Long {
        return bankBalanceDao.getTotalBankBalance() ?: 0L
    }
    
    /**
     * 은행 잔고 저장 (기존 데이터가 있으면 업데이트)
     */
    suspend fun insertOrUpdateBankBalance(bankBalance: BankBalanceEntity): Long {
        val existing = getBankBalanceByAccount(bankBalance.bankName, bankBalance.accountNumber)
        
        return if (existing != null) {
            // 기존 데이터 업데이트
            val updatedBalance = bankBalance.copy(
                id = existing.id,
                updatedAt = LocalDateTime.now()
            )
            bankBalanceDao.updateBankBalance(updatedBalance)
            existing.id
        } else {
            // 새 데이터 삽입
            bankBalanceDao.insertBankBalance(bankBalance)
        }
    }
    
    /**
     * 은행 잔고 저장
     */
    suspend fun insertBankBalance(bankBalance: BankBalanceEntity): Long {
        return bankBalanceDao.insertBankBalance(bankBalance)
    }
    
    /**
     * 은행 잔고 여러 건 저장
     */
    suspend fun insertBankBalances(bankBalances: List<BankBalanceEntity>) {
        bankBalanceDao.insertBankBalances(bankBalances)
    }
    
    /**
     * 은행 잔고 수정
     */
    suspend fun updateBankBalance(bankBalance: BankBalanceEntity) {
        bankBalanceDao.updateBankBalance(bankBalance)
    }
    
    /**
     * 은행 잔고 삭제
     */
    suspend fun deleteBankBalance(bankBalance: BankBalanceEntity) {
        bankBalanceDao.deleteBankBalance(bankBalance)
    }
    
    /**
     * 은행 잔고 ID로 삭제
     */
    suspend fun deleteBankBalanceById(id: Long) {
        bankBalanceDao.deleteBankBalanceById(id)
    }
    
    /**
     * 모든 은행 잔고 삭제
     */
    suspend fun deleteAllBankBalances() {
        bankBalanceDao.deleteAllBankBalances()
    }
    
    suspend fun getBankBalanceCount(): Int {
        return bankBalanceDao.getBankBalanceCount()
    }
}
