package com.ssj.statuswindow.model

import java.time.LocalDate

/**
 * 정교화된 국민연금 계산을 위한 연도별 급여 정보
 */
data class YearlySalaryInfo(
    val year: Int,                    // 연도
    val monthlySalary: Long,          // 해당 연도 월급여
    val annualSalary: Long,           // 해당 연도 연급여
    val contributionMonths: Int,      // 해당 연도 가입 개월수 (최대 12개월)
    val isEstimated: Boolean          // 추정값 여부 (true: 과거/미래 추정, false: 실제)
)

/**
 * 정교화된 국민연금 계산 결과
 */
data class RefinedPensionCalculationResult(
    val currentMonthlySalary: Long,              // 현재 월급여
    val hasActualSalary: Boolean,                // 실제 급여 입력 여부
    val yearlySalaryHistory: List<YearlySalaryInfo>, // 연도별 급여 이력
    val averageMonthlySalary: Long,              // 평균 월급여 (가입기간 전체)
    val totalContributionMonths: Int,            // 총 가입 개월수
    val estimatedMonthlyPension: Long,           // 예상 월 국민연금
    val calculationMethod: String,                // 계산 방법 설명
    val inflationRate: Double,                   // 적용된 물가상승율
    val calculationDate: LocalDate                // 계산 일자
)
