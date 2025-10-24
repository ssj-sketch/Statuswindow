package com.ssj.statuswindow.repo

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ssj.statuswindow.model.IncomeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 수입 정보 관리 Repository (데이터 영속성 포함)
 */
class IncomeRepository(private val context: Context) {
    
    private val _incomes = MutableStateFlow<List<IncomeInfo>>(emptyList())
    val incomes: StateFlow<List<IncomeInfo>> = _incomes.asStateFlow()
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("income_data", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    init {
        try {
            loadIncomesFromStorage()
        } catch (e: Exception) {
            android.util.Log.e("IncomeRepository", "초기화 중 오류 발생", e)
            _incomes.value = emptyList()
        }
    }
    
    /**
     * 수입 정보 추가 (중복 검사 포함 - 품질 개선)
     */
    fun addIncome(income: IncomeInfo) {
        val currentList = _incomes.value.toMutableList()
        
        // 중복 검사 개선: 같은 날짜, 같은 금액, 같은 은행의 급여는 중복으로 간주
        // 단, 급여의 경우 월별로 구분하여 중복 검사 (품질 개선)
        val isDuplicate = currentList.any { existing ->
            existing.transactionDate?.let { existingDate ->
                income.transactionDate?.let { incomeDate ->
                    existingDate.toLocalDate() == incomeDate.toLocalDate() &&
                    existing.amount == income.amount &&
                    existing.bankName == income.bankName &&
                    existing.description.contains("급여") && income.description.contains("급여") &&
                    existing.source == income.source &&
                    existingDate.year == incomeDate.year &&
                    existingDate.monthValue == incomeDate.monthValue
                } ?: false
            } ?: false
        }
        
        if (!isDuplicate) {
            currentList.add(income)
            _incomes.value = currentList
            
            // 데이터 영구 저장
            saveIncomesToStorage()
            
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
        return _incomes.value.filter { it.transactionDate != null }.sortedByDescending { it.transactionDate }
    }
    
    /**
     * 이번 달 수입 합계 (오늘 기준 - 품질 개선)
     */
    fun getCurrentMonthIncome(): Long {
        return try {
            val today = LocalDate.now()
            val incomes = _incomes.value ?: emptyList()
            
            var total = 0L
            for (income in incomes) {
                try {
                    val transactionDate = income.transactionDate
                    if (transactionDate != null) {
                        val localDate = transactionDate.toLocalDate()
                        if (localDate.year == today.year && localDate.monthValue == today.monthValue) {
                            total += income.amount
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("IncomeRepository", "수입 항목 처리 오류: ${income}", e)
                    // 오류가 발생한 항목은 건너뛰고 계속 진행
                }
            }
            
            android.util.Log.d("IncomeRepository", "이번달 수입 계산 완료: 총액 ${total}원")
            total
        } catch (e: Exception) {
            android.util.Log.e("IncomeRepository", "getCurrentMonthIncome 오류", e)
            0L
        }
    }
    
    /**
     * 특정 월의 수입 합계
     */
    fun getMonthlyIncome(year: Int, month: Int): Long {
        return _incomes.value.filter { income ->
            income.transactionDate?.let { date ->
                date.year == year && date.monthValue == month
            } ?: false
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
            income.transactionDate?.let { date ->
                val monthKey = "${date.year}-${date.monthValue.toString().padStart(2, '0')}"
                stats[monthKey] = (stats[monthKey] ?: 0) + income.amount
            }
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
            income.transactionDate?.let { date ->
                date.year == today.year &&
                date.monthValue == today.monthValue &&
                !income.description.contains("급여") && !income.description.contains("월급")
            } ?: false
        }.sumOf { it.amount }
    }
    
    /**
     * 이번 달 급여 수입 합계
     */
    fun getCurrentMonthSalaryIncome(): Long {
        val today = LocalDate.now()
        return _incomes.value.filter { income ->
            income.transactionDate?.let { date ->
                date.year == today.year &&
                date.monthValue == today.monthValue &&
                (income.description.contains("급여") || income.description.contains("월급"))
            } ?: false
        }.sumOf { it.amount }
    }
    
    /**
     * 데이터를 SharedPreferences에 저장
     */
    private fun saveIncomesToStorage() {
        try {
            val json = gson.toJson(_incomes.value)
            sharedPreferences.edit().putString("incomes", json).apply()
            android.util.Log.d("IncomeRepository", "데이터 저장 완료: ${_incomes.value.size}건")
        } catch (e: Exception) {
            android.util.Log.e("IncomeRepository", "데이터 저장 실패", e)
        }
    }
    
    /**
     * SharedPreferences에서 데이터 로드
     */
    private fun loadIncomesFromStorage() {
        try {
            val json = sharedPreferences.getString("incomes", null)
            if (json != null) {
                val type = object : TypeToken<List<IncomeInfo>>() {}.type
                val loadedIncomes: List<IncomeInfo> = gson.fromJson(json, type)
                _incomes.value = loadedIncomes
                android.util.Log.d("IncomeRepository", "데이터 로드 완료: ${loadedIncomes.size}건")
            } else {
                android.util.Log.d("IncomeRepository", "저장된 데이터 없음")
            }
        } catch (e: Exception) {
            android.util.Log.e("IncomeRepository", "데이터 로드 실패", e)
            _incomes.value = emptyList()
        }
    }
}
