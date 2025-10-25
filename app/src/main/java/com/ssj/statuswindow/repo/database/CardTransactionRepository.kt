package com.ssj.statuswindow.repo.database

import com.ssj.statuswindow.database.StatusWindowDatabase
import com.ssj.statuswindow.database.entity.CardTransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * 카드 거래 내역 Room 데이터베이스 레포지토리
 */
class CardTransactionRepository(private val database: StatusWindowDatabase) {
    
    private val cardTransactionDao = database.cardTransactionDao()
    
    /**
     * 모든 카드 거래 내역 조회
     */
    suspend fun getAllCardTransactions(): List<CardTransactionEntity> {
        return cardTransactionDao.getAllCardTransactions()
    }
    
    /**
     * 특정 카드 거래 내역 조회
     */
    suspend fun getCardTransactionById(id: Long): CardTransactionEntity? {
        return cardTransactionDao.getCardTransactionById(id)
    }
    
    /**
     * 카드 타입별 거래 내역 조회
     */
    fun getCardTransactionsByCardType(cardType: String): Flow<List<CardTransactionEntity>> {
        return cardTransactionDao.getCardTransactionsByCardType(cardType)
    }
    
    /**
     * 날짜 범위별 거래 내역 조회
     */
    suspend fun getCardTransactionsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): List<CardTransactionEntity> {
        return cardTransactionDao.getCardTransactionsByDateRange(startDate, endDate)
    }
    
    /**
     * 날짜 범위별 총 사용금액 조회
     */
    suspend fun getTotalAmountByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Long {
        return cardTransactionDao.getTotalAmountByDateRange(startDate, endDate) ?: 0L
    }
    
    /**
     * 카드 거래 내역 저장
     */
    suspend fun insertCardTransaction(cardTransaction: CardTransactionEntity): Long {
        return cardTransactionDao.insertCardTransaction(cardTransaction)
    }
    
    /**
     * 카드 거래 내역 여러 건 저장
     */
    suspend fun insertCardTransactions(cardTransactions: List<CardTransactionEntity>) {
        cardTransactionDao.insertCardTransactions(cardTransactions)
    }
    
    /**
     * 카드 거래 내역 수정
     */
    suspend fun updateCardTransaction(cardTransaction: CardTransactionEntity) {
        cardTransactionDao.updateCardTransaction(cardTransaction)
    }
    
    /**
     * 카드 거래 내역 삭제
     */
    suspend fun deleteCardTransaction(cardTransaction: CardTransactionEntity) {
        cardTransactionDao.deleteCardTransaction(cardTransaction)
    }
    
    /**
     * 카드 거래 내역 ID로 삭제
     */
    suspend fun deleteCardTransactionById(id: Long) {
        cardTransactionDao.deleteCardTransactionById(id)
    }
    
    /**
     * 모든 카드 거래 내역 삭제
     */
    suspend fun deleteAllCardTransactions() {
        cardTransactionDao.deleteAllCardTransactions()
    }
    
    suspend fun getCardTransactionCount(): Int {
        return cardTransactionDao.getCardTransactionCount()
    }
}
