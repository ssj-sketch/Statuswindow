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
     * BankTransactionDao 접근자
     */
    fun getBankTransactionDao() = database.bankTransactionDao()
    
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
            
            // 1단계: 원본 SMS 텍스트 중복 체크
            val trimmedSmsText = smsText.trim()
            val existingLog = database.smsProcessingLogDao().getSmsProcessingLogByInputSms(trimmedSmsText)
            if (existingLog != null) {
                android.util.Log.d("SmsDataRepository", "중복 SMS 감지됨 - 처리 건너뛰기: $trimmedSmsText")
                return@withContext SmsSaveResult(
                    isSuccess = true,
                    message = "중복 SMS로 인해 처리 건너뛰기",
                    cardTransactionIds = mutableListOf(),
                    incomeTransactionIds = mutableListOf(),
                    bankBalanceIds = mutableListOf()
                )
            }
            
            // 여러 줄의 SMS를 엔터 단위로 분리
            val smsLines = smsText.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            
            android.util.Log.d("SmsDataRepository", "분리된 SMS 줄 수: ${smsLines.size}")
            
            val result = SmsSaveResult()
            var totalParsedEntities = 0
            
            // 각 줄을 개별적으로 파싱
            smsLines.forEachIndexed { index, line ->
                android.util.Log.d("SmsDataRepository", "SMS 줄 ${index + 1} 처리 중: $line")
                
                try {
                    val parsedEntities = AiBasedSmsParser.parseSmsText(line)
                    android.util.Log.d("SmsDataRepository", "SMS 줄 ${index + 1} 파싱 결과: ${parsedEntities.size}개 엔티티")
                    
                    parsedEntities.forEach { entity ->
                        when (entity) {
                            is com.ssj.statuswindow.database.entity.CardTransactionEntity -> {
                                // 중복 체크
                                val existingCount = database.cardTransactionDao().checkDuplicateCardTransaction(
                                    entity.cardNumber, entity.amount, entity.merchant, entity.transactionDate
                                )
                                
                                if (existingCount == 0) {
                                    // 승인과 취소 거래 모두 저장 (취소는 음수 금액으로 처리)
                                    val id = cardTransactionRepository.insertCardTransaction(entity)
                                    result.cardTransactionIds.add(id)
                                    
                                    if (entity.transactionType == "승인") {
                                        android.util.Log.d("SmsDataRepository", "카드 승인 거래 저장 완료: ID=$id, 금액=${entity.amount}원")
                                    } else {
                                        android.util.Log.d("SmsDataRepository", "카드 취소 거래 저장 완료: ID=$id, 금액=${entity.amount}원 (차감)")
                                    }
                                } else {
                                    android.util.Log.d("SmsDataRepository", "중복 카드 거래 제외: ${entity.cardNumber}_${entity.amount}_${entity.merchant}_${entity.transactionDate}")
                                }
                            }
                            
                            is com.ssj.statuswindow.database.entity.IncomeTransactionEntity -> {
                                // 중복 체크
                                val existingCount = database.incomeTransactionDao().checkDuplicateIncomeTransaction(
                                    entity.accountNumber, entity.amount, entity.transactionDate
                                )
                                
                                if (existingCount == 0) {
                                    val id = incomeTransactionRepository.insertIncomeTransaction(entity)
                                    result.incomeTransactionIds.add(id)
                                    android.util.Log.d("SmsDataRepository", "수입 내역 저장 완료: ID=$id, 금액=${entity.amount}원")
                                    
                                    // BankTransactionEntity도 함께 저장 (중복 체크 포함)
                                    val bankEntity = com.ssj.statuswindow.database.entity.BankTransactionEntity(
                                        bankName = entity.bankName,
                                        accountNumber = entity.accountNumber,
                                        accountType = "입출금",
                                        transactionType = entity.transactionType,
                                        amount = entity.amount,
                                        balance = entity.balance,
                                        description = entity.description,
                                        transactionDate = entity.transactionDate,
                                        memo = "",
                                        originalText = line
                                    )
                                    
                                    val bankExistingCount = database.bankTransactionDao().checkDuplicateBankTransaction(
                                        bankEntity.accountNumber, bankEntity.amount, bankEntity.transactionDate, bankEntity.transactionType
                                    )
                                    
                                    if (bankExistingCount == 0) {
                                        val bankId = database.bankTransactionDao().insertBankTransaction(bankEntity)
                                        android.util.Log.d("SmsDataRepository", "은행 거래 내역 저장 완료: ID=$bankId, 금액=${entity.amount}원")
                                    } else {
                                        android.util.Log.d("SmsDataRepository", "중복 은행 거래 제외: ${bankEntity.accountNumber}_${bankEntity.amount}_${bankEntity.transactionDate}")
                                    }
                                } else {
                                    android.util.Log.d("SmsDataRepository", "중복 수입 내역 제외: ${entity.accountNumber}_${entity.amount}_${entity.transactionDate}")
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
    suspend fun getCardTransactions() = cardTransactionRepository.getAllCardTransactions()
    
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
     * 월별 카드 사용 금액 조회
     */
    suspend fun getMonthlyCardUsageAmount(startDate: java.time.LocalDateTime, endDate: java.time.LocalDateTime): Long {
        return cardTransactionRepository.getTotalAmountByDateRange(startDate, endDate)
    }
    
    /**
     * 모든 데이터 삭제 (테스트용)
     * 조건 없이 전체 테이블을 완전히 삭제합니다.
     * 각 테이블 삭제 후 즉시 확인하여 완전성 검증
     */
    suspend fun clearAllData() {
        try {
            android.util.Log.d("SmsDataRepository", "=== 모든 데이터 삭제 시작 (강화된 버전) ===")
            
            // 1. 카드 거래 테이블 삭제 및 확인
            android.util.Log.d("SmsDataRepository", "1. 카드 거래 테이블 삭제: card_transactions")
            val beforeCardCount = cardTransactionRepository.getCardTransactionCount()
            android.util.Log.d("SmsDataRepository", "   삭제 전 카드거래 수: $beforeCardCount")
            
            cardTransactionRepository.deleteAllCardTransactions()
            kotlinx.coroutines.delay(100) // 삭제 완료 대기
            
            val afterCardCount = cardTransactionRepository.getCardTransactionCount()
            android.util.Log.d("SmsDataRepository", "   삭제 후 카드거래 수: $afterCardCount")
            
            // 2. 소득 거래 테이블 삭제 및 확인
            android.util.Log.d("SmsDataRepository", "2. 소득 거래 테이블 삭제: income_transactions")
            val beforeIncomeCount = incomeTransactionRepository.getIncomeTransactionCount()
            android.util.Log.d("SmsDataRepository", "   삭제 전 소득거래 수: $beforeIncomeCount")
            
            incomeTransactionRepository.deleteAllIncomeTransactions()
            kotlinx.coroutines.delay(100) // 삭제 완료 대기
            
            val afterIncomeCount = incomeTransactionRepository.getIncomeTransactionCount()
            android.util.Log.d("SmsDataRepository", "   삭제 후 소득거래 수: $afterIncomeCount")
            
            // 3. 은행 잔고 테이블 삭제 및 확인
            android.util.Log.d("SmsDataRepository", "3. 은행 잔고 테이블 삭제: bank_balances")
            val beforeBankBalanceCount = bankBalanceRepository.getBankBalanceCount()
            android.util.Log.d("SmsDataRepository", "   삭제 전 은행잔고 수: $beforeBankBalanceCount")
            
            bankBalanceRepository.deleteAllBankBalances()
            kotlinx.coroutines.delay(100) // 삭제 완료 대기
            
            val afterBankBalanceCount = bankBalanceRepository.getBankBalanceCount()
            android.util.Log.d("SmsDataRepository", "   삭제 후 은행잔고 수: $afterBankBalanceCount")
            
            // 4. SMS 처리 로그 테이블 삭제 및 확인
            android.util.Log.d("SmsDataRepository", "4. SMS 처리 로그 테이블 삭제: sms_processing_logs")
            val beforeSmsLogCount = smsProcessingLogRepository.getSmsProcessingLogCount()
            android.util.Log.d("SmsDataRepository", "   삭제 전 SMS로그 수: $beforeSmsLogCount")
            
            smsProcessingLogRepository.deleteAllSmsProcessingLogs()
            kotlinx.coroutines.delay(100) // 삭제 완료 대기
            
            val afterSmsLogCount = smsProcessingLogRepository.getSmsProcessingLogCount()
            android.util.Log.d("SmsDataRepository", "   삭제 후 SMS로그 수: $afterSmsLogCount")
            
            // 5. 은행 거래 테이블 삭제 및 확인 (직접 DAO 호출)
            android.util.Log.d("SmsDataRepository", "5. 은행 거래 테이블 삭제: bank_transaction")
            val beforeBankTransactionCount = database.bankTransactionDao().getBankTransactionCount()
            android.util.Log.d("SmsDataRepository", "   삭제 전 은행거래 수: $beforeBankTransactionCount")
            
            database.bankTransactionDao().deleteAllBankTransactions()
            kotlinx.coroutines.delay(100) // 삭제 완료 대기
            
            val afterBankTransactionCount = database.bankTransactionDao().getBankTransactionCount()
            android.util.Log.d("SmsDataRepository", "   삭제 후 은행거래 수: $afterBankTransactionCount")
            
            // 6. 신용카드 사용 테이블 삭제 및 확인 (직접 DAO 호출)
            android.util.Log.d("SmsDataRepository", "6. 신용카드 사용 테이블 삭제: credit_card_usage")
            val beforeCreditCardCount = database.creditCardUsageDao().getCreditCardUsageCount()
            android.util.Log.d("SmsDataRepository", "   삭제 전 신용카드사용 수: $beforeCreditCardCount")
            
            database.creditCardUsageDao().deleteAllCreditCardUsage()
            kotlinx.coroutines.delay(100) // 삭제 완료 대기
            
            val afterCreditCardCount = database.creditCardUsageDao().getCreditCardUsageCount()
            android.util.Log.d("SmsDataRepository", "   삭제 후 신용카드사용 수: $afterCreditCardCount")
            
            // 최종 확인
            android.util.Log.d("SmsDataRepository", "=== 최종 삭제 결과 확인 ===")
            android.util.Log.d("SmsDataRepository", "카드거래: $afterCardCount (목표: 0)")
            android.util.Log.d("SmsDataRepository", "소득거래: $afterIncomeCount (목표: 0)")
            android.util.Log.d("SmsDataRepository", "은행잔고: $afterBankBalanceCount (목표: 0)")
            android.util.Log.d("SmsDataRepository", "SMS로그: $afterSmsLogCount (목표: 0)")
            android.util.Log.d("SmsDataRepository", "은행거래: $afterBankTransactionCount (목표: 0)")
            android.util.Log.d("SmsDataRepository", "신용카드사용: $afterCreditCardCount (목표: 0)")
            
            val allTablesEmpty = afterCardCount == 0 && afterIncomeCount == 0 && 
                               afterBankBalanceCount == 0 && afterSmsLogCount == 0 &&
                               afterBankTransactionCount == 0 && afterCreditCardCount == 0
            
            if (allTablesEmpty) {
                android.util.Log.d("SmsDataRepository", "✅ 모든 테이블이 완전히 삭제되었습니다.")
            } else {
                android.util.Log.w("SmsDataRepository", "⚠️ 일부 테이블에 데이터가 남아있습니다.")
            }
            
            android.util.Log.d("SmsDataRepository", "=== 모든 데이터 삭제 완료 (강화된 버전) ===")
            
        } catch (e: Exception) {
            android.util.Log.e("SmsDataRepository", "데이터 삭제 중 오류 발생: ${e.message}", e)
            throw e
        }
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
