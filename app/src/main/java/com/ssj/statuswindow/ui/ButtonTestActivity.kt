package com.ssj.statuswindow.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.test.ButtonTestManager
import com.ssj.statuswindow.util.NavigationManager
import com.ssj.statuswindow.ui.components.AppToolbar
import kotlinx.coroutines.*

/**
 * 버튼 자동 테스트 화면
 */
class ButtonTestActivity : AppCompatActivity() {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var appToolbar: AppToolbar
    private lateinit var navigationView: NavigationView
    private lateinit var btnRunAllTests: Button
    private lateinit var btnClearResults: Button
    private lateinit var tvTestSummary: TextView
    private lateinit var layoutButtonList: LinearLayout
    private lateinit var tvDetailedResults: TextView
    
    private lateinit var buttonTestManager: ButtonTestManager
    private val testButtons = mutableListOf<Button>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_button_test)
        
        setupViews()
        setupToolbar()
        setupNavigation()
        setupButtonTestManager()
        setupClickListeners()
        createIndividualTestButtons()
    }
    
    private fun setupViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        appToolbar = findViewById(R.id.appToolbar)
        
        // AppToolbar 설정
        appToolbar.setupWithDrawer(this, drawerLayout)
        appToolbar.setTitle("버튼 테스트")
        navigationView = findViewById(R.id.navigationView)
        btnRunAllTests = findViewById(R.id.btnRunAllTests)
        btnClearResults = findViewById(R.id.btnClearResults)
        tvTestSummary = findViewById(R.id.tvTestSummary)
        layoutButtonList = findViewById(R.id.layoutButtonList)
        tvDetailedResults = findViewById(R.id.tvDetailedResults)
    }
    
    private fun setupToolbar() {
        // AppToolbar는 이미 setupViews에서 설정됨
        // 기존 toolbar 관련 코드는 제거
    }
    
    private fun setupNavigation() {
        NavigationManager.setupNavigation(this, navigationView, drawerLayout, ButtonTestActivity::class.java)
        NavigationManager.setActiveMenuItem(navigationView, ButtonTestActivity::class.java)
    }
    
    private fun setupButtonTestManager() {
        buttonTestManager = ButtonTestManager(this)
    }
    
    private fun setupClickListeners() {
        btnRunAllTests.setOnClickListener {
            runAllTests()
        }
        
        btnClearResults.setOnClickListener {
            clearResults()
        }
    }
    
    /**
     * 개별 테스트 버튼들 생성
     */
    private fun createIndividualTestButtons() {
        val testableButtons = buttonTestManager.getTestableButtons()
        
        testableButtons.forEach { buttonInfo ->
            val button = Button(this).apply {
                text = "테스트: ${buttonInfo.name}"
                textSize = 14f
                setOnClickListener {
                    testSpecificButton(buttonInfo.name)
                }
            }
            
            val description = TextView(this).apply {
                text = "  → ${buttonInfo.description}"
                textSize = 12f
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
            }
            
            layoutButtonList.addView(button)
            layoutButtonList.addView(description)
            
            testButtons.add(button)
        }
    }
    
    /**
     * 모든 버튼 테스트 실행
     */
    private fun runAllTests() {
        btnRunAllTests.isEnabled = false
        btnRunAllTests.text = "테스트 실행 중..."
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 백그라운드에서 테스트 실행
                val results = withContext(Dispatchers.IO) {
                    buttonTestManager.runAllTests()
                }
                
                // 결과 업데이트
                updateTestResults(results)
                
            } catch (e: Exception) {
                android.util.Log.e("ButtonTestActivity", "테스트 실행 오류: ${e.message}", e)
                Toast.makeText(this@ButtonTestActivity, "테스트 실행 중 오류 발생: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                btnRunAllTests.isEnabled = true
                btnRunAllTests.text = "🚀 모든 버튼 테스트 실행"
            }
        }
    }
    
    /**
     * 특정 버튼 테스트
     */
    private fun testSpecificButton(buttonName: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    buttonTestManager.testSpecificButton(buttonName)
                }
                
                if (result != null) {
                    val message = if (result.success) {
                        "✅ ${result.buttonName} 테스트 성공"
                    } else {
                        "❌ ${result.buttonName} 테스트 실패: ${result.errorMessage}"
                    }
                    
                    Toast.makeText(this@ButtonTestActivity, message, Toast.LENGTH_SHORT).show()
                    updateDetailedResults()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("ButtonTestActivity", "개별 테스트 오류: ${e.message}", e)
                Toast.makeText(this@ButtonTestActivity, "테스트 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 테스트 결과 업데이트
     */
    private fun updateTestResults(results: List<ButtonTestManager.TestResult>) {
        val totalTests = results.size
        val successCount = results.count { it.success }
        val failureCount = totalTests - successCount
        val successRate = if (totalTests > 0) (successCount * 100 / totalTests) else 0
        
        tvTestSummary.text = "테스트 결과: 성공 $successCount/$totalTests (${successRate}%)"
        
        // 실패한 테스트가 있으면 로그에 기록
        if (failureCount > 0) {
            android.util.Log.w("ButtonTestActivity", "테스트 실패: ${failureCount}개")
        }
        
        updateDetailedResults()
    }
    
    /**
     * 상세 결과 업데이트
     */
    private fun updateDetailedResults() {
        val results = buttonTestManager.getLastTestResults()
        
        val resultText = buildString {
            appendLine("=== 버튼 테스트 상세 결과 ===")
            appendLine("테스트 시간: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
            appendLine()
            
            if (results.isEmpty()) {
                appendLine("아직 테스트가 실행되지 않았습니다.")
            } else {
                val successResults = results.filter { it.success }
                val failureResults = results.filter { !it.success }
                
                appendLine("✅ 성공한 테스트 (${successResults.size}개):")
                successResults.forEach { result ->
                    appendLine("  • ${result.buttonName} -> ${result.activityName}")
                }
                
                if (failureResults.isNotEmpty()) {
                    appendLine()
                    appendLine("❌ 실패한 테스트 (${failureResults.size}개):")
                    failureResults.forEach { result ->
                        appendLine("  • ${result.buttonName} -> ${result.errorMessage}")
                    }
                }
            }
        }
        
        tvDetailedResults.text = resultText
    }
    
    /**
     * 결과 초기화
     */
    private fun clearResults() {
        tvTestSummary.text = "테스트 결과: 대기 중..."
        tvDetailedResults.text = "테스트 결과가 여기에 표시됩니다..."
        Toast.makeText(this, "결과가 초기화되었습니다.", Toast.LENGTH_SHORT).show()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(navigationView)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
