// 경로: C:/app/Statuswindow/app/src/main/java/com/ssj/statuswindow/repo/CardEventRepository.kt
package com.ssj.statuswindow.repo

import android.content.Context
import com.ssj.statuswindow.model.CardEvent
import com.ssj.statuswindow.model.CardTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CardEventRepository private constructor(context: Context) {

    // 여기에 데이터베이스 인스턴스나 DAO 객체를 초기화하는 코드를 넣을 수 있습니다.
    // 예: private val cardEventDao = AppDatabase.getDatabase(context).cardEventDao()

    private val _events = MutableStateFlow<List<CardEvent>>(emptyList())
    val events: StateFlow<List<CardEvent>> = _events.asStateFlow()
    
    // CardTransaction 저장소
    private val _transactions = MutableStateFlow<List<CardTransaction>>(emptyList())
    val transactions: StateFlow<List<CardTransaction>> = _transactions.asStateFlow()

    /**
     * 단일 카드 이벤트를 추가합니다.
     */
    fun add(event: CardEvent) {
        val currentEvents = _events.value.toMutableList()
        currentEvents.add(event)
        _events.value = currentEvents
    }

    /**
     * 여러 개의 카드 이벤트 리스트를 데이터베이스에 추가합니다.
     * (내부 구현은 프로젝트의 데이터베이스 구조에 맞게 작성해야 합니다.)
     */
    fun addAll(events: List<CardEvent>) {
        val currentEvents = _events.value.toMutableList()
        currentEvents.addAll(events)
        _events.value = currentEvents
        println("Adding ${events.size} card events to the repository.") // 임시 구현
    }
    
    /**
     * CardTransaction을 추가합니다.
     */
    fun addTransaction(transaction: CardTransaction) {
        val currentTransactions = _transactions.value.toMutableList()
        currentTransactions.add(transaction)
        _transactions.value = currentTransactions
    }
    
    /**
     * 여러 CardTransaction을 추가합니다.
     */
    fun addAllTransactions(transactions: List<CardTransaction>) {
        val currentTransactions = _transactions.value.toMutableList()
        currentTransactions.addAll(transactions)
        _transactions.value = currentTransactions
    }
    
    /**
     * 모든 CardTransaction을 가져옵니다.
     */
    fun getAllTransactions(): List<CardTransaction> {
        return _transactions.value
    }
    
    /**
     * CardTransaction의 메모를 업데이트합니다.
     */
    fun updateTransactionMemo(transaction: CardTransaction, newMemo: String) {
        val currentTransactions = _transactions.value.toMutableList()
        val index = currentTransactions.indexOfFirst { 
            it.cardNumber == transaction.cardNumber && 
            it.amount == transaction.amount && 
            it.transactionDate == transaction.transactionDate && 
            it.merchant == transaction.merchant 
        }
        
        if (index != -1) {
            val updatedTransaction = currentTransactions[index].copy(memo = newMemo)
            currentTransactions[index] = updatedTransaction
            _transactions.value = currentTransactions
        }
    }
    
    /**
     * CardTransaction을 삭제합니다.
     */
    fun removeTransaction(transaction: CardTransaction) {
        val currentTransactions = _transactions.value.toMutableList()
        currentTransactions.removeAll { 
            it.cardNumber == transaction.cardNumber && 
            it.amount == transaction.amount && 
            it.transactionDate == transaction.transactionDate && 
            it.merchant == transaction.merchant 
        }
        _transactions.value = currentTransactions
    }

    companion object {
        // @Volatile: 이 변수에 대한 변경 사항이 모든 스레드에 즉시 보이도록 합니다.
        @Volatile
        private var INSTANCE: CardEventRepository? = null

        /**
         * CardEventRepository의 싱글턴 인스턴스를 반환합니다.
         * 인스턴스가 없으면 새로 생성하고, 있으면 기존 인스턴스를 반환합니다.
         */
        fun instance(context: Context): CardEventRepository {
            // 엘비스 연산자(?:)를 사용하여 인스턴스가 null일 경우에만 synchronized 블록을 실행합니다.
            return INSTANCE ?: synchronized(this) {
                val instance = CardEventRepository(context.applicationContext)
                INSTANCE = instance
                // 반환
                instance
            }
        }
    }
}
