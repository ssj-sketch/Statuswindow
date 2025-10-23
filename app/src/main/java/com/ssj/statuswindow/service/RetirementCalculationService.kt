package com.ssj.statuswindow.service

import com.ssj.statuswindow.model.RetirementPlan
import com.ssj.statuswindow.model.DynamicRetirementAsset
import com.ssj.statuswindow.model.PensionContributionInfo
import com.ssj.statuswindow.model.RetirementSettings
import com.ssj.statuswindow.model.RefinedPensionCalculationResult
import com.ssj.statuswindow.model.YearlySalaryInfo
import com.ssj.statuswindow.model.RetirementAssetEstimate
import com.ssj.statuswindow.model.PensionCalculationResult
import com.ssj.statuswindow.model.PensionInputInfo
import com.ssj.statuswindow.model.PensionCalculationInfo
import kotlin.math.pow
import java.text.NumberFormat
import java.util.Locale
import java.time.LocalDate

/**
 * 은퇴 계획 계산 서비스
 */
object RetirementCalculationService {
    
    // 국가별 물가상승율 (20년 평균, 연율)
    private val INFLATION_RATES = mapOf(
        "KR" to 0.025, // 한국: 2.5%
        "US" to 0.022, // 미국: 2.2%
        "JP" to 0.008, // 일본: 0.8%
        "DE" to 0.015, // 독일: 1.5%
        "GB" to 0.020, // 영국: 2.0%
        "FR" to 0.015, // 프랑스: 1.5%
        "CA" to 0.018, // 캐나다: 1.8%
        "AU" to 0.025, // 호주: 2.5%
        "CN" to 0.030, // 중국: 3.0%
        "IN" to 0.060  // 인도: 6.0%
    )
    
    // 국가별 평균수명 (2023년 기준)
    private val LIFE_EXPECTANCY = mapOf(
        "KR" to 83.5, // 한국
        "US" to 76.1, // 미국
        "JP" to 84.3, // 일본
        "DE" to 81.0, // 독일
        "GB" to 80.7, // 영국
        "FR" to 82.3, // 프랑스
        "CA" to 82.1, // 캐나다
        "AU" to 82.8, // 호주
        "CN" to 77.1, // 중국
        "IN" to 70.4  // 인도
    )
    
    // 국가별 국민연금 수령률 (급여 대비)
    private val PENSION_RATES = mapOf(
        "KR" to 0.40, // 한국: 40%
        "US" to 0.35, // 미국: 35%
        "JP" to 0.50, // 일본: 50%
        "DE" to 0.45, // 독일: 45%
        "GB" to 0.30, // 영국: 30%
        "FR" to 0.50, // 프랑스: 50%
        "CA" to 0.25, // 캐나다: 25%
        "AU" to 0.30, // 호주: 30%
        "CN" to 0.35, // 중국: 35%
        "IN" to 0.20  // 인도: 20%
    )
    
    /**
     * 은퇴 계획 계산
     */
    fun calculateRetirementPlan(
        currentAge: Int,
        retirementAge: Int,
        currentSalary: Long,
        monthlyExpense: Long,
        country: String = "KR"
    ): RetirementPlan {
        val inflationRate = INFLATION_RATES[country] ?: 0.025
        val pensionRate = PENSION_RATES[country] ?: 0.40
        
        // 은퇴까지 남은 년수
        val yearsToRetirement = retirementAge - currentAge
        
        // 은퇴 시점의 월 생활비 (물가상승 반영)
        val monthlyRetirementExpense = calculateFutureValue(
            monthlyExpense.toDouble(),
            inflationRate,
            yearsToRetirement
        ).toLong()
        
        // 은퇴 후 예상 수명 (은퇴 시점 기준)
        val lifeExpectancy = LIFE_EXPECTANCY[country] ?: 83.5
        val retirementYears = lifeExpectancy - retirementAge
        
        // 목표 은퇴 자산 계산 (4% 규칙 적용)
        val targetRetirementAsset = monthlyRetirementExpense * 12 * 25 // 25년분
        
        // 국민연금 수령액 계산
        val pensionAmount = calculatePensionAmount(currentSalary, pensionRate, inflationRate, yearsToRetirement)
        
        return RetirementPlan(
            currentAge = currentAge,
            retirementAge = retirementAge,
            currentSalary = currentSalary,
            monthlyExpense = monthlyExpense,
            targetRetirementAsset = targetRetirementAsset,
            monthlyRetirementExpense = monthlyRetirementExpense,
            pensionAmount = pensionAmount,
            country = country,
            inflationRate = inflationRate
        )
    }
    
    /**
     * 국민연금 수령액 계산
     */
    private fun calculatePensionAmount(
        currentSalary: Long,
        pensionRate: Double,
        inflationRate: Double,
        yearsToRetirement: Int
    ): Long {
        // 현재 급여를 은퇴 시점 가치로 조정
        val futureSalary = calculateFutureValue(
            currentSalary.toDouble(),
            inflationRate,
            yearsToRetirement
        )
        
        // 국민연금 수령액 계산
        return (futureSalary * pensionRate).toLong()
    }
    
    /**
     * 국가별 기대수명 반환
     */
    private fun getLifeExpectancy(age: Int, country: String): Int {
        val lifeExpectancyByCountry = mapOf(
            "KR" to 83,
            "US" to 79,
            "JP" to 85,
            "DE" to 81,
            "GB" to 81,
            "FR" to 83,
            "CA" to 82,
            "AU" to 83,
            "CN" to 77,
            "IN" to 70
        )
        
        return lifeExpectancyByCountry[country] ?: 80
    }
    
    /**
     * 월 저축 필요액 계산
     */
    fun calculateRequiredMonthlySavings(
        targetAsset: Long,
        currentAge: Int,
        retirementAge: Int,
        expectedReturn: Double = 0.07 // 연 7% 수익률 가정
    ): Long {
        val yearsToRetirement = retirementAge - currentAge
        val monthsToRetirement = yearsToRetirement * 12
        
        // 월 수익률
        val monthlyReturn = expectedReturn / 12
        
        // 필요한 월 저축액 계산 (연금 공식)
        val requiredSavings = if (monthlyReturn > 0) {
            targetAsset * monthlyReturn / ((1 + monthlyReturn).pow(monthsToRetirement) - 1)
        } else {
            targetAsset / monthsToRetirement
        }
        
        return requiredSavings.toLong()
    }
    
    /**
     * 현재 소비 패턴으로 예상되는 은퇴 자산 계산
     */
    fun calculateProjectedRetirementAsset(
        currentAge: Int,
        retirementAge: Int,
        currentSalary: Long,
        monthlyExpense: Long,
        monthlySavings: Long,
        expectedReturn: Double = 0.07
    ): Long {
        val yearsToRetirement = retirementAge - currentAge
        val monthsToRetirement = yearsToRetirement * 12
        
        // 월 수익률
        val monthlyReturn = expectedReturn / 12
        
        // 미래 가치 계산 (연금 공식)
        val futureValue = if (monthlyReturn > 0) {
            monthlySavings * ((1 + monthlyReturn).pow(monthsToRetirement) - 1) / monthlyReturn
        } else {
            monthlySavings * monthsToRetirement
        }
        
        return futureValue.toLong()
    }
    
    /**
     * 소비 증가가 은퇴 자산에 미치는 영향 계산
     */
    fun calculateSpendingImpact(
        additionalSpending: Long,
        currentAge: Int,
        retirementAge: Int,
        expectedReturn: Double = 0.07
    ): Long {
        val yearsToRetirement = retirementAge - currentAge
        val monthsToRetirement = yearsToRetirement * 12
        
        // 월 수익률
        val monthlyReturn = expectedReturn / 12
        
        // 추가 소비로 인한 기회비용 (복리 효과)
        val opportunityCost = if (monthlyReturn > 0) {
            additionalSpending * ((1 + monthlyReturn).pow(monthsToRetirement) - 1) / monthlyReturn
        } else {
            additionalSpending * monthsToRetirement
        }
        
        return opportunityCost.toLong()
    }
    
    /**
     * 현재가치 계산
     */
    private fun calculatePresentValue(futureValue: Double, discountRate: Double, years: Int): Double {
        return futureValue / (1 + discountRate).pow(years)
    }
    
    /**
     * 미래가치 계산
     */
    private fun calculateFutureValue(presentValue: Double, interestRate: Double, years: Int): Double {
        return presentValue * (1 + interestRate).pow(years)
    }
    fun calculateDynamicRetirementAsset(
        currentAge: Int,
        retirementAge: Int,
        currentSalary: Long,
        monthlyIncome: Long,
        monthlySpending: Long,
        currentAsset: Long = 0L,
        country: String = "KR"
    ): DynamicRetirementAsset {
        val inflationRate = INFLATION_RATES[country] ?: 0.025
        val yearsToRetirement = retirementAge - currentAge
        
        // 월 가처분 소득 계산
        val monthlyDisposableIncome = monthlyIncome - monthlySpending
        
        // 은퇴까지의 총 수입 (현재가치 기준)
        val totalIncomeToRetirement = calculatePresentValue(
            monthlyIncome.toDouble() * 12,
            inflationRate,
            yearsToRetirement
        ).toLong()
        
        // 은퇴까지의 총 소비 (현재가치 기준)
        val totalSpendingToRetirement = calculatePresentValue(
            monthlySpending.toDouble() * 12,
            inflationRate,
            yearsToRetirement
        ).toLong()
        
        // 예상 은퇴자산 = 현재자산 + 총수입 - 총소비
        val projectedRetirementAsset = currentAsset + totalIncomeToRetirement - totalSpendingToRetirement
        
        // 은퇴 후 월 생활비 계산 (4% 규칙 적용)
        val monthlyRetirementExpense = if (projectedRetirementAsset > 0) {
            projectedRetirementAsset / (25 * 12) // 25년분으로 나누기
        } else {
            0L
        }
        
        // 국민연금 현재가치 계산
        val currentPensionValue = calculateCurrentPensionValue(currentSalary, country)
        
        return DynamicRetirementAsset(
            projectedAsset = projectedRetirementAsset,
            monthlyRetirementExpense = monthlyRetirementExpense,
            currentPensionValue = currentPensionValue,
            monthlyDisposableIncome = monthlyDisposableIncome,
            totalIncomeToRetirement = totalIncomeToRetirement,
            totalSpendingToRetirement = totalSpendingToRetirement
        )
    }
    
    /**
     * 국민연금 현재가치 계산
     */
    private fun calculateCurrentPensionValue(currentSalary: Long, country: String): Long {
        val pensionRate = PENSION_RATES[country] ?: 0.40
        val inflationRate = INFLATION_RATES[country] ?: 0.025
        
        // 현재 급여 기준 국민연금 수령액
        val annualPension = currentSalary * 12 * pensionRate
        
        // 현재가치로 환산 (20년간 수령 가정)
        return calculatePresentValue(annualPension.toDouble(), inflationRate, 20).toLong()
    }
    
    /**
     * 실수령액 기반 국민연금 납입액 역계산
     * 국민연금 공식 홈페이지 기준: https://www.nps.or.kr/comm/quick/getOHAH0011P0.do
     */
    fun calculatePensionContributionFromNetSalary(netSalary: Long, country: String = "KR"): PensionContributionInfo {
        // 2025년 기준 국민연금 정보
        val minIncome = 400000L      // 기준소득월액 하한액
        val maxIncome = 6370000L     // 기준소득월액 상한액
        val pensionRate = 0.045      // 국민연금 보험료율 4.5%
        val incomeTaxRate = 0.10     // 소득세율 (대략 10%)
        val localTaxRate = 0.10      // 지방소득세율 (소득세의 10%)
        val healthInsuranceRate = 0.035 // 건강보험료율 3.5%
        val employmentInsuranceRate = 0.008 // 고용보험료율 0.8%
        
        // 총 공제율 계산
        val totalDeductionRate = pensionRate + incomeTaxRate + localTaxRate + healthInsuranceRate + employmentInsuranceRate
        
        // 실수령액에서 총급여 역계산
        val grossSalary = (netSalary / (1 - totalDeductionRate)).toLong()
        
        // 기준소득월액 범위 내로 조정
        val adjustedGrossSalary = grossSalary.coerceIn(minIncome, maxIncome)
        
        // 국민연금 납입액 계산
        val pensionContribution = (adjustedGrossSalary * pensionRate).toLong()
        
        // 기타 공제액 계산
        val incomeTax = (adjustedGrossSalary * incomeTaxRate).toLong()
        val localTax = (incomeTax * localTaxRate).toLong()
        val healthInsurance = (adjustedGrossSalary * healthInsuranceRate).toLong()
        val employmentInsurance = (adjustedGrossSalary * employmentInsuranceRate).toLong()
        
        return PensionContributionInfo(
            grossSalary = adjustedGrossSalary,
            netSalary = netSalary,
            pensionContribution = pensionContribution,
            incomeTax = incomeTax,
            localTax = localTax,
            healthInsurance = healthInsurance,
            employmentInsurance = employmentInsurance,
            totalDeductions = pensionContribution + incomeTax + localTax + healthInsurance + employmentInsurance
        )
    }
    
    /**
     * 국민연금 수령액 계산 (가입기간별)
     * 국민연금 공식: {1.245*(A+B)*P20/P+...+1.2*(A+B)*P23/P}(1+0.05n/12) x 지급률
     */
    fun calculatePensionAmount(
        contributionYears: Int,
        averageIncome: Long,
        country: String = "KR"
    ): Long {
        // 2025년 기준 전체 가입자 평균 소득월액 (A값)
        val averageAllIncome = 3089062L
        
        // 가입기간별 지급률 계산
        val paymentRate = when {
            contributionYears >= 40 -> 1.0
            contributionYears >= 35 -> 0.95
            contributionYears >= 30 -> 0.90
            contributionYears >= 25 -> 0.85
            contributionYears >= 20 -> 0.80
            contributionYears >= 15 -> 0.75
            contributionYears >= 10 -> 0.50 + (contributionYears - 10) * 0.05
            else -> 0.0
        }
        
        // 연금액 산정 공식 적용
        val baseAmount = (averageAllIncome + averageIncome) * 1.245
        val monthlyPension = (baseAmount * paymentRate).toLong()
        
        return monthlyPension
    }
    
    /**
     * 새로운 국민연금 계산 공식 (사용자 요청사항 반영)
     * 기준소득월액 = 월 실수령액 / 0.9
     * 예상 국민연금 월수령액 = 기준소득월액 × (0.015 × 가입연수)
     */
    fun calculatePensionWithNewFormula(
        birthDate: String,           // 생년월일 (예: 1980-05)
        netMonthlyIncome: Long,      // 월급여 실수령액 (세후 금액, 원 단위)
        retirementAge: Int,          // 은퇴희망나이 (예: 60)
        subscriptionMonths: Int      // 국민연금 가입 개월수 (예: 360)
    ): PensionCalculationResult {
        val nf = NumberFormat.getNumberInstance(Locale.KOREA)
        
        // 1. 기준소득월액 = 월 실수령액 / 0.9 (세후 → 세전 환산)
        val baseIncomeAmount = (netMonthlyIncome / 0.9).toLong()
        
        // 2. 가입연수 = 가입개월수 / 12
        val subscriptionYears = subscriptionMonths / 12.0
        
        // 3. 예상 국민연금 월수령액 = 기준소득월액 × (0.015 × 가입연수)
        val expectedPensionAmount = (baseIncomeAmount * (0.015 * subscriptionYears)).toLong()
        
        // 입력 정보 포맷팅
        val inputInfo = PensionInputInfo(
            birthDate = birthDate,
            netMonthlyIncome = "${nf.format(netMonthlyIncome)}원",
            retirementAge = "${retirementAge}세",
            subscriptionMonths = "${subscriptionMonths}개월"
        )
        
        // 계산 결과 포맷팅
        val calculationInfo = PensionCalculationInfo(
            baseIncomeAmount = "${nf.format(baseIncomeAmount)}원",
            expectedPensionAmount = "${nf.format(expectedPensionAmount)}원/월"
        )
        
        val description = "월 실수령액을 세전 ${nf.format(baseIncomeAmount)}원으로 환산하고, 가입기간 ${subscriptionYears.toInt()}년에 따라 ${nf.format(expectedPensionAmount)}원의 연금액이 산출됨."
        
        return PensionCalculationResult(
            inputInfo = inputInfo,
            calculationResult = calculationInfo,
            description = description
        )
    }
    
    /**
     * 은퇴설정 기반 국민연금 계산
     */
    fun calculatePensionFromRetirementSettings(
        settings: RetirementSettings,
        netMonthlyIncome: Long
    ): PensionCalculationResult {
        val birthDateStr = "${settings.birthDate.year}-${settings.birthDate.monthValue.toString().padStart(2, '0')}"
        
        return calculatePensionWithNewFormula(
            birthDate = birthDateStr,
            netMonthlyIncome = netMonthlyIncome,
            retirementAge = settings.desiredRetirementAge,
            subscriptionMonths = settings.pensionSubscriptionMonths
        )
    }
    
    /**
     * 은퇴 시점 총자산 추정 계산
     */
    fun calculateRetirementAssetEstimate(
        retirementSettings: RetirementSettings,
        currentAssets: Long = 0L,
        currentMonthlyIncome: Long = 0L,
        currentMonthlyExpense: Long = 0L,
        countryCode: String = "KR"
    ): RetirementAssetEstimate {
        val currentDate = LocalDate.now()
        val currentAge = calculateAge(retirementSettings.birthDate, currentDate)
        val yearsToRetirement = retirementSettings.desiredRetirementAge - currentAge
        
        // 물가상승율 및 실질 수익률
        val inflationRate = INFLATION_RATES[countryCode] ?: 0.025
        val realReturnRate = 0.05 // 실질 수익률 5% 가정 (물가상승 고려)
        
        // 월 저축액 계산
        val monthlySavings = maxOf(0L, currentMonthlyIncome - currentMonthlyExpense)
        val annualSavings = monthlySavings * 12
        
        // 은퇴까지 총 저축액 (복리 계산)
        val totalSavingsUntilRetirement = if (yearsToRetirement > 0) {
            calculateFutureValue(annualSavings, realReturnRate, yearsToRetirement)
        } else {
            0L
        }
        
        // 현재 자산의 미래 가치
        val currentAssetsFutureValue = if (yearsToRetirement > 0) {
            calculateFutureValue(currentAssets, realReturnRate, yearsToRetirement)
        } else {
            currentAssets
        }
        
        // 은퇴 시점 추정 총자산
        val estimatedRetirementAssets = currentAssetsFutureValue + totalSavingsUntilRetirement
        
        // 국민연금 계산
        val pensionResult = calculatePensionFromRetirementSettings(retirementSettings, currentMonthlyIncome)
        val nationalPensionMonthly = extractPensionAmount(pensionResult.calculationResult.expectedPensionAmount)
        
        // 은퇴 후 월 생활비 계산 (현재 생활비 기준, 물가상승 고려)
        val monthlyLivingExpenseAfterRetirement = if (yearsToRetirement > 0) {
            calculateFutureValue(currentMonthlyExpense, inflationRate, yearsToRetirement)
        } else {
            currentMonthlyExpense
        }
        
        // 은퇴 후 생존 년수
        val lifeExpectancy = LIFE_EXPECTANCY[countryCode] ?: 83.5
        val yearsAfterRetirement = maxOf(1, (lifeExpectancy - retirementSettings.desiredRetirementAge).toInt())
        
        // 총 국민연금 수령액
        val totalPensionReceived = nationalPensionMonthly * 12 * yearsAfterRetirement
        
        return RetirementAssetEstimate(
            currentAge = currentAge,
            retirementAge = retirementSettings.desiredRetirementAge,
            yearsToRetirement = yearsToRetirement,
            currentAssets = currentAssets,
            estimatedMonthlyIncome = currentMonthlyIncome,
            estimatedMonthlyExpense = currentMonthlyExpense,
            monthlySavings = monthlySavings,
            annualSavings = annualSavings,
            totalSavingsUntilRetirement = totalSavingsUntilRetirement,
            estimatedRetirementAssets = estimatedRetirementAssets,
            inflationRate = inflationRate,
            realReturnRate = realReturnRate,
            nationalPensionMonthly = nationalPensionMonthly,
            monthlyLivingExpenseAfterRetirement = monthlyLivingExpenseAfterRetirement,
            yearsAfterRetirement = yearsAfterRetirement,
            totalPensionReceived = totalPensionReceived,
            calculationDate = currentDate
        )
    }
    
    /**
     * 미래 가치 계산 (복리)
     */
    private fun calculateFutureValue(presentValue: Long, annualRate: Double, years: Int): Long {
        if (years <= 0) return presentValue
        return (presentValue * (1 + annualRate).pow(years)).toLong()
    }
    
    /**
     * 나이 계산
     */
    private fun calculateAge(birthDate: LocalDate, currentDate: LocalDate): Int {
        var age = currentDate.year - birthDate.year
        if (currentDate.monthValue < birthDate.monthValue || 
            (currentDate.monthValue == birthDate.monthValue && currentDate.dayOfMonth < birthDate.dayOfMonth)) {
            age--
        }
        return age
    }
    
    /**
     * 국민연금 금액에서 숫자만 추출
     */
    private fun extractPensionAmount(pensionAmountStr: String): Long {
        return try {
            pensionAmountStr.replace(Regex("[^0-9]"), "").toLong()
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * 정교화된 국민연금 계산
     * - 실제 급여가 없을 경우 표시 안함
     * - 이번달 실수령액을 기준으로 과거와 미래 급여 추산
     * - 각 국가별 물가상승율 적용
     */
    fun calculateRefinedPension(
        retirementSettings: RetirementSettings,
        currentMonthlySalary: Long,
        countryCode: String = "KR"
    ): RefinedPensionCalculationResult? {
        // 실제 급여가 없으면 null 반환 (표시 안함)
        if (currentMonthlySalary <= 0) {
            return null
        }
        
        val currentDate = LocalDate.now()
        val currentYear = currentDate.year
        val currentAge = calculateAge(retirementSettings.birthDate, currentDate)
        val inflationRate = INFLATION_RATES[countryCode] ?: 0.025
        
        // 가입 시작 연도 계산 (30세부터 가입했다고 가정)
        val subscriptionStartAge = 30
        val subscriptionStartYear = retirementSettings.birthDate.year + subscriptionStartAge
        
        // 가입 종료 연도 계산 (현재 연도 또는 은퇴 연도 중 작은 값)
        val subscriptionEndYear = minOf(currentYear, retirementSettings.birthDate.year + retirementSettings.desiredRetirementAge)
        
        // 연도별 급여 이력 생성
        val yearlySalaryHistory = mutableListOf<YearlySalaryInfo>()
        
        for (year in subscriptionStartYear..subscriptionEndYear) {
            val yearsFromCurrent = year - currentYear
            val monthlySalary = calculateSalaryForYear(currentMonthlySalary, yearsFromCurrent, inflationRate)
            val annualSalary = monthlySalary * 12
            
            // 해당 연도의 가입 개월수 계산
            val contributionMonths = when {
                year < currentYear -> 12 // 과거 연도는 모두 가입
                year == currentYear -> currentDate.monthValue // 현재 연도는 현재 월까지
                else -> 0 // 미래 연도는 가입하지 않음
            }
            
            yearlySalaryHistory.add(
                YearlySalaryInfo(
                    year = year,
                    monthlySalary = monthlySalary,
                    annualSalary = annualSalary,
                    contributionMonths = contributionMonths,
                    isEstimated = year != currentYear
                )
            )
        }
        
        // 평균 월급여 계산 (실제 가입한 기간만)
        val totalContributionMonths = yearlySalaryHistory.sumOf { it.contributionMonths }
        val averageMonthlySalary = if (totalContributionMonths > 0) {
            yearlySalaryHistory.sumOf { it.monthlySalary * it.contributionMonths } / totalContributionMonths
        } else {
            currentMonthlySalary
        }
        
        // 국민연금 계산 (새로운 공식 적용)
        val subscriptionYears = totalContributionMonths / 12.0
        val baseIncomeAmount = (averageMonthlySalary / 0.9).toLong() // 세후 → 세전 환산
        val estimatedMonthlyPension = (baseIncomeAmount * (0.015 * subscriptionYears)).toLong()
        
        val calculationMethod = if (yearlySalaryHistory.any { it.isEstimated }) {
            "현재 급여를 기준으로 과거/미래 급여를 물가상승율(${(inflationRate * 100).toInt()}%)로 추산하여 계산"
        } else {
            "실제 급여 이력을 기반으로 계산"
        }
        
        return RefinedPensionCalculationResult(
            currentMonthlySalary = currentMonthlySalary,
            hasActualSalary = true,
            yearlySalaryHistory = yearlySalaryHistory,
            averageMonthlySalary = averageMonthlySalary,
            totalContributionMonths = totalContributionMonths,
            estimatedMonthlyPension = estimatedMonthlyPension,
            calculationMethod = calculationMethod,
            inflationRate = inflationRate,
            calculationDate = currentDate
        )
    }
    
    /**
     * 특정 연도의 급여 계산 (물가상승율 적용/역산)
     */
    private fun calculateSalaryForYear(currentSalary: Long, yearsFromCurrent: Int, inflationRate: Double): Long {
        if (yearsFromCurrent == 0) return currentSalary
        
        return if (yearsFromCurrent > 0) {
            // 미래 연도: 물가상승율 적용
            (currentSalary * (1 + inflationRate).pow(yearsFromCurrent)).toLong()
        } else {
            // 과거 연도: 물가상승율 역산
            (currentSalary * (1 / (1 + inflationRate)).pow(-yearsFromCurrent)).toLong()
        }
    }
}
