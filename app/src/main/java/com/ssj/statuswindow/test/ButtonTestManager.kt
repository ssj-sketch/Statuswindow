package com.ssj.statuswindow.test

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.ssj.statuswindow.ui.*

/**
 * 모든 버튼들을 자동으로 테스트하는 매니저
 */
class ButtonTestManager(private val context: Context) {
    
    private val testResults = mutableListOf<TestResult>()
    
    data class TestResult(
        val buttonName: String,
        val activityName: String,
        val success: Boolean,
        val errorMessage: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    data class ButtonInfo(
        val name: String,
        val activityClass: Class<*>,
        val description: String
    )
    
    // 모든 테스트 가능한 버튼 목록
    private val testableButtons = listOf(
        ButtonInfo("카드 사용내역", CardDetailsActivity::class.java, "카드 거래 내역 상세 화면"),
        ButtonInfo("입출금내역", BankTransactionActivity::class.java, "은행 거래 내역 화면"),
        ButtonInfo("SMS 테스트", SmsDataTestActivity::class.java, "SMS 데이터 파싱 테스트 화면"),
        ButtonInfo("카드 테이블", CardTableActivity::class.java, "카드 사용내역 엑셀 테이블 화면"),
        ButtonInfo("카드 이벤트", CardEventActivity::class.java, "카드 이벤트 화면"),
        ButtonInfo("수입 상세", IncomeDetailActivity::class.java, "수입 상세 화면"),
        ButtonInfo("자산 관리", AssetManagementActivity::class.java, "자산 관리 화면"),
        ButtonInfo("설정", SettingsActivity::class.java, "앱 설정 화면"),
        ButtonInfo("디버그 로그", DebugLogActivity::class.java, "디버그 로그 화면")
    )
    
    /**
     * 모든 버튼을 순차적으로 테스트
     */
    fun runAllTests(): List<TestResult> {
        Log.i("ButtonTestManager", "=== 버튼 자동 테스트 시작 ===")
        testResults.clear()
        
        testableButtons.forEach { buttonInfo ->
            testButton(buttonInfo)
            // 각 테스트 사이에 잠시 대기
            Thread.sleep(1000)
        }
        
        Log.i("ButtonTestManager", "=== 버튼 자동 테스트 완료 ===")
        printTestSummary()
        
        return testResults.toList()
    }
    
    /**
     * 개별 버튼 테스트
     */
    private fun testButton(buttonInfo: ButtonInfo) {
        Log.d("ButtonTestManager", "테스트 중: ${buttonInfo.name}")
        
        try {
            val intent = Intent(context, buttonInfo.activityClass)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            
            testResults.add(TestResult(
                buttonName = buttonInfo.name,
                activityName = buttonInfo.activityClass.simpleName,
                success = true
            ))
            
            Log.i("ButtonTestManager", "✅ 성공: ${buttonInfo.name} -> ${buttonInfo.activityClass.simpleName}")
            
        } catch (e: Exception) {
            val errorMsg = "오류: ${e.message}"
            testResults.add(TestResult(
                buttonName = buttonInfo.name,
                activityName = buttonInfo.activityClass.simpleName,
                success = false,
                errorMessage = errorMsg
            ))
            
            Log.e("ButtonTestManager", "❌ 실패: ${buttonInfo.name} -> $errorMsg", e)
        }
    }
    
    /**
     * 테스트 결과 요약 출력
     */
    private fun printTestSummary() {
        val totalTests = testResults.size
        val successCount = testResults.count { it.success }
        val failureCount = totalTests - successCount
        
        Log.i("ButtonTestManager", "=== 테스트 결과 요약 ===")
        Log.i("ButtonTestManager", "총 테스트: $totalTests")
        Log.i("ButtonTestManager", "성공: $successCount")
        Log.i("ButtonTestManager", "실패: $failureCount")
        Log.i("ButtonTestManager", "성공률: ${(successCount * 100 / totalTests)}%")
        
        // 실패한 테스트들 상세 출력
        val failures = testResults.filter { !it.success }
        if (failures.isNotEmpty()) {
            Log.w("ButtonTestManager", "=== 실패한 테스트들 ===")
            failures.forEach { result ->
                Log.w("ButtonTestManager", "❌ ${result.buttonName}: ${result.errorMessage}")
            }
        }
    }
    
    /**
     * 특정 버튼만 테스트
     */
    fun testSpecificButton(buttonName: String): TestResult? {
        val buttonInfo = testableButtons.find { it.name == buttonName }
        return if (buttonInfo != null) {
            testButton(buttonInfo)
            testResults.lastOrNull()
        } else {
            Log.w("ButtonTestManager", "버튼을 찾을 수 없음: $buttonName")
            null
        }
    }
    
    /**
     * 테스트 가능한 모든 버튼 목록 반환
     */
    fun getTestableButtons(): List<ButtonInfo> = testableButtons.toList()
    
    /**
     * 최근 테스트 결과 반환
     */
    fun getLastTestResults(): List<TestResult> = testResults.toList()
}

