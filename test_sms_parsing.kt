// 실제 SMS 파싱 테스트
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun main() {
    println("=== 실제 SMS 파싱 테스트 ===")
    
    val smsData = listOf(
        "신한 10/11 21:54  100-***-159993 입금 급여  2,500,000 잔액  3,265,147 급여",
        "신한 09/11 21:54  100-***-159993 입금 급여  2,500,000 잔액  5,265,147 급여",
        "신한 08/11 21:54  100-***-159993 입금 급여  2,500,000 잔액  4,265,147 급여",
        "신한 07/11 21:54  100-***-159993 입금 급여  2,500,000 잔액  3,265,147 급여"
    )
    
    // 한국 AI 엔진의 급여 패턴 테스트
    val salaryPattern = java.util.regex.Pattern.compile("([가-힣]+)\\s+(\\d{1,2}/\\d{1,2})\\s+(\\d{1,2}:\\d{2})\\s+(\\d{3}-\\*{3}-\\d{6})\\s+입금\\s+급여\\s+(\\d{1,3}(?:,\\d{3})*)\\s+잔액")
    
    val parsedSalaries = mutableListOf<SalaryData>()
    
    smsData.forEachIndexed { index, sms ->
        println("SMS ${index + 1}: $sms")
        
        val matcher = salaryPattern.matcher(sms)
        if (matcher.find()) {
            val bankName = matcher.group(1) ?: ""
            val dateStr = matcher.group(2) ?: ""
            val timeStr = matcher.group(3) ?: ""
            val accountNumber = matcher.group(4) ?: ""
            val amountStr = matcher.group(5) ?: ""
            
            val amount = amountStr.replace(",", "").toLongOrNull() ?: 0L
            
            // 날짜 파싱 (현재 연도 기준)
            val currentYear = LocalDateTime.now().year
            val dateTimeStr = "$currentYear/$dateStr $timeStr"
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
            val dateTime = LocalDateTime.parse(dateTimeStr, formatter)
            
            val salaryData = SalaryData(
                bankName = bankName,
                dateTime = dateTime,
                accountNumber = accountNumber,
                amount = amount
            )
            
            parsedSalaries.add(salaryData)
            
            println("  파싱 성공:")
            println("    은행: $bankName")
            println("    날짜: $dateTime")
            println("    계좌: $accountNumber")
            println("    금액: ${String.format("%,d", amount)}원")
            println()
        } else {
            println("  파싱 실패!")
            println()
        }
    }
    
    // 중복 검사 시뮬레이션
    println("=== 중복 검사 결과 ===")
    val uniqueSalaries = mutableListOf<SalaryData>()
    
    parsedSalaries.forEach { salary ->
        val isDuplicate = uniqueSalaries.any { existing ->
            existing.dateTime == salary.dateTime &&
            existing.amount == salary.amount &&
            existing.bankName == salary.bankName
        }
        
        if (!isDuplicate) {
            uniqueSalaries.add(salary)
            println("새로운 급여 추가: ${salary.dateTime} - ${String.format("%,d", salary.amount)}원")
        } else {
            println("중복 급여 감지: ${salary.dateTime} - ${String.format("%,d", salary.amount)}원")
        }
    }
    
    // 이번 달 수입 계산
    val currentMonth = LocalDateTime.now().monthValue
    val currentYear = LocalDateTime.now().year
    
    val thisMonthSalaries = uniqueSalaries.filter { salary ->
        salary.dateTime.year == currentYear && salary.dateTime.monthValue == currentMonth
    }
    
    val thisMonthIncome = thisMonthSalaries.sumOf { it.amount }
    
    println("\n=== 최종 결과 ===")
    println("총 급여 건수: ${uniqueSalaries.size}건")
    println("이번 달 급여: ${thisMonthSalaries.size}건")
    println("이번 달 수입: ${String.format("%,d", thisMonthIncome)}원")
    
    uniqueSalaries.forEach { salary ->
        val isThisMonth = salary.dateTime.year == currentYear && salary.dateTime.monthValue == currentMonth
        val monthFlag = if (isThisMonth) " [이번달]" else ""
        println("  - ${salary.dateTime}: ${String.format("%,d", salary.amount)}원$monthFlag")
    }
}

data class SalaryData(
    val bankName: String,
    val dateTime: LocalDateTime,
    val accountNumber: String,
    val amount: Long
)
