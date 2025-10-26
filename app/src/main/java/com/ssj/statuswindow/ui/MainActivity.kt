package com.ssj.statuswindow.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.util.SmsParser
import com.ssj.statuswindow.model.CardTransaction
import com.ssj.statuswindow.database.StatusWindowDatabase
import com.ssj.statuswindow.database.entity.CardTransactionEntity
import com.ssj.statuswindow.database.entity.CreditCardUsageEntity
import com.ssj.statuswindow.service.MerchantCategoryAiService
import com.ssj.statuswindow.repo.database.SmsDataRepository
import com.ssj.statuswindow.ui.CardTableActivity
import com.ssj.statuswindow.ui.BankTransactionTableActivity
import com.ssj.statuswindow.ui.ButtonTestActivity
import com.ssj.statuswindow.util.NavigationManager
import com.ssj.statuswindow.util.LogManager
import com.ssj.statuswindow.ui.components.AppToolbar
import com.ssj.statuswindow.ui.components.SummaryCard
import com.ssj.statuswindow.ui.components.ProgressBarCard
import com.ssj.statuswindow.ui.components.Buttons
import com.ssj.statuswindow.test.SimpleComponentValidator
import java.text.NumberFormat
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * StatusWindow - 점진적 기능 복원 버전
 */
class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    
    // AppToolbar 컴포넌트
    private lateinit var appToolbar: AppToolbar
    
    // SummaryCard 컴포넌트들
    private lateinit var spendingCard: SummaryCard
    private lateinit var incomeCard: SummaryCard
    
    // ProgressBarCard 컴포넌트
    private lateinit var spendingProgressCard: ProgressBarCard
    
    // 간단한 UI 요소들
    private lateinit var tvSummary: TextView
    
    private val transactions = mutableListOf<CardTransaction>()
    private lateinit var database: StatusWindowDatabase
    private lateinit var categoryAiService: MerchantCategoryAiService
    
    // 브로드캐스트 리시버
    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.ssj.statuswindow.REFRESH_DASHBOARD") {
                LogManager.getInstance().d("MainActivity", "대시보드 새로고침 요청 수신")
                refreshDashboardData()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_main)
            
            // 데이터베이스 초기화
            database = StatusWindowDatabase.getDatabase(this)
            
            // LogManager 초기화
            LogManager.getInstance().initialize(this)
            
            // 카테고리 AI 서비스 초기화 (안전하게 처리)
            try {
                categoryAiService = MerchantCategoryAiService(this)
            } catch (e: Exception) {
                LogManager.getInstance().e("MainActivity", "카테고리 AI 서비스 초기화 실패: ${e.message}", e)
                // AI 서비스 없이도 앱이 동작하도록 처리
            }
            
            // 브로드캐스트 리시버 등록 (API 레벨에 따른 호환성 처리)
            val filter = IntentFilter("com.ssj.statuswindow.REFRESH_DASHBOARD")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(refreshReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(refreshReceiver, filter)
            }
            
            setupViews()
            setupNavigation()
            
            // 앱 시작 시 기존 데이터로 대시보드 초기화
            loadDashboardData()
            
        } catch (e: Exception) {
            e.printStackTrace()
            // 폴백: 간단한 TextView
            val textView = TextView(this)
            textView.text = "오류 발생: ${e.message}"
            setContentView(textView)
        }
    }
    
    private fun setupViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        
        // AppToolbar 초기화 및 설정
        appToolbar = findViewById(R.id.appToolbar)
        appToolbar.setupWithDrawer(this, drawerLayout)
        appToolbar.setTitle("💰 StatusWindow")
        
        // SummaryCard 컴포넌트들 초기화
        spendingCard = findViewById(R.id.spendingCard)
        incomeCard = findViewById(R.id.incomeCard)
        
        // ProgressBarCard 컴포넌트 초기화
        spendingProgressCard = findViewById(R.id.spendingProgressCard)
        
        // SummaryCard 설정
        spendingCard.setTitle("💳 이달 소비")
        spendingCard.setPrimaryValue("0원")
        spendingCard.setSubtitle("이번 달 카드 사용 금액")
        
        incomeCard.setTitle("💰 이달 소득")
        incomeCard.setPrimaryValue("0원")
        incomeCard.setSubtitle("이번 달 입금 금액")
        
        // ProgressBarCard 설정
        spendingProgressCard.setup(
            title = "📊 소비 진척도",
            current = 0L,
            target = 1000000L, // 100만원 목표
            progressType = ProgressBarCard.ProgressType.SPENDING
        )
        
        // 간단한 UI 요소들 초기화
        tvSummary = findViewById(R.id.tvSummary)
        
        // 테스트 버튼들 설정
        setupTestButtons()
    }
    
    private fun setupTestButtons() {
        // 카드 사용내역 테스트 버튼
        findViewById<android.widget.Button>(R.id.btnTestCardDetails).setOnClickListener {
            LogManager.getInstance().d("MainActivity", "카드 사용내역 테스트 버튼 클릭")
            try {
                startActivity(Intent(this, CardDetailsActivity::class.java))
                LogManager.getInstance().d("MainActivity", "CardDetailsActivity 시작 성공")
            } catch (e: Exception) {
                LogManager.getInstance().e("MainActivity", "CardDetailsActivity 시작 실패: ${e.message}", e)
                android.widget.Toast.makeText(this, "카드 사용내역 페이지를 열 수 없습니다: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        // 입출금내역 테스트 버튼
        findViewById<android.widget.Button>(R.id.btnTestBankDetails).setOnClickListener {
            LogManager.getInstance().d("MainActivity", "입출금내역 테스트 버튼 클릭")
            try {
                startActivity(Intent(this, BankTransactionActivity::class.java))
                LogManager.getInstance().d("MainActivity", "BankTransactionActivity 시작 성공")
            } catch (e: Exception) {
                LogManager.getInstance().e("MainActivity", "BankTransactionActivity 시작 실패: ${e.message}", e)
                android.widget.Toast.makeText(this, "입출금내역 페이지를 열 수 없습니다: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        // SMS 테스트 버튼
        findViewById<android.widget.Button>(R.id.btnTestSmsData).setOnClickListener {
            LogManager.getInstance().d("MainActivity", "SMS 테스트 버튼 클릭")
            try {
                startActivity(Intent(this, SmsDataTestActivity::class.java))
                LogManager.getInstance().d("MainActivity", "SmsDataTestActivity 시작 성공")
            } catch (e: Exception) {
                LogManager.getInstance().e("MainActivity", "SmsDataTestActivity 시작 실패: ${e.message}", e)
                android.widget.Toast.makeText(this, "SMS 테스트 페이지를 열 수 없습니다: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        // 카드 테이블 테스트 버튼
        findViewById<android.widget.Button>(R.id.btnTestCardTable).setOnClickListener {
            LogManager.getInstance().d("MainActivity", "카드 테이블 테스트 버튼 클릭")
            try {
                startActivity(Intent(this, CardTableActivity::class.java))
                LogManager.getInstance().d("MainActivity", "CardTableActivity 시작 성공")
            } catch (e: Exception) {
                LogManager.getInstance().e("MainActivity", "CardTableActivity 시작 실패: ${e.message}", e)
                android.widget.Toast.makeText(this, "카드 테이블 페이지를 열 수 없습니다: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        // 버튼 자동 테스트 버튼
        findViewById<android.widget.Button>(R.id.btnButtonTest).setOnClickListener {
            LogManager.getInstance().d("MainActivity", "버튼 자동 테스트 버튼 클릭")
            try {
                startActivity(Intent(this, ButtonTestActivity::class.java))
                LogManager.getInstance().d("MainActivity", "ButtonTestActivity 시작 성공")
            } catch (e: Exception) {
                LogManager.getInstance().e("MainActivity", "ButtonTestActivity 시작 실패: ${e.message}", e)
                android.widget.Toast.makeText(this, "버튼 자동 테스트 페이지를 열 수 없습니다: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        // 로그 뷰어 버튼
        findViewById<android.widget.Button>(R.id.btnLogViewer).setOnClickListener {
            LogManager.getInstance().d("MainActivity", "로그 뷰어 버튼 클릭")
            try {
                startActivity(Intent(this, LogViewerActivity::class.java))
                LogManager.getInstance().d("MainActivity", "LogViewerActivity 시작 성공")
            } catch (e: Exception) {
                LogManager.getInstance().e("MainActivity", "LogViewerActivity 시작 실패: ${e.message}", e)
                android.widget.Toast.makeText(this, "로그 뷰어 페이지를 열 수 없습니다: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        // 통합 테스트 버튼
        findViewById<android.widget.Button>(R.id.btnIntegrationTest).setOnClickListener {
            LogManager.getInstance().d("MainActivity", "통합 테스트 버튼 클릭")
            runIntegrationTest()
        }
    }
    
    private fun setupNavigation() {
        // NavigationManager를 사용한 네비게이션 설정
        NavigationManager.setupNavigation(this, navigationView, drawerLayout, MainActivity::class.java)
    }
    
    /**
     * 컴포넌트 검증 실행
     */
    private fun runIntegrationTest() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                LogManager.getInstance().i("MainActivity", "컴포넌트 검증 시작")
                
                val isValid = SimpleComponentValidator.validateAllComponents(this@MainActivity)
                
                withContext(Dispatchers.Main) {
                    SimpleComponentValidator.showValidationResult(this@MainActivity, isValid)
                }
                
                LogManager.getInstance().i("MainActivity", "컴포넌트 검증 완료: ${if (isValid) "성공" else "실패"}")
            } catch (e: Exception) {
                LogManager.getInstance().e("MainActivity", "컴포넌트 검증 실행 실패: ${e.message}", e)
                
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        this@MainActivity, 
                        "검증 실행 실패: ${e.message}", 
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return super.onSupportNavigateUp()
    }
    
    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("MainActivity", "대시보드 데이터 로드 시작")
                
                // 카드 사용 데이터 로드
                val cardTransactions = withContext(Dispatchers.IO) {
                    database.cardTransactionDao().getAllCardTransactions()
                }
                
                // 은행 거래 데이터 로드
                val bankTransactions = withContext(Dispatchers.IO) {
                    database.bankTransactionDao().getAllBankTransactions().first()
                }
                
                // 대시보드 업데이트
                updateDashboard(cardTransactions, bankTransactions)
                
                android.util.Log.d("MainActivity", "대시보드 데이터 로드 완료")
                
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "대시보드 데이터 로드 실패: ${e.message}", e)
            }
        }
    }
    
    private fun updateDashboard(cardTransactions: List<CardTransactionEntity>, bankTransactions: List<com.ssj.statuswindow.database.entity.BankTransactionEntity>) {
        try {
            val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
            
            // 카드 사용 금액 계산
            val currentMonth = java.time.LocalDate.now().monthValue
            val currentYear = java.time.LocalDate.now().year
            
            val monthlyCardAmount = cardTransactions
                .filter { 
                    it.transactionDate.year == currentYear && it.transactionDate.monthValue == currentMonth
                }
                .sumOf { it.amount }
            
            val monthlyBankAmount = bankTransactions
                .filter { 
                    it.transactionDate.year == currentYear && it.transactionDate.monthValue == currentMonth
                }
                .sumOf { it.amount }
            
            // UI 업데이트
            spendingCard.setPrimaryValue("${formatter.format(monthlyCardAmount)}원")
            incomeCard.setPrimaryValue("${formatter.format(monthlyBankAmount)}원")
            
            // ProgressBarCard 업데이트
            spendingProgressCard.setCurrentValue(monthlyCardAmount)
            spendingProgressCard.updateProgress()
            
            // 요약 정보 업데이트
            val totalTransactions = cardTransactions.size + bankTransactions.size
            tvSummary.text = "📊 파싱 결과 요약\n\n총 거래: ${totalTransactions}건\n카드 거래: ${cardTransactions.size}건\n은행 거래: ${bankTransactions.size}건"
            
            android.util.Log.d("MainActivity", "대시보드 업데이트 완료: 카드=${formatter.format(monthlyCardAmount)}원, 은행=${formatter.format(monthlyBankAmount)}원")
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "대시보드 업데이트 오류: ${e.message}", e)
        }
    }
    
    private fun refreshDashboardData() {
        loadDashboardData()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(refreshReceiver)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "브로드캐스트 리시버 해제 실패: ${e.message}")
        }
    }
}