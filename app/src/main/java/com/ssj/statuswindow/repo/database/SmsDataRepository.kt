package com.ssj.statuswindow.repo.database

import android.content.Context
import com.google.gson.Gson
import com.ssj.statuswindow.database.StatusWindowDatabase
import com.ssj.statuswindow.database.entity.SmsProcessingLogEntity
import com.ssj.statuswindow.util.AiBasedSmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * SMS 데이터 저장을 위한 통합 레포지토리 (Room 기반)
 */
class SmsDataRepository(private val context: Context) {
    
    private val database = StatusWindowDatabase.getDatabase(context)
    private val cardTransactionRepository = CardTransactionRepository(database)
    private val incomeTransactionRepository = IncomeTransactionRepository(database)
    private val bankBalanceRepository = BankBalanceRepository(database)
    private val smsProcessingLogRepository = SmsProcessingLogRepository(database)
    private val gson = Gson()
    
    /**
     * SMS 텍스트를 파싱하여 적절한 데이터베이스에 저장
     * 여러 줄의 SMS는 엔터 단위로 분리하여 각각 처리
     * 중복 제거 로직 포함
     */
    suspend fun saveSmsData(smsText: String): SmsSaveResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val processingLog = SmsProcessingLogEntity(
            inputSms = smsText,
            processingStatus = "PROCESSING",
            createdAt = LocalDateTime.now()
        )
        
        try {
            android.util.Log.d("SmsDataRepository", "=== SMS 처리 시작 ===")
            android.util.Log.d("SmsDataRepository", "입력 SMS: $smsText")
            
            // 여러 줄의 SMS를 엔터 단위로 분리
            val smsLines = smsText.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            
            android.util.Log.d("SmsDataRepository", "분리된 SMS 줄 수: ${smsLines.size}")
            
            val result = SmsSaveResult()
            var totalParsedEntities = 0
            
            // 중복 제거를 위한 Set
            val processedCardTransactions = mutableSetOf<String>()
            val processedIncomeTransactions = mutableSetOf<String>()
            
            // 각 줄을 개별적으로 파싱
            smsLines.forEachIndexed { index, line ->
                android.util.Log.d("SmsDataRepository", "SMS 줄 ${index + 1} 처리 중: $line")
                
                try {
                    val parsedEntities = AiBasedSmsParser.parseSmsText(line)
                    android.util.Log.d("SmsDataRepository", "SMS 줄 ${index + 1} 파싱 결과: ${parsedEntities.size}개 엔티티")
                    
                    parsedEntities.forEach { entity ->
                        when (entity) {
                            is com.ssj.statuswindow.database.entity.CardTransactionEntity -> {
                                // 중복 체크: 카드번호 + 금액 + 가맹점 + 시간으로 중복 판단
                                val duplicateKey = "${entity.cardNumber}_${entity.amount}_${entity.merchant}_${entity.transactionDate}"
                                
                                if (!processedCardTransactions.contains(duplicateKey)) {
                                    // 취소 거래는 카드 거래로 저장하지 않음
                                    if (entity.transactionType == "승인") {
                                        val id = cardTransactionRepository.insertCardTransaction(entity)
                                        result.cardTransactionIds.add(id)
                                        processedCardTransactions.add(duplicateKey)
                                        android.util.Log.d("SmsDataRepository", "카드 거래 저장 완료: ID=$id, 금액=${entity.amount}원")
                                    } else {
                                        android.util.Log.d("SmsDataRepository", "취소 거래는 저장하지 않음: ${entity.transactionType}")
                                    }
                                } else {
                                    android.util.Log.d("SmsDataRepository", "중복 카드 거래 제외: $duplicateKey")
                                }
                            }
                            
                            is com.ssj.statuswindow.database.entity.IncomeTransactionEntity -> {
                                // 중복 체크: 계좌번호 + 금액 + 시간으로 중복 판단
                                val duplicateKey = "${entity.accountNumber}_${entity.amount}_${entity.transactionDate}"
                                
                                if (!processedIncomeTransactions.contains(duplicateKey)) {
                                    val id = incomeTransactionRepository.insertIncomeTransaction(entity)
                                    result.incomeTransactionIds.add(id)
                                    processedIncomeTransactions.add(duplicateKey)
                                    android.util.Log.d("SmsDataRepository", "수입 내역 저장 완료: ID=$id, 금액=${entity.amount}원")
                                } else {
                                    android.util.Log.d("SmsDataRepository", "중복 수입 내역 제외: $duplicateKey")
                                }
                            }
                            
                            is com.ssj.statuswindow.database.entity.BankBalanceEntity -> {
                                // 은행 잔고는 항상 업데이트 (최신 잔고 유지)
                                val id = bankBalanceRepository.insertOrUpdateBankBalance(entity)
                                result.bankBalanceIds.add(id)
                                android.util.Log.d("SmsDataRepository", "은행 잔고 저장 완료: ID=$id, 잔액=${entity.balance}원")
                            }
                        }
                    }
                    
                    totalParsedEntities += parsedEntities.size
                    
                } catch (e: Exception) {
                    android.util.Log.e("SmsDataRepository", "SMS 줄 ${index + 1} 파싱 오류: ${e.message}", e)
                }
            }
            
            val processingTime = System.currentTimeMillis() - startTime
            val totalSaved = result.cardTransactionIds.size + result.incomeTransactionIds.size + result.bankBalanceIds.size
            
            result.isSuccess = totalSaved > 0
            result.message = if (result.isSuccess) {
                "SMS 데이터 저장 완료: ${smsLines.size}줄 처리, 카드거래 ${result.cardTransactionIds.size}건, 수입내역 ${result.incomeTransactionIds.size}건, 은행잔고 ${result.bankBalanceIds.size}건"
            } else {
                "SMS 파싱은 성공했지만 저장된 데이터가 없습니다. (${smsLines.size}줄 처리됨)"
            }
            
            // 처리 로그 저장
            val finalProcessingLog = processingLog.copy(
                processingStatus = if (result.isSuccess) "SUCCESS" else "PARTIAL",
                parsedEntitiesCount = totalParsedEntities,
                cardTransactionIds = gson.toJson(result.cardTransactionIds),
                incomeTransactionIds = gson.toJson(result.incomeTransactionIds),
                bankBalanceIds = gson.toJson(result.bankBalanceIds),
                processingTimeMs = processingTime
            )
            smsProcessingLogRepository.insertSmsProcessingLog(finalProcessingLog)
            
            android.util.Log.d("SmsDataRepository", result.message)
            android.util.Log.d("SmsDataRepository", "=== SMS 처리 완료 ===")
            
            return@withContext result
            
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            android.util.Log.e("SmsDataRepository", "SMS 처리 오류: ${e.message}", e)
            
            // 실패 로그 저장
            val errorProcessingLog = processingLog.copy(
                processingStatus = "FAILED",
                errorMessage = e.message ?: "알 수 없는 오류",
                processingTimeMs = processingTime
            )
            smsProcessingLogRepository.insertSmsProcessingLog(errorProcessingLog)
            
            return@withContext SmsSaveResult(
                isSuccess = false,
                message = "SMS 데이터 저장 실패: ${e.message}"
            )
        }
    }
    
    /**
     * 카드 거래 내역 저장
     */
    suspend fun saveCardTransaction(cardTransaction: com.ssj.statuswindow.database.entity.CardTransactionEntity): Long {
        return cardTransactionRepository.insertCardTransaction(cardTransaction)
    }
    
    /**
     * 수입 내역 저장
     */
    suspend fun saveIncomeTransaction(incomeTransaction: com.ssj.statuswindow.database.entity.IncomeTransactionEntity): Long {
        return incomeTransactionRepository.insertIncomeTransaction(incomeTransaction)
    }
    
    /**
     * 은행 잔고 저장
     */
    suspend fun saveBankBalance(bankBalance: com.ssj.statuswindow.database.entity.BankBalanceEntity): Long {
        return bankBalanceRepository.insertOrUpdateBankBalance(bankBalance)
    }
    
    /**
     * 카드 거래 내역 조회
     */
    fun getCardTransactions() = cardTransactionRepository.getAllCardTransactions()
    
    /**
     * 수입 내역 조회
     */
    fun getIncomeTransactions() = incomeTransactionRepository.getAllIncomeTransactions()
    
    /**
     * 은행 잔고 조회
     */
    fun getBankBalances() = bankBalanceRepository.getAllBankBalances()
    
    /**
     * 전체 은행 잔고 합계 조회
     */
    suspend fun getTotalBankBalance(): Long {
        return bankBalanceRepository.getTotalBankBalance()
    }
    
    /**
     * 카드 거래 내역 개수 조회
     */
    suspend fun getCardTransactionCount(): Int {
        return cardTransactionRepository.getCardTransactionCount()
    }
    
    /**
     * 수입 내역 개수 조회
     */
    suspend fun getIncomeTransactionCount(): Int {
        return incomeTransactionRepository.getIncomeTransactionCount()
    }
    
    /**
     * 은행 잔고 개수 조회
     */
    suspend fun getBankBalanceCount(): Int {
        return bankBalanceRepository.getBankBalanceCount()
    }
    
    /**
     * 모든 데이터 삭제 (테스트용)
     */
    suspend fun clearAllData() {
        cardTransactionRepository.deleteAllCardTransactions()
        incomeTransactionRepository.deleteAllIncomeTransactions()
        bankBalanceRepository.deleteAllBankBalances()
        smsProcessingLogRepository.deleteAllSmsProcessingLogs()
    }
    
    /**
     * SMS 처리 로그 조회
     */
    fun getSmsProcessingLogs() = smsProcessingLogRepository.getAllSmsProcessingLogs()
    
    /**
     * SMS 처리 로그 개수 조회
     */
    suspend fun getSmsProcessingLogCount(): Int {
        return smsProcessingLogRepository.getSmsProcessingLogCount()
    }
    
    /**
     * 성공한 SMS 처리 로그 개수 조회
     */
    suspend fun getSuccessfulSmsProcessingLogCount(): Int {
        return smsProcessingLogRepository.getSmsProcessingLogCountByStatus("SUCCESS")
    }
    
    /**
     * 실패한 SMS 처리 로그 개수 조회
     */
    suspend fun getFailedSmsProcessingLogCount(): Int {
        return smsProcessingLogRepository.getSmsProcessingLogCountByStatus("FAILED")
    }
}

/**
 * SMS 데이터 저장 결과
 */
data class SmsSaveResult(
    var isSuccess: Boolean = false,
    var message: String = "",
    val cardTransactionIds: MutableList<Long> = mutableListOf(),
    val incomeTransactionIds: MutableList<Long> = mutableListOf(),
    val bankBalanceIds: MutableList<Long> = mutableListOf()
)
