package com.ssj.statuswindow.model

import java.time.LocalDate

/**
 * 은퇴설정 정보
 */
data class RetirementSettings(
    val birthDate: LocalDate,           // 생년월일
    val pensionSubscriptionMonths: Int, // 국민연금 납부개월수
    val desiredRetirementAge: Int       // 희망은퇴나이
) {
    /**
     * 현재 나이 계산
     */
    fun getCurrentAge(): Int {
        val today = LocalDate.now()
        var age = today.year - birthDate.year
        if (today.monthValue < birthDate.monthValue || 
            (today.monthValue == birthDate.monthValue && today.dayOfMonth < birthDate.dayOfMonth)) {
            age--
        }
        return age
    }
    
    /**
     * 은퇴까지 남은 년수
     */
    fun getYearsToRetirement(): Int {
        return desiredRetirementAge - getCurrentAge()
    }
    
    /**
     * 국민연금 가입 연수
     */
    fun getPensionSubscriptionYears(): Double {
        return pensionSubscriptionMonths / 12.0
    }
}

/**
 * 국민연금 계산 결과
 */
data class PensionCalculationResult(
    val inputInfo: PensionInputInfo,
    val calculationResult: PensionCalculationInfo,
    val description: String
)

/**
 * 국민연금 입력 정보
 */
data class PensionInputInfo(
    val birthDate: String,           // 생년월일 (YYYY-MM)
    val netMonthlyIncome: String,     // 월급여실수령액
    val retirementAge: String,        // 은퇴희망나이
    val subscriptionMonths: String    // 가입개월수
)

/**
 * 국민연금 계산 정보
 */
data class PensionCalculationInfo(
    val baseIncomeAmount: String,     // 기준소득월액
    val expectedPensionAmount: String // 예상연금수령액(65세부터)
)
