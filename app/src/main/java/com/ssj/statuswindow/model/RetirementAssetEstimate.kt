package com.ssj.statuswindow.model

import java.time.LocalDate

/**
 * 은퇴 시점 총자산 추정 결과
 */
data class RetirementAssetEstimate(
    val currentAge: Int,                    // 현재 나이
    val retirementAge: Int,                 // 은퇴 나이
    val yearsToRetirement: Int,             // 은퇴까지 남은 년수
    val currentAssets: Long,                // 현재 자산
    val estimatedMonthlyIncome: Long,       // 예상 월 수입
    val estimatedMonthlyExpense: Long,       // 예상 월 지출
    val monthlySavings: Long,               // 월 저축액
    val annualSavings: Long,                // 연 저축액
    val totalSavingsUntilRetirement: Long,  // 은퇴까지 총 저축액
    val estimatedRetirementAssets: Long,    // 은퇴 시점 추정 총자산
    val inflationRate: Double,             // 물가상승율
    val realReturnRate: Double,             // 실질 수익률
    val nationalPensionMonthly: Long,       // 월 국민연금
    val monthlyLivingExpenseAfterRetirement: Long, // 은퇴 후 월 생활비
    val yearsAfterRetirement: Int,          // 은퇴 후 생존 년수
    val totalPensionReceived: Long,         // 총 국민연금 수령액
    val calculationDate: LocalDate          // 계산 일자
)
