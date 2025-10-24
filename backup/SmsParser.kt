package com.ssj.statuswindow.util

import com.ssj.statuswindow.model.CardTransaction
import com.ssj.statuswindow.model.CardEvent
import com.ssj.statuswindow.model.BankBalance
import com.ssj.statuswindow.ai.AiEngineFactory
import com.ssj.statuswindow.ai.CountrySpecificAiEngine
import com.ssj.statuswindow.ai.SalaryInfo
import com.ssj.statuswindow.ai.AutoTransferInfo
import com.ssj.statuswindow.service.DeviceCountryDetectionService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import android.content.Context
import android.util.Log

/**
 * SMS 텍스트를 파싱하여 카드 거래 내역을 추출하는 유틸리티
 * 국가별 AI 엔진을 사용하여 다양한 패턴을 처리
 */
object SmsParser {
    
    private const val TAG = "SmsParser"
    
    // 디바이스 국가 감지 서비스
    private var countryDetectionService: DeviceCountryDetectionService? = null
    
    // 현재 선택된 국가 코드
    private var currentCountryCode: String = "KR" // 기본값: 한국
    
    // 기존 정규식 패턴 (백업용)
    private val SMS_PATTERN = Pattern.compile(
        "([가-힣]+카드)\\((\\d+)\\)(승인|취소)\\s+([가-힣*]+)\\s+(\\d{1,3}(?:,\\d{3})*)원\\((일시불|\\d+개월)\\)(\\d{1,2}/\\d{1,2}\\s*\\d{1,2}:\\d{2})\\s+([가-힣\\s]+)\\s+누적(\\d{1,3}(?:,\\d{3})*)(?:원)?"
    )
    
    // 은행 잔고 SMS 패턴
    private val BANK_BALANCE_PATTERN = Pattern.compile(
        "([가-힣]+)\\s+(\\d{1,2}/\\d{1,2})\\s+(\\d{1,2}:\\d{2})\\s+(\\d{3}-\\*{3}-\\d{6})\\s+(입금|출금)\\s+([가-힣]+)\\s+(\\d{1,3}(?:,\\d{3})*)\\s+잔액\\s+(\\d{1,3}(?:,\\d{3})*)\\s+([가-힣]+)"
    )
    
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd HH:mm")
    
    // 기존 거래 내역을 저장 (중복 검사용)
    private val existingTransactions = mutableListOf<CardTransaction>()
    
    /**
     * SMS 파서 초기화 (Context 필요)
     */
    fun initialize(context: Context) {
        try {
            countryDetectionService = DeviceCountryDetectionService(context)
            
            // 디바이스 정보를 기반으로 국가 자동 감지 (한국으로 강제 설정)
            val detectedCountry = countryDetectionService?.detectCountry() ?: "KR"
            currentCountryCode = "KR" // 한국 SMS이므로 강제로 한국 설정
            
            Log.d(TAG, "SmsParser initialized with country: $currentCountryCode")
            
            // 해당 국가의 AI 엔진 초기화
            val engine = AiEngineFactory.getEngine(currentCountryCode)
            engine?.initialize()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SmsParser", e)
            currentCountryCode = "KR" // 오류 시 기본값 사용
        }
    }
    
    /**
     * 현재 선택된 국가 코드 반환
     */
    fun getCurrentCountryCode(): String {
        return currentCountryCode
    }
    
    /**
     * 국가 코드 설정
     */
    fun setCountryCode(countryCode: String) {
        if (AiEngineFactory.isCountrySupported(countryCode)) {
            currentCountryCode = countryCode
            Log.d(TAG, "Country code changed to: $currentCountryCode")
        } else {
            Log.w(TAG, "Unsupported country code: $countryCode")
        }
    }
    
    /**
     * 지원되는 국가 목록 반환
     */
    fun getSupportedCountries(): Map<String, String> {
        return AiEngineFactory.getSupportedCountries()
    }
    
    /**
     * SMS 텍스트에서 카드 거래 정보를 추출 (국가별 AI 엔진 사용)
     */
    fun parseSmsText(smsText: String, duplicateDetectionMinutes: Int = 5): List<CardTransaction> {
        val transactions = mutableListOf<CardTransaction>()
        val lines = smsText.trim().split("\n")
        
        for (line in lines) {
            if (line.isBlank()) continue
            
            try {
                // 1. 현재 국가의 AI 엔진으로 파싱 시도
                val currentEngine = AiEngineFactory.getEngine(currentCountryCode)
                var transaction: CardTransaction? = null
                
                if (currentEngine != null) {
                    transaction = currentEngine.extractCardTransaction(line)
                }
                
                // 2. 현재 엔진이 실패한 경우 자동 감지된 최적 엔진으로 시도
                if (transaction == null) {
                    val bestEngine = AiEngineFactory.detectBestEngine(line)
                    if (bestEngine != null) {
                        transaction = bestEngine.extractCardTransaction(line)
                        
                        // 다른 국가의 엔진이 더 적합한 경우 현재 국가 업데이트
                        if (transaction != null && bestEngine.getCountryCode() != currentCountryCode) {
                            Log.d(TAG, "Better engine detected: ${bestEngine.getCountryCode()} for SMS: $line")
                            // 필요시 현재 국가 코드 업데이트
                            // setCountryCode(bestEngine.getCountryCode())
                        }
                    }
                }
                
                // 3. AI 엔진이 모두 실패한 경우 기존 정규식으로 백업 시도
                if (transaction == null) {
                    transaction = parseSingleSmsFallback(line)
                }
                
                // 4. 중복 검사 후 추가
                if (transaction != null && !isDuplicateTransaction(transaction, duplicateDetectionMinutes)) {
                    transactions.add(transaction)
                    existingTransactions.add(transaction)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing SMS line: $line", e)
            }
        }
        
        return transactions
    }
    
    /**
     * SMS 텍스트에서 은행 잔고 정보를 추출 (국가별 AI 엔진 사용)
     */
    fun parseBankBalance(smsText: String): BankBalance? {
        try {
            // 1. 현재 국가의 AI 엔진으로 파싱 시도
            val currentEngine = AiEngineFactory.getEngine(currentCountryCode)
            var bankBalance: BankBalance? = null
            
            if (currentEngine != null) {
                bankBalance = currentEngine.extractBankBalance(smsText)
            }
            
            // 2. 현재 엔진이 실패한 경우 자동 감지된 최적 엔진으로 시도
            if (bankBalance == null) {
                val bestEngine = AiEngineFactory.detectBestEngine(smsText)
                if (bestEngine != null) {
                    bankBalance = bestEngine.extractBankBalance(smsText)
                }
            }
            
            // 3. AI 엔진이 모두 실패한 경우 기존 정규식으로 백업 시도
            if (bankBalance == null) {
                bankBalance = parseBankBalanceFallback(smsText)
            }
            
            return bankBalance
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing bank balance from SMS: $smsText", e)
            return null
        }
    }
    
    /**
     * SMS 텍스트에서 급여 정보를 추출 (국가별 AI 엔진 사용)
     */
    fun parseSalaryInfo(smsText: String): SalaryInfo? {
        try {
            // 1. 현재 국가의 AI 엔진으로 파싱 시도
            val currentEngine = AiEngineFactory.getEngine(currentCountryCode)
            var salaryInfo: SalaryInfo? = null
            
            if (currentEngine != null) {
                salaryInfo = currentEngine.extractSalaryInfo(smsText)
            }
            
            // 2. 현재 엔진이 실패한 경우 자동 감지된 최적 엔진으로 시도
            if (salaryInfo == null) {
                val bestEngine = AiEngineFactory.detectBestEngine(smsText)
                if (bestEngine != null) {
                    salaryInfo = bestEngine.extractSalaryInfo(smsText)
                }
            }
            
            return salaryInfo
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing salary info from SMS: $smsText", e)
            return null
        }
    }
    
    /**
     * SMS 텍스트에서 자동이체 정보를 추출 (국가별 AI 엔진 사용)
     */
    fun parseAutoTransferInfo(smsText: String): AutoTransferInfo? {
        try {
            // 1. 현재 국가의 AI 엔진으로 파싱 시도
            val currentEngine = AiEngineFactory.getEngine(currentCountryCode)
            var autoTransferInfo: AutoTransferInfo? = null
            
            if (currentEngine != null) {
                autoTransferInfo = currentEngine.extractAutoTransferInfo(smsText)
            }
            
            // 2. 현재 엔진이 실패한 경우 자동 감지된 최적 엔진으로 시도
            if (autoTransferInfo == null) {
                val bestEngine = AiEngineFactory.detectBestEngine(smsText)
                if (bestEngine != null) {
                    autoTransferInfo = bestEngine.extractAutoTransferInfo(smsText)
                }
            }
            
            return autoTransferInfo
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing auto transfer info from SMS: $smsText", e)
            return null
        }
    }
    
    /**
     * 특정 국가의 AI 엔진을 사용하여 SMS 파싱
     */
    fun parseWithCountry(smsText: String, countryCode: String): List<CardTransaction> {
        val transactions = mutableListOf<CardTransaction>()
        val lines = smsText.trim().split("\n")
        
        val engine = AiEngineFactory.getEngine(countryCode)
        if (engine == null) {
            Log.w(TAG, "Unsupported country code: $countryCode")
            return transactions
        }
        
        for (line in lines) {
            if (line.isBlank()) continue
            
            try {
                val transaction = engine.extractCardTransaction(line)
                if (transaction != null && !isDuplicateTransaction(transaction, 5)) {
                    transactions.add(transaction)
                    existingTransactions.add(transaction)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing SMS with country $countryCode: $line", e)
            }
        }
        
        return transactions
    }
    
    /**
     * 기존 정규식 방식으로 파싱 (백업용)
     */
    private fun parseSingleSmsFallback(smsLine: String): CardTransaction? {
        val matcher = SMS_PATTERN.matcher(smsLine.trim())
        
        if (!matcher.find()) {
            return null
        }
        
        return try {
            val cardType = matcher.group(1) ?: return null
            val cardNumber = matcher.group(2) ?: return null
            val transactionType = matcher.group(3) ?: return null
            val user = matcher.group(4) ?: return null
            val amountStr = matcher.group(5)?.replace(",", "") ?: return null
            val installment = matcher.group(6) ?: return null
            val dateStr = matcher.group(7) ?: return null
            val merchant = matcher.group(8)?.trim() ?: return null
            val cumulativeStr = matcher.group(9)?.replace(",", "") ?: return null
            
            val amount = amountStr.toLong()
            val cumulativeAmount = cumulativeStr.toLong()
            
            val currentYear = LocalDateTime.now().year
            val dateTime = LocalDateTime.parse("$currentYear-$dateStr", 
                DateTimeFormatter.ofPattern("yyyy-MM/dd HH:mm"))
            
            val cleanMerchant = merchant.replace("주식회사\\s*".toRegex(), "").trim()
            
            CardTransaction(
                cardType = cardType,
                cardNumber = cardNumber,
                transactionType = transactionType,
                user = user,
                amount = amount,
                installment = installment,
                transactionDate = dateTime,
                merchant = cleanMerchant,
                cumulativeAmount = cumulativeAmount,
                category = null,
                memo = "",
                originalText = smsLine
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SMS fallback: $smsLine", e)
            null
        }
    }
    
    /**
     * 기존 정규식 방식으로 은행 잔고 파싱 (백업용)
     */
    private fun parseBankBalanceFallback(smsText: String): BankBalance? {
        val matcher = BANK_BALANCE_PATTERN.matcher(smsText)
        
        if (matcher.find()) {
            try {
                val bankName = matcher.group(1) // 신한
                val dateStr = matcher.group(2) // 01/11
                val timeStr = matcher.group(3) // 21:54
                val accountNumber = matcher.group(4) // 100-***-159993
                val transactionType = matcher.group(5) // 입금
                val description = matcher.group(6) // 급여
                val amountStr = matcher.group(7) // 2,500,000
                val balanceStr = matcher.group(8) // 3,265,147
                val memo = matcher.group(9) // 급여
                
                // 금액에서 콤마 제거
                val balance = balanceStr.replace(",", "").toLong()
                
                // 현재 날짜와 시간으로 설정 (실제로는 SMS 수신 시간을 사용해야 함)
                val now = LocalDateTime.now()
                val currentYear = now.year
                
                // 날짜 파싱 (MM/dd 형식)
                val dateParts = dateStr.split("/")
                val month = dateParts[0].toInt()
                val day = dateParts[1].toInt()
                
                // 시간 파싱 (HH:mm 형식)
                val timeParts = timeStr.split(":")
                val hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()
                
                val transactionDateTime = LocalDateTime.of(currentYear, month, day, hour, minute)
                
                return BankBalance(
                    bankName = bankName,
                    accountNumber = accountNumber,
                    balance = balance,
                    accountType = if (transactionType == "입금") "입출금" else "입출금",
                    lastUpdated = transactionDateTime,
                    memo = "$transactionType $description"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing bank balance fallback: $smsText", e)
            }
        }
        
        return null
    }
    
    /**
     * 시간 기반 중복 거래 검사 (동일 시간에 같은 금액, 가맹점이면 중복)
     */
    private fun isDuplicateTransaction(newTransaction: CardTransaction, duplicateDetectionMinutes: Int): Boolean {
        val currentTime = newTransaction.transactionDate
        val timeThreshold = java.time.Duration.ofMinutes(duplicateDetectionMinutes.toLong())
        
        return existingTransactions.any { existing ->
            // 같은 카드, 같은 금액, 같은 가맹점인지 확인
            val isSameTransaction = existing.cardNumber == newTransaction.cardNumber &&
                    existing.amount == newTransaction.amount &&
                    existing.merchant == newTransaction.merchant
            
            if (isSameTransaction) {
                // 시간 차이 확인 (기본 5분 이내)
                val timeDifference = java.time.Duration.between(existing.transactionDate, currentTime)
                timeDifference <= timeThreshold
            } else {
                false
            }
        }
    }
    
    /**
     * 기존 거래 내역 초기화 (테스트용)
     */
    fun clearExistingTransactions() {
        existingTransactions.clear()
    }
    
    /**
     * 기존 호환성을 위한 parse 메서드 (CardEvent 반환)
     */
    fun parse(smsText: String): List<CardEvent> {
        val transactions = parseSmsText(smsText)
        return transactions.map { transaction ->
            CardEvent(
                id = "sms_${transaction.transactionDate.hashCode()}",
                time = transaction.transactionDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                merchant = transaction.merchant,
                amount = transaction.amount,
                sourceApp = "sms_ai_parser",
                raw = transaction.originalText,
                cardBrand = transaction.cardType,
                cardLast4 = transaction.cardNumber,
                cumulativeAmount = transaction.cumulativeAmount
            )
        }
    }
    
    /**
     * SMS 파서 정리
     */
    fun cleanup() {
        try {
            AiEngineFactory.cleanupAllEngines()
            existingTransactions.clear()
            Log.d(TAG, "SmsParser cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up SmsParser", e)
        }
    }
}