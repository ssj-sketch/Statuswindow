package com.ssj.statuswindow.model

import java.time.LocalDateTime

/**
 * 급여 정보 데이터 모델
 */
data class SalaryInfo(
    val id: Long = 0,
    val amount: Long, // 급여 금액
    val company: String, // 회사명
    val bankName: String, // 은행명
    val accountNumber: String, // 계좌번호 (마지막 4자리)
    val salaryDate: LocalDateTime, // 급여 지급일
    val isDetected: Boolean = true, // 자동 감지 여부
    val memo: String = "" // 메모
)

/**
 * 자동이체 정보 데이터 모델
 */
data class AutoTransferInfo(
    val id: Long = 0,
    val amount: Long, // 이체 금액
    val fromAccount: String, // 출금 계좌
    val toAccount: String, // 입금 계좌
    val transferDate: LocalDateTime, // 이체일
    val transferType: String, // 이체 유형 (예: 보험료, 관리비, 적금 등)
    val memo: String = "" // 메모
)

/**
 * 은퇴 계획 정보 데이터 모델
 */
data class RetirementPlan(
    val currentAge: Int, // 현재 나이
    val retirementAge: Int, // 은퇴 희망 나이
    val currentSalary: Long, // 현재 급여
    val monthlyExpense: Long, // 월 생활비
    val targetRetirementAsset: Long, // 목표 은퇴 자산
    val monthlyRetirementExpense: Long, // 은퇴 후 월 생활비
    val pensionAmount: Long, // 예상 국민연금 수령액
    val country: String = "KR", // 국가 코드
    val inflationRate: Double = 0.025 // 물가상승율 (연 2.5%)
)

/**
 * 동적 은퇴자산 정보
 */
data class DynamicRetirementAsset(
    val projectedAsset: Long,              // 예상 은퇴자산
    val monthlyRetirementExpense: Long,     // 은퇴 후 월 생활비
    val currentPensionValue: Long,          // 현재가치 기준 국민연금
    val monthlyDisposableIncome: Long,     // 월 가처분 소득
    val totalIncomeToRetirement: Long,      // 은퇴까지 총 수입
    val totalSpendingToRetirement: Long     // 은퇴까지 총 소비
)

/**
 * 국민연금 납입 정보
 */
data class PensionContributionInfo(
    val grossSalary: Long,              // 총급여
    val netSalary: Long,                // 실수령액
    val pensionContribution: Long,      // 국민연금 납입액
    val incomeTax: Long,                // 소득세
    val localTax: Long,                 // 지방소득세
    val healthInsurance: Long,          // 건강보험료
    val employmentInsurance: Long,      // 고용보험료
    val totalDeductions: Long           // 총 공제액
)
