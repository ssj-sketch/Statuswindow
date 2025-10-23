package com.ssj.statuswindow.util

import com.ssj.statuswindow.model.SalaryInfo
import com.ssj.statuswindow.model.AutoTransferInfo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

/**
 * 급여 및 자동이체 감지 AI 엔진
 */
object FinancialNotificationAnalyzer {
    
    // 급여 관련 키워드 패턴
    private val SALARY_PATTERNS = listOf(
        "급여",
        "월급",
        "월급여",
        "임금",
        "보수",
        "월보수",
        "월임금",
        "월급여",
        "급여지급",
        "월급지급",
        "임금지급",
        "보수지급"
    )
    
    // 자동이체 관련 키워드 패턴
    private val AUTO_TRANSFER_PATTERNS = listOf(
        "자동이체",
        "자동출금",
        "자동납부",
        "정기이체",
        "정기출금",
        "정기납부",
        "자동결제",
        "정기결제"
    )
    
    // 금액 패턴 (숫자와 콤마, 원 포함)
    private val AMOUNT_PATTERN = Pattern.compile("([0-9,]+)\\s*원?")
    
    // 계좌번호 패턴 (숫자 4자리 이상)
    private val ACCOUNT_PATTERN = Pattern.compile("([0-9]{4,})")
    
    // 날짜 패턴
    private val DATE_PATTERN = Pattern.compile("(\\d{4})[.-/](\\d{1,2})[.-/](\\d{1,2})")
    private val KOREAN_DATE_PATTERN = Pattern.compile("(\\d{1,2})/(\\d{1,2})")
    private val TIME_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{2})")
    
    /**
     * 알림 텍스트에서 급여 정보 추출 (품질 개선)
     */
    fun extractSalaryInfo(notificationText: String, appName: String): SalaryInfo? {
        val text = notificationText.lowercase()
        
        // 디버그 로그 추가 (품질 개선)
        android.util.Log.d("FinancialNotificationAnalyzer", "급여 정보 추출 시도: $notificationText")
        
        // 급여 관련 키워드가 있는지 확인
        val hasSalaryKeyword = SALARY_PATTERNS.any { keyword ->
            text.contains(keyword.lowercase())
        }
        
        if (!hasSalaryKeyword) {
            android.util.Log.d("FinancialNotificationAnalyzer", "급여 키워드 없음")
            return null
        }
        
        // 금액 추출
        val amount = extractAmount(text) ?: run {
            android.util.Log.d("FinancialNotificationAnalyzer", "금액 추출 실패")
            return null
        }
        
        // 계좌번호 추출
        val accountNumber = extractAccountNumber(text)
        
        // 날짜/시간 추출
        val dateTime = extractDateTime(text)
        
        // 회사명 추출 (은행명에서 추정)
        val company = extractCompanyName(text, appName)
        
        val salaryInfo = SalaryInfo(
            amount = amount,
            company = company,
            bankName = appName,
            accountNumber = accountNumber,
            salaryDate = dateTime,
            isDetected = true
        )
        
        // 디버그 로그 추가 (품질 개선)
        android.util.Log.d("FinancialNotificationAnalyzer", "급여 정보 추출 성공: ${salaryInfo.amount}원, ${salaryInfo.salaryDate}")
        
        return salaryInfo
    }
    
    /**
     * 알림 텍스트에서 자동이체 정보 추출
     */
    fun extractAutoTransferInfo(notificationText: String, appName: String): AutoTransferInfo? {
        val text = notificationText.lowercase()
        
        // 자동이체 관련 키워드가 있는지 확인
        val hasAutoTransferKeyword = AUTO_TRANSFER_PATTERNS.any { keyword ->
            text.contains(keyword.lowercase())
        }
        
        if (!hasAutoTransferKeyword) return null
        
        // 금액 추출
        val amount = extractAmount(text) ?: return null
        
        // 계좌번호 추출
        val fromAccount = extractAccountNumber(text)
        val toAccount = extractToAccountNumber(text)
        
        // 날짜/시간 추출
        val dateTime = extractDateTime(text)
        
        // 이체 유형 추출
        val transferType = extractTransferType(text)
        
        return AutoTransferInfo(
            amount = amount,
            fromAccount = fromAccount,
            toAccount = toAccount,
            transferDate = dateTime,
            transferType = transferType
        )
    }
    
    /**
     * 텍스트에서 금액 추출 (급여 우선)
     */
    private fun extractAmount(text: String): Long? {
        val matcher = AMOUNT_PATTERN.matcher(text)
        val amounts = mutableListOf<Long>()
        
        while (matcher.find()) {
            val amountStr = matcher.group(1).replace(",", "")
            val amount = amountStr.toLongOrNull() ?: continue
            amounts.add(amount)
        }
        
        if (amounts.isEmpty()) return null
        
        // 급여 관련 키워드가 있는 위치 근처의 금액을 우선 선택
        val salaryKeywords = listOf("급여", "월급", "입금")
        
        for (keyword in salaryKeywords) {
            val keywordIndex = text.indexOf(keyword)
            if (keywordIndex >= 0) {
                // 키워드 근처의 금액을 찾기 위해 패턴 매칭
                val nearSalaryPattern = java.util.regex.Pattern.compile("$keyword\\s+(\\d{1,3}(?:,\\d{3})*)")
                val nearMatcher = nearSalaryPattern.matcher(text)
                if (nearMatcher.find()) {
                    val nearAmountStr = nearMatcher.group(1).replace(",", "")
                    val nearAmount = nearAmountStr.toLongOrNull()
                    if (nearAmount != null && amounts.contains(nearAmount)) {
                        return nearAmount
                    }
                }
            }
        }
        
        // 급여 관련 금액을 찾지 못한 경우, 가장 작은 금액을 선택 (급여는 보통 중간 정도 금액)
        return amounts.minOrNull()
    }
    
    /**
     * 텍스트에서 계좌번호 추출
     */
    private fun extractAccountNumber(text: String): String {
        val matcher = ACCOUNT_PATTERN.matcher(text)
        if (matcher.find()) {
            val account = matcher.group(1)
            return if (account.length >= 4) account.takeLast(4) else account
        }
        return ""
    }
    
    /**
     * 텍스트에서 입금 계좌번호 추출
     */
    private fun extractToAccountNumber(text: String): String {
        // "입금", "이체" 등의 키워드 뒤에 나오는 계좌번호 찾기
        val patterns = listOf(
            "입금.*?([0-9]{4,})",
            "이체.*?([0-9]{4,})",
            "납부.*?([0-9]{4,})"
        )
        
        for (pattern in patterns) {
            val p = Pattern.compile(pattern)
            val matcher = p.matcher(text)
            if (matcher.find()) {
                val account = matcher.group(1)
                return if (account.length >= 4) account.takeLast(4) else account
            }
        }
        
        return ""
    }
    
    /**
     * 텍스트에서 날짜/시간 추출
     */
    private fun extractDateTime(text: String): LocalDateTime {
        val now = LocalDateTime.now()
        
        // 1. 한국 날짜 형식 시도 (MM/dd)
        val koreanDateMatcher = KOREAN_DATE_PATTERN.matcher(text)
        if (koreanDateMatcher.find()) {
            val month = koreanDateMatcher.group(1).toIntOrNull() ?: now.monthValue
            val day = koreanDateMatcher.group(2).toIntOrNull() ?: now.dayOfMonth
            
            // 시간 추출
            val timeMatcher = TIME_PATTERN.matcher(text)
            if (timeMatcher.find()) {
                val hour = timeMatcher.group(1).toIntOrNull() ?: 0
                val minute = timeMatcher.group(2).toIntOrNull() ?: 0
                
                // 현재 연도 사용 (한국 형식은 연도가 없음)
                return LocalDateTime.of(now.year, month, day, hour, minute)
            }
            
            return LocalDateTime.of(now.year, month, day, 0, 0)
        }
        
        // 2. 기존 날짜 형식 시도 (yyyy-MM-dd)
        val dateMatcher = DATE_PATTERN.matcher(text)
        if (dateMatcher.find()) {
            val year = dateMatcher.group(1).toIntOrNull() ?: now.year
            val month = dateMatcher.group(2).toIntOrNull() ?: now.monthValue
            val day = dateMatcher.group(3).toIntOrNull() ?: now.dayOfMonth
            
            // 시간 추출
            val timeMatcher = TIME_PATTERN.matcher(text)
            if (timeMatcher.find()) {
                val hour = timeMatcher.group(1).toIntOrNull() ?: 0
                val minute = timeMatcher.group(2).toIntOrNull() ?: 0
                
                return LocalDateTime.of(year, month, day, hour, minute)
            }
            
            return LocalDateTime.of(year, month, day, 0, 0)
        }
        
        return now
    }
    
    /**
     * 텍스트에서 회사명 추출
     */
    private fun extractCompanyName(text: String, appName: String): String {
        // 은행명에서 회사명 추정
        val bankToCompany = mapOf(
            "신한은행" to "회사명",
            "국민은행" to "회사명",
            "우리은행" to "회사명",
            "하나은행" to "회사명",
            "농협은행" to "회사명",
            "기업은행" to "회사명"
        )
        
        return bankToCompany[appName] ?: "회사명"
    }
    
    /**
     * 텍스트에서 이체 유형 추출
     */
    private fun extractTransferType(text: String): String {
        val transferTypes = mapOf(
            "보험" to "보험료",
            "관리비" to "관리비",
            "적금" to "적금",
            "대출" to "대출상환",
            "카드" to "카드대금",
            "통신" to "통신비",
            "전기" to "전기요금",
            "가스" to "가스요금",
            "수도" to "수도요금"
        )
        
        for ((keyword, type) in transferTypes) {
            if (text.contains(keyword)) {
                return type
            }
        }
        
        return "기타"
    }
    
    /**
     * 급여 알림인지 확인
     */
    fun isSalaryNotification(notificationText: String): Boolean {
        val text = notificationText.lowercase()
        return SALARY_PATTERNS.any { keyword ->
            text.contains(keyword.lowercase())
        }
    }
    
    /**
     * 자동이체 알림인지 확인
     */
    fun isAutoTransferNotification(notificationText: String): Boolean {
        val text = notificationText.lowercase()
        return AUTO_TRANSFER_PATTERNS.any { keyword ->
            text.contains(keyword.lowercase())
        }
    }
}
