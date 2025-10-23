package com.ssj.statuswindow.repo

import com.ssj.statuswindow.model.IncomeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 수입 정보 관리 Repository
 */
class IncomeRepository {
    
    private val _incomes = MutableStateFlow<List<IncomeInfo>>(emptyList())
    val incomes: StateFlow<List<IncomeInfo>> = _incomes.asStateFlow()
    
    /**
     * 수입 정보 추가 (중복 검사 포함 - 품질 개선)
     */
    fun addIncome(income: IncomeInfo) {
        val currentList = _incomes.value.toMutableList()
        
        // 중복 검사 개선: 같은 날짜, 같은 금액, 같은 은행의 급여는 중복으로 간주
        // 단, 급여의 경우 월별로 구분하여 중복 검사 (품질 개선)
        val isDuplicate = currentList.any { existing ->
            existing.transactionDate.toLocalDate() == income.transactionDate.toLocalDate() &&
            existing.amount == income.amount &&
            existing.bankName == income.bankName &&
            existing.description.contains("급여") && income.description.contains("급여") &&
            existing.source == income.source &&
            existing.transactionDate.year == income.transactionDate.year &&
            existing.transactionDate.monthValue == income.transactionDate.monthValue
        }
        
        if (!isDuplicate) {
            currentList.add(income)
            _incomes.value = currentList
            
            // 디버그 로그 추가 (품질 개선)
            android.util.Log.d("IncomeRepository", "새로운 수입 추가: ${income.description} - ${income.amount}원 (${income.transactionDate})")
        } else {
            android.util.Log.d("IncomeRepository", "중복 수입 감지됨: ${income.description} - ${income.amount}원 (${income.transactionDate})")
        }
    }
    
    /**
     * 수입 정보 목록 가져오기 (최신순 정렬)
     */
    fun getAllIncomes(): List<IncomeInfo> {
        return _incomes.value.sortedByDescending { it.transactionDate }
    }
    
    /**
     * 이번 달 수입 합계 (오늘 기준 - 품질 개선)
     */
    fun getCurrentMonthIncome(): Long {
        val today = LocalDate.now()
        val currentMonthIncomes = _incomes.value.filter { income ->
            income.transactionDate.year == today.year &&
            income.transactionDate.monthValue == today.monthValue
        }
        
        val total = currentMonthIncomes.sumOf { it.amount }
        
        // 디버그 로그 추가 (품질 개선)
        android.util.Log.d("IncomeRepository", "이번달 수입 계산: ${currentMonthIncomes.size}건, 총액: ${total}원")
        currentMonthIncomes.forEach { income ->
            android.util.Log.d("IncomeRepository", "  - ${income.description}: ${income.amount}원 (${income.transactionDate})")
        }
        
        return total
    }
    
    /**
     * 특정 월의 수입 합계
     */
    fun getMonthlyIncome(year: Int, month: Int): Long {
        return _incomes.value.filter { income ->
            income.transactionDate.year == year &&
            income.transactionDate.monthValue == month
        }.sumOf { it.amount }
    }
    
    /**
     * 누적 수입 합계 (전체 기간)
     */
    fun getTotalIncome(): Long {
        return _incomes.value.sumOf { it.amount }
    }
    
    /**
     * 월별 수입 통계
     */
    fun getMonthlyIncomeStats(): Map<String, Long> {
        val stats = mutableMapOf<String, Long>()
        
        _incomes.value.forEach { income ->
            val monthKey = "${income.transactionDate.year}-${income.transactionDate.monthValue.toString().padStart(2, '0')}"
            stats[monthKey] = (stats[monthKey] ?: 0) + income.amount
        }
        
        return stats
    }
    
    /**
     * 수입 정보 삭제
     */
    fun removeIncome(income: IncomeInfo) {
        val currentList = _incomes.value.toMutableList()
        currentList.remove(income)
        _incomes.value = currentList
    }
    
    /**
     * 부업 수입만 필터링 (급여가 아닌 수입)
     */
    fun getSideJobIncomes(): List<IncomeInfo> {
        return _incomes.value.filter { income ->
            !income.description.contains("급여") && !income.description.contains("월급")
        }.sortedByDescending { it.transactionDate }
    }
    
    /**
     * 급여 수입만 필터링
     */
    fun getSalaryIncomes(): List<IncomeInfo> {
        return _incomes.value.filter { income ->
            income.description.contains("급여") || income.description.contains("월급")
        }.sortedByDescending { it.transactionDate }
    }
    
    /**
     * 이번 달 부업 수입 합계
     */
    fun getCurrentMonthSideJobIncome(): Long {
        val today = LocalDate.now()
        return _incomes.value.filter { income ->
            income.transactionDate.year == today.year &&
            income.transactionDate.monthValue == today.monthValue &&
            !income.description.contains("급여") && !income.description.contains("월급")
        }.sumOf { it.amount }
    }
    
    /**
     * 이번 달 급여 수입 합계
     */
    fun getCurrentMonthSalaryIncome(): Long {
        val today = LocalDate.now()
        return _incomes.value.filter { income ->
            income.transactionDate.year == today.year &&
            income.transactionDate.monthValue == today.monthValue &&
            (income.description.contains("급여") || income.description.contains("월급"))
        }.sumOf { it.amount }
    }
}
