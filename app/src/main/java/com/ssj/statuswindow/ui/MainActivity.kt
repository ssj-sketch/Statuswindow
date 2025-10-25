package com.ssj.statuswindow.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
import java.text.NumberFormat
import java.util.*
import kotlinx.coroutines.*

/**
 * StatusWindow - 점진적 기능 복원 버전
 */
class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar
    private lateinit var navigationView: NavigationView
    private lateinit var tvMonthlySpending: TextView
    private lateinit var tvMonthlyIncome: TextView
    private lateinit var tvIncomeChange: TextView
    private lateinit var tvIncomeChangePercent: TextView
    private lateinit var progressSpending: ProgressBar
    private lateinit var tvProgressPercent: TextView
    private lateinit var tvSummary: TextView
    
    private val transactions = mutableListOf<CardTransaction>()
    private lateinit var database: StatusWindowDatabase
    private lateinit var categoryAiService: MerchantCategoryAiService
    
    // 브로드캐스트 리시버
    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.ssj.statuswindow.REFRESH_DASHBOARD") {
                android.util.Log.d("MainActivity", "대시보드 새로고침 요청 수신")
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
            
            // 카테고리 AI 서비스 초기화 (안전하게 처리)
            try {
                categoryAiService = MerchantCategoryAiService(this)
            } catch (e: Exception) {
                Log.e("MainActivity", "카테고리 AI 서비스 초기화 실패: ${e.message}")
                // AI 서비스 없이도 앱이 동작하도록 처리
            }
            
            // 앱 시작 시 기존 데이터 초기화 (선택사항)
            // clearAllData()
            
            // 브로드캐스트 리시버 등록 (API 레벨에 따른 호환성 처리)
            val filter = IntentFilter("com.ssj.statuswindow.REFRESH_DASHBOARD")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(refreshReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(refreshReceiver, filter)
            }
            
            setupViews()
            setupToolbar()
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
        toolbar = findViewById(R.id.toolbar)
        navigationView = findViewById(R.id.navigationView)
        tvMonthlySpending = findViewById(R.id.tvMonthlySpending)
        tvMonthlyIncome = findViewById(R.id.tvMonthlyIncome)
        tvIncomeChange = findViewById(R.id.tvIncomeChange)
        tvIncomeChangePercent = findViewById(R.id.tvIncomeChangePercent)
        progressSpending = findViewById(R.id.progressSpending)
        tvProgressPercent = findViewById(R.id.tvProgressPercent)
        tvSummary = findViewById(R.id.tvSummary)
        
        // 테스트 버튼들 설정
        setupTestButtons()
    }
    
    private fun setupTestButtons() {
        // 카드 사용내역 테스트 버튼
        findViewById<android.widget.Button>(R.id.btnTestCardDetails).setOnClickListener {
            android.util.Log.d("MainActivity", "카드 사용내역 테스트 버튼 클릭")
            try {
                startActivity(Intent(this, CardDetailsActivity::class.java))
                android.util.Log.d("MainActivity", "CardDetailsActivity 시작 성공")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "CardDetailsActivity 시작 실패: ${e.message}", e)
                android.widget.Toast.makeText(this, "카드 사용내역 페이지를 열 수 없습니다: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        // 입출금내역 테스트 버튼
        findViewById<android.widget.Button>(R.id.btnTestBankDetails).setOnClickListener {
            android.util.Log.d("MainActivity", "입출금내역 테스트 버튼 클릭")
            try {
                startActivity(Intent(this, BankTransactionActivity::class.java))
                android.util.Log.d("MainActivity", "BankTransactionActivity 시작 성공")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "BankTransactionActivity 시작 실패: ${e.message}", e)
                android.widget.Toast.makeText(this, "입출금내역 페이지를 열 수 없습니다: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        // SMS 테스트 버튼
        findViewById<android.widget.Button>(R.id.btnTestSmsData).setOnClickListener {
            android.util.Log.d("MainActivity", "SMS 테스트 버튼 클릭")
            try {
                startActivity(Intent(this, SmsDataTestActivity::class.java))
                android.util.Log.d("MainActivity", "SmsDataTestActivity 시작 성공")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "SmsDataTestActivity 시작 실패: ${e.message}", e)
                android.widget.Toast.makeText(this, "SMS 테스트 페이지를 열 수 없습니다: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        // 카드 테이블 테스트 버튼
        findViewById<android.widget.Button>(R.id.btnTestCardTable).setOnClickListener {
            android.util.Log.d("MainActivity", "카드 테이블 테스트 버튼 클릭")
            try {
                startActivity(Intent(this, CardTableActivity::class.java))
                android.util.Log.d("MainActivity", "CardTableActivity 시작 성공")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "CardTableActivity 시작 실패: ${e.message}", e)
                android.widget.Toast.makeText(this, "카드 테이블 페이지를 열 수 없습니다: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        // 버튼 자동 테스트 버튼
        findViewById<android.widget.Button>(R.id.btnButtonTest).setOnClickListener {
            android.util.Log.d("MainActivity", "버튼 자동 테스트 버튼 클릭")
            try {
                startActivity(Intent(this, ButtonTestActivity::class.java))
                android.util.Log.d("MainActivity", "ButtonTestActivity 시작 성공")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "ButtonTestActivity 시작 실패: ${e.message}", e)
                android.widget.Toast.makeText(this, "버튼 테스트 페이지를 열 수 없습니다: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
    }
    
    private fun setupNavigation() {
        NavigationManager.setupNavigation(this, navigationView, drawerLayout, MainActivity::class.java)
        NavigationManager.setActiveMenuItem(navigationView, MainActivity::class.java)
    }
    
    /**
     * 카드사용내역 테이블 표시
     */
    private fun showCardUsageTable() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("MainActivity", "=== 카드사용내역 테이블 조회 시작 ===")
                
                val cardTransactionDao = database.cardTransactionDao()
                val allCardTransactions = cardTransactionDao.getAllCardTransactions()
                
                withContext(Dispatchers.Main) {
                    displayCardUsageTable(allCardTransactions)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "카드사용내역 테이블 조회 오류: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    tvSummary.text = "❌ 카드사용내역 테이블 조회 오류: ${e.message}"
                }
            }
        }
    }
    
    /**
     * 입출금내역 테이블 표시
     */
    private fun showBankTransactionTable() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("MainActivity", "=== 입출금내역 테이블 조회 시작 ===")
                
                val bankTransactionDao = database.bankTransactionDao()
                val allBankTransactions = bankTransactionDao.getAllBankTransactions()
                
                allBankTransactions.collect { bankList ->
                    withContext(Dispatchers.Main) {
                        displayBankTransactionTable(bankList)
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "입출금내역 테이블 조회 오류: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    tvSummary.text = "❌ 입출금내역 테이블 조회 오류: ${e.message}"
                }
            }
        }
    }
    
    /**
     * 카드사용내역 테이블 데이터 표시
     */
    private fun displayCardUsageTable(cardList: List<CardTransactionEntity>) {
        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("MM/dd HH:mm")
        
        val tableText = StringBuilder()
        tableText.append("💳 카드사용내역 테이블\n")
        tableText.append("=".repeat(50)).append("\n\n")
        
        if (cardList.isEmpty()) {
            tableText.append("저장된 카드사용내역이 없습니다.\n")
            tableText.append("테스트 데이터 관리에서 데이터를 입력해주세요.")
        } else {
            // 테이블 헤더
            tableText.append("ID | 카드번호 | 거래타입 | 금액 | 할부 | 가맹점 | 거래일시\n")
            tableText.append("-".repeat(80)).append("\n")
            
            // 테이블 데이터
            cardList.forEach { card ->
                tableText.append("${card.id} | ")
                tableText.append("${card.cardNumber} | ")
                tableText.append("${card.transactionType} | ")
                tableText.append("${formatter.format(card.amount)}원 | ")
                tableText.append("${card.installment} | ")
                tableText.append("${card.merchant} | ")
                tableText.append("${card.transactionDate.format(dateFormatter)}\n")
            }
            
            // 통계 정보
            tableText.append("\n📊 통계 정보\n")
            tableText.append("-".repeat(30)).append("\n")
            tableText.append("총 거래 건수: ${cardList.size}건\n")
            
            val totalAmount = cardList.sumOf { it.amount }
            tableText.append("총 사용금액: ${formatter.format(totalAmount)}원\n")
            
            // 거래타입별 통계
            val typeStats = cardList.groupBy { it.transactionType }
            tableText.append("\n💳 거래타입별 통계\n")
            tableText.append("-".repeat(30)).append("\n")
            
            typeStats.forEach { (type, transactions) ->
                val typeTotalAmount = transactions.sumOf { it.amount }
                tableText.append("${type}: ${transactions.size}건, ${formatter.format(typeTotalAmount)}원\n")
            }
        }
        
        tvSummary.text = tableText.toString()
        android.util.Log.d("MainActivity", "=== 카드사용내역 테이블 표시 완료 ===")
    }
    
    /**
     * 입출금내역 테이블 데이터 표시
     */
    private fun displayBankTransactionTable(bankList: List<com.ssj.statuswindow.database.entity.BankTransactionEntity>) {
        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("MM/dd HH:mm")
        
        val tableText = StringBuilder()
        tableText.append("🏦 입출금내역 테이블\n")
        tableText.append("=".repeat(50)).append("\n\n")
        
        if (bankList.isEmpty()) {
            tableText.append("저장된 입출금내역이 없습니다.\n")
            tableText.append("테스트 데이터 관리에서 데이터를 입력해주세요.")
        } else {
            // 테이블 헤더
            tableText.append("ID | 계좌번호 | 거래타입 | 금액 | 잔액 | 메모 | 거래일시\n")
            tableText.append("-".repeat(80)).append("\n")
            
            // 테이블 데이터
            bankList.forEach { bank ->
                tableText.append("${bank.id} | ")
                tableText.append("${bank.accountNumber} | ")
                tableText.append("${bank.transactionType} | ")
                tableText.append("${formatter.format(bank.amount)}원 | ")
                tableText.append("${formatter.format(bank.balance)}원 | ")
                tableText.append("${bank.memo} | ")
                tableText.append("${bank.transactionDate.format(dateFormatter)}\n")
            }
            
            // 통계 정보
            tableText.append("\n📊 통계 정보\n")
            tableText.append("-".repeat(30)).append("\n")
            tableText.append("총 거래 건수: ${bankList.size}건\n")
            
            val totalAmount = bankList.sumOf { it.amount }
            tableText.append("총 거래금액: ${formatter.format(totalAmount)}원\n")
            
            // 거래타입별 통계
            val typeStats = bankList.groupBy { it.transactionType }
            tableText.append("\n🏦 거래타입별 통계\n")
            tableText.append("-".repeat(30)).append("\n")
            
            typeStats.forEach { (type, transactions) ->
                val typeTotalAmount = transactions.sumOf { it.amount }
                tableText.append("${type}: ${transactions.size}건, ${formatter.format(typeTotalAmount)}원\n")
            }
        }
        
        tvSummary.text = tableText.toString()
        android.util.Log.d("MainActivity", "=== 입출금내역 테이블 표시 완료 ===")
    }
    
    
    
    private fun clearAllDataAndTest() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("MainActivity", "=== 데이터 삭제 시작 ===")
                
                // 모든 데이터 삭제 (강력한 삭제)
                val cardTransactionDao = database.cardTransactionDao()
                val creditCardUsageDao = database.creditCardUsageDao()
                val bankTransactionDao = database.bankTransactionDao()
                
                // 삭제 전 개수 확인
                val cardCountBefore = cardTransactionDao.getCardTransactionCount()
                val creditCountBefore = creditCardUsageDao.getCreditCardUsageCount()
                val bankCountBefore = bankTransactionDao.getBankTransactionCount()
                
                android.util.Log.d("MainActivity", "삭제 전 개수 - 카드: $cardCountBefore, 신용카드: $creditCountBefore, 은행: $bankCountBefore")
                
                // 모든 데이터 삭제
                cardTransactionDao.deleteAllCardTransactions()
                creditCardUsageDao.deleteAllCreditCardUsage()
                bankTransactionDao.deleteAllBankTransactions()
                
                // 삭제 후 개수 확인
                val cardCountAfter = cardTransactionDao.getCardTransactionCount()
                val creditCountAfter = creditCardUsageDao.getCreditCardUsageCount()
                val bankCountAfter = bankTransactionDao.getBankTransactionCount()
                
                android.util.Log.d("MainActivity", "삭제 후 개수 - 카드: $cardCountAfter, 신용카드: $creditCountAfter, 은행: $bankCountAfter")
                
                withContext(Dispatchers.Main) {
                    // 메모리 기반 데이터 초기화
                    transactions.clear()
                    
                    // UI 완전 초기화
                    updateSummary("📊 파싱 결과 요약\n\n총 거래: 0건\n카드 거래: 0건\n은행 거래: 0건\n소득 거래: 0건")
                    updateDashboard(0L, 0L, 0L)
                    updateIncomeDashboard(0L, 0L)
                    
                    val message = "기존 데이터가 삭제되었습니다.\n삭제된 데이터: 카드 ${cardCountBefore}건, 신용카드 ${creditCountBefore}건, 은행 ${bankCountBefore}건"
                    android.widget.Toast.makeText(this@MainActivity, message, android.widget.Toast.LENGTH_LONG).show()
                    
                    android.util.Log.d("MainActivity", "=== 데이터 삭제 완료, 샘플 테스트 시작 ===")
                    executeTestSmsParsing()
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("MainActivity", "데이터 삭제 오류: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@MainActivity, "데이터 삭제 오류: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun executeTestSmsParsing() {
        try {
            // 수정된 실제 SMS 샘플 데이터들 (할부 거래 포함)
            val testSmsList = listOf(
                "신한카드(1054)승인 신*진 42,820원(일시불)10/22 14:59 주식회사 이마트 누적1,903,674",
                "신한카드(1054)승인 신*진 98,700원(2개월)10/22 15:48 카톨릭대병원 누적1,960,854원",
                "신한카드(1054)취소 신*진 12,700원(일시불)10/22 15:48 스타벅스 누적1,860,854원",
                "신한카드(1054)승인 신*진 12,700원(일시불)10/22 15:48 스타벅스 누적1,860,854원",
                "신한카드(1054)승인 신*진 42,820원(일시불)10/21 14:59 주식회사 이마트 누적1,903,674",
                "신한카드(1054)승인 신*진 98,700원(3개월)10/21 15:48 카톨릭대병원 누적1,960,854원",
                "신한카드(1054)승인 신*진 12,700원(일시불)10/21 15:48 스타벅스 누적1,860,854원",
                "신한 10/11 21:54 100-***-159993 입금  2,500,000 잔액  3,700,000 급여",
                "신한 10/11 21:54 100-***-159993 출금  3,500,000 잔액  1,200,000 신한카드",
                "신한 09/11 21:54 100-***-159993 입금  2,500,000 잔액  5,000,000 신승진",
                "신한 08/11 21:54 100-***-159993 입금  2,500,000 잔액  2,500,000 급여"
            )
            
            parseSmsData(testSmsList.joinToString("\n"))
            
        } catch (e: Exception) {
            e.printStackTrace()
            updateSummary("❌ 오류 발생: ${e.message}")
        }
    }
    
    
    private fun parseSmsData(smsText: String) {
        try {
            // SMS 파싱
            val parsedTransactions = SmsParser.parseSmsText(smsText, 0)
            
            // 기존 데이터 초기화
            transactions.clear()
            transactions.addAll(parsedTransactions)
            
                    // 어댑터 업데이트 (제거됨 - 상세페이지로 이동)
            
            // 소득 정보도 파싱
            val parsedIncome = SmsParser.parseIncomeFromSms(smsText)
            
            // 요약 정보 업데이트
            updateSummary(parsedTransactions, parsedIncome)
            
            // Room DB에 저장
            saveTransactionsToDatabase(parsedTransactions, parsedIncome)
            
        } catch (e: Exception) {
            e.printStackTrace()
            updateSummary("❌ 파싱 오류: ${e.message}")
        }
    }
    
    private fun saveTransactionsToDatabase(transactions: List<CardTransaction>, incomeTransactions: List<com.ssj.statuswindow.database.entity.BankTransactionEntity>) {
        // 메모리 누수 방지를 위해 lifecycleScope 사용
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cardTransactionDao = database.cardTransactionDao()
                val creditCardUsageDao = database.creditCardUsageDao()
                
                // 기존 CardTransactionEntity 저장 (간단한 중복 검사)
                val cardEntities = transactions.filter { transaction ->
                    // 간단한 중복 검사 - 원본 텍스트 기준
                    val existingCount = 0 // 임시로 중복 검사 비활성화
                    val isDuplicate = existingCount > 0
                    
                    if (isDuplicate) {
                        Log.d("MainActivity", "🚫 중복 거래 차단: ${transaction.merchant} - ${transaction.amount}원 (${transaction.transactionType})")
                    } else {
                        Log.d("MainActivity", "✅ 신규 거래 추가: ${transaction.merchant} - ${transaction.amount}원 (${transaction.transactionType})")
                    }
                    
                    !isDuplicate
                }.map { transaction ->
                    CardTransactionEntity(
                        id = 0, // Room이 자동 생성
                        cardType = transaction.cardType,
                        cardNumber = transaction.cardNumber,
                        transactionType = transaction.transactionType,
                        user = transaction.user,
                        amount = transaction.amount,
                        installment = transaction.installment,
                        transactionDate = transaction.transactionDate,
                        merchant = transaction.merchant,
                        cumulativeAmount = transaction.cumulativeAmount,
                        category = transaction.category,
                        memo = transaction.memo,
                        originalText = transaction.originalText
                    )
                }
                
                // 신용카드 사용내역만 별도 테이블에 저장 (간단한 중복 검사)
                val creditCardEntities = transactions.filter { it.cardType.contains("카드") }.filter { transaction ->
                    // 신용카드 테이블에서도 중복 검사 - 원본 텍스트 기준
                    val existingCount = 0 // 임시로 중복 검사 비활성화
                    val isDuplicate = existingCount > 0
                    
                    if (isDuplicate) {
                        Log.d("MainActivity", "🚫 신용카드 테이블 중복 차단: ${transaction.merchant} - ${transaction.amount}원")
                    } else {
                        Log.d("MainActivity", "✅ 신용카드 테이블 신규 추가: ${transaction.merchant} - ${transaction.amount}원")
                    }
                    
                    !isDuplicate
                }.map { transaction ->
                    val installmentMonths = when {
                        transaction.installment == "일시불" -> 1
                        transaction.installment.contains("개월") -> {
                            transaction.installment.replace("개월", "").toIntOrNull() ?: 1
                        }
                        else -> 1
                    }
                    
                    val monthlyPayment = when {
                        transaction.transactionType == "취소" -> -transaction.amount
                        transaction.installment == "일시불" -> transaction.amount
                        else -> transaction.amount / installmentMonths
                    }
                    
                    // AI로 카테고리 추론 (한국어 기본) - 안전하게 처리
                    val inferredCategory = try {
                        if (::categoryAiService.isInitialized) {
                            categoryAiService.inferCategory(transaction.merchant, "ko")
                        } else {
                            "기타" // 기본값
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "카테고리 추론 실패: ${e.message}")
                        "기타" // 기본값
                    }
                    
                    // 청구년월 계산 (거래일 기준)
                    val billingYear = transaction.transactionDate.year
                    val billingMonth = transaction.transactionDate.monthValue
                    
                    CreditCardUsageEntity(
                        id = 0, // Room이 자동 생성
                        cardType = transaction.cardType,
                        cardNumber = transaction.cardNumber,
                        cardName = "${transaction.cardType}${transaction.cardNumber}",
                        transactionType = transaction.transactionType,
                        amount = transaction.amount,
                        installment = transaction.installment,
                        installmentMonths = installmentMonths,
                        monthlyPayment = monthlyPayment,
                        transactionDate = transaction.transactionDate,
                        merchant = transaction.merchant,
                        merchantCategory = inferredCategory, // AI 추론된 카테고리
                        billingYear = billingYear, // 청구년도
                        billingMonth = billingMonth, // 청구월
                        billingAmount = monthlyPayment, // 해당월 청구금액
                        cumulativeAmount = transaction.cumulativeAmount,
                        monthlyBillAmount = monthlyPayment,
                        user = transaction.user,
                        originalText = transaction.originalText
                    )
                }
                
                // 소득 데이터도 저장 (간단한 중복 검사)
                val bankTransactionDao = database.bankTransactionDao()
                val filteredIncomeTransactions = incomeTransactions.filter { incomeTransaction ->
                    // 소득 데이터 중복 검사 - 원본 텍스트 기준
                    val existingCount = 0 // 임시로 중복 검사 비활성화
                    val isDuplicate = existingCount > 0
                    
                    if (isDuplicate) {
                        Log.d("MainActivity", "🚫 소득 데이터 중복 차단: ${incomeTransaction.description} - ${incomeTransaction.amount}원")
                    } else {
                        Log.d("MainActivity", "✅ 소득 데이터 신규 추가: ${incomeTransaction.description} - ${incomeTransaction.amount}원")
                    }
                    
                    !isDuplicate
                }
                
                bankTransactionDao.insertBankTransactionList(filteredIncomeTransactions)
                
                // DB에 저장
                cardTransactionDao.insertCardTransactions(cardEntities)
                creditCardUsageDao.insertCreditCardUsageList(creditCardEntities)
                
                // 메인 스레드에서 성공 메시지 표시
                withContext(Dispatchers.Main) {
                    val currentSummary = tvSummary.text.toString()
                    val duplicateCount = transactions.size - cardEntities.size
                    val duplicateIncomeCount = incomeTransactions.size - filteredIncomeTransactions.size
                    
                    val message = if (duplicateCount > 0 || duplicateIncomeCount > 0) {
                        "💾 DB 저장 완료: ${cardEntities.size}건 (카드: ${creditCardEntities.size}건, 소득: ${filteredIncomeTransactions.size}건)\n🚫 중복 차단: 카드 ${duplicateCount}건, 소득 ${duplicateIncomeCount}건"
                } else {
                        "💾 DB 저장 완료: ${cardEntities.size}건 (카드: ${creditCardEntities.size}건, 소득: ${filteredIncomeTransactions.size}건)"
                    }
                    
                    tvSummary.text = "$currentSummary\n\n$message"
                    
                    // DB 저장 후 대시보드 새로고침
                    loadDashboardData()
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val currentSummary = tvSummary.text.toString()
                    tvSummary.text = "$currentSummary\n\n❌ DB 저장 실패: ${e.message}"
                }
            }
        }
    }
    
    private fun updateSummary(transactions: List<CardTransaction>, incomeTransactions: List<com.ssj.statuswindow.database.entity.BankTransactionEntity>) {
        val totalCount = transactions.size
        val cardTransactions = transactions.filter { it.cardType.contains("카드") }
        val bankTransactions = transactions.filter { !it.cardType.contains("카드") }
        
        val summary = StringBuilder()
        summary.append("📊 파싱 결과 요약\n\n")
        summary.append("총 거래: ${totalCount}건\n")
        summary.append("카드 거래: ${cardTransactions.size}건\n")
        summary.append("은행 거래: ${bankTransactions.size}건\n")
        summary.append("소득 거래: ${incomeTransactions.size}건\n\n")
        
        if (cardTransactions.isNotEmpty()) {
            // DB에서 직접 계산 (쿼리 합산)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val cardTransactionDao = database.cardTransactionDao()
                    
                    // 현재 월의 시작과 끝 날짜 계산
                    val now = java.time.LocalDateTime.now()
                    val startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                    val endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59)
                    
                    // DB 쿼리로 계산
                    val totalAmount = cardTransactionDao.getTotalCardUsageAmount(startOfMonth, endOfMonth) ?: 0L
                    val monthlyBillAmount = cardTransactionDao.getMonthlyBillAmount(startOfMonth, endOfMonth) ?: 0L
                    
                    // 전월 카드 사용금액 조회
                    val lastMonth = now.minusMonths(1)
                    val startOfLastMonth = lastMonth.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                    val endOfLastMonth = lastMonth.withDayOfMonth(lastMonth.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59)
                    val lastMonthCardAmount = cardTransactionDao.getTotalCardUsageAmount(startOfLastMonth, endOfLastMonth) ?: 0L
                    
                            // 메인 스레드에서 UI 업데이트
                            withContext(Dispatchers.Main) {
                                val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
                                summary.append("카드사용 총액: ${formatter.format(totalAmount)}원 (DB 쿼리)\n")
                                summary.append("이번달 청구금액: ${formatter.format(monthlyBillAmount)}원 (DB 쿼리)\n")
                                summary.append("파싱된 거래 수: ${cardTransactions.size}건\n")
                                
                                // 소득 정보도 DB에서 조회 (입금만)
                                val bankTransactionDao = database.bankTransactionDao()
                                val totalIncome = bankTransactionDao.getTotalDepositAmount(startOfMonth, endOfMonth) ?: 0L
                                summary.append("소득 총액: ${formatter.format(totalIncome)}원 (입금만, DB 쿼리)\n\n")
                                
                                // 각 거래별 상세 정보 표시 (메모리 계산으로 비교)
                                summary.append("=== 거래 상세 (메모리 계산) ===\n")
                                cardTransactions.forEachIndexed { index, transaction ->
                                    val billAmount = calculateMonthlyBillAmount(transaction)
                                    summary.append("${index + 1}. ${transaction.merchant} - ${transaction.transactionType} - ${transaction.installment} - ${formatter.format(transaction.amount)}원 → ${formatter.format(billAmount)}원\n")
                                }
                                
                                tvSummary.text = summary.toString()
                                
                                // 대시보드 업데이트 추가 (DB에서 읽어온 값 사용)
                                updateDashboard(monthlyBillAmount, totalAmount, lastMonthCardAmount)
                                
                                // 소득금액도 업데이트 (입출금내역에서 입금만)
                                val currentMonthIncome = totalIncome
                                val lastMonth = java.time.LocalDateTime.now().minusMonths(1)
                                val startOfLastMonth = lastMonth.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                                val endOfLastMonth = lastMonth.withDayOfMonth(lastMonth.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59)
                                val lastMonthIncome = bankTransactionDao.getTotalDepositAmount(startOfLastMonth, endOfLastMonth) ?: 0L
                                updateIncomeDashboard(currentMonthIncome, lastMonthIncome)
                            }
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        summary.append("❌ DB 쿼리 오류: ${e.message}")
                        tvSummary.text = summary.toString()
                    }
                }
                }
            } else {
            tvSummary.text = summary.toString()
        }
    }
    
    private fun updateSummary(message: String) {
        tvSummary.text = message
    }
    
    /**
     * 신용카드 테이블 표시
     */
    
    /**
     * 신용카드 테이블 데이터 표시
     */
    
    
    
    /**
     * 텍스트 변경 애니메이션
     */
    private fun animateTextChange(textView: TextView, newText: String) {
        textView.animate()
            .alpha(0.3f)
            .setDuration(200)
            .withEndAction {
                textView.text = newText
                textView.animate()
                    .alpha(1.0f)
                    .setDuration(300)
                    .start()
            }
            .start()
    }
    
    /**
     * 진행률 바 애니메이션 (색상 포함)
     */
    private fun animateProgressBarWithColor(progressBar: ProgressBar, targetProgress: Int) {
        val currentProgress = progressBar.progress
        val animator = android.animation.ValueAnimator.ofInt(currentProgress, targetProgress)
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Int
            progressBar.progress = progress
            
            // 진행률에 따른 색상 변경
            val color = when {
                progress >= 80 -> android.R.color.holo_red_dark
                progress >= 60 -> android.R.color.holo_orange_dark
                else -> android.R.color.holo_green_dark
            }
            progressBar.progressTintList = android.content.res.ColorStateList.valueOf(
                progressBar.context.resources.getColor(color, null)
            )
        }
        animator.start()
    }
    
    /**
     * 색상 변경 애니메이션
     */
    private fun animateColorChange(textView: TextView, progressPercent: Int) {
        val color = when {
            progressPercent >= 80 -> android.R.color.holo_red_dark
            progressPercent >= 60 -> android.R.color.holo_orange_dark
            else -> android.R.color.holo_green_dark
        }
        
        textView.setTextColor(textView.context.resources.getColor(color, null))
        
        // 펄스 애니메이션 효과
        textView.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(200)
            .withEndAction {
                textView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }
    
    /**
     * 데이터 새로고침 (DB에서 다시 조회)
     */
    /**
     * 대시보드 데이터 새로고침 (외부에서 호출 가능)
     * 품질 개선: 항상 DB에서 최신 데이터를 재조회하여 메모리 캐시 무시
     */
    fun refreshDashboardData() {
        android.util.Log.d("MainActivity", "=== 대시보드 데이터 새로고침 시작 (외부 호출) ===")
        // 메모리 캐시를 무시하고 항상 DB에서 최신 데이터 재조회
        loadDashboardData()
    }
    
    /**
     * 앱 시작 시 기존 데이터로 대시보드 초기화
     */
    /**
     * 메인화면 대시보드 데이터 로드 (DB에서 항상 최신 데이터 재조회)
     * 품질 개선: 메모리 캐시에 의존하지 않고 항상 DB에서 실시간 데이터 조회
     */
    private fun loadDashboardData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("MainActivity", "=== 메인화면 데이터 재조회 시작 ===")
                
                val cardTransactionDao = database.cardTransactionDao()
                val bankTransactionDao = database.bankTransactionDao()
                val bankBalanceDao = database.bankBalanceDao()
                
                // 현재 월의 시작과 끝 날짜 계산 (9월과 10월 데이터 모두 조회)
                val now = java.time.LocalDateTime.now()
                val currentMonth = now.monthValue
                
                // 9월과 10월 데이터를 모두 조회하기 위해 범위 확장
                val startOfRange = now.withMonth(9).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                val endOfRange = now.withMonth(10).withDayOfMonth(31).withHour(23).withMinute(59).withSecond(59)
                
                // 현재 월(10월) 범위
                val startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                val endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59)
                
                // 전월(9월) 범위
                val lastMonth = now.minusMonths(1)
                val startOfLastMonth = lastMonth.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                val endOfLastMonth = lastMonth.withDayOfMonth(lastMonth.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59)
                
                android.util.Log.d("MainActivity", "조회 기간: ${startOfMonth} ~ ${endOfMonth}")
                
                // 1. 카드 거래 데이터 조회 (항상 DB에서 최신 데이터)
                val monthlyBillAmount = cardTransactionDao.getMonthlyBillAmount(startOfMonth, endOfMonth) ?: 0L
                val totalCardAmount = cardTransactionDao.getTotalCardUsageAmount(startOfMonth, endOfMonth) ?: 0L
                val cardTransactionCount = cardTransactionDao.getCardTransactionCountByDateRange(startOfMonth, endOfMonth)
                
                // 전월 카드 사용금액 조회
                val lastMonthCardAmount = cardTransactionDao.getTotalCardUsageAmount(startOfLastMonth, endOfLastMonth) ?: 0L
                val lastMonthCardCount = cardTransactionDao.getCardTransactionCountByDateRange(startOfLastMonth, endOfLastMonth)
                
                android.util.Log.d("MainActivity", "카드 거래 - 청구금액: $monthlyBillAmount, 총사용액: $totalCardAmount, 건수: $cardTransactionCount")
                android.util.Log.d("MainActivity", "전월 카드 거래 - 총사용액: $lastMonthCardAmount, 건수: $lastMonthCardCount")
                
                // 2. 소득 데이터 조회 (BankTransactionDao 사용 - 입금만 조회)
                val currentMonthIncome = bankTransactionDao.getTotalDepositAmount(startOfMonth, endOfMonth) ?: 0L
                val incomeTransactionCount = bankTransactionDao.getBankTransactionCountByType("입금")
                
                // 디버깅: 전체 데이터 조회
                android.util.Log.d("MainActivity", "=== 전체 은행거래 데이터 디버깅 ===")
                
                android.util.Log.d("MainActivity", "소득 거래 - 총액: $currentMonthIncome, 건수: $incomeTransactionCount")
                android.util.Log.d("MainActivity", "조회 기간: ${startOfMonth} ~ ${endOfMonth}")
                
                // 3. 전월 소득 데이터 조회 (비교용)
                val lastMonthIncome = bankTransactionDao.getTotalDepositAmount(startOfLastMonth, endOfLastMonth) ?: 0L
                
                android.util.Log.d("MainActivity", "전월 소득: $lastMonthIncome")
                android.util.Log.d("MainActivity", "전월 조회 기간: ${startOfLastMonth} ~ ${endOfLastMonth}")
                
                // 디버깅: 전월 입금 내역 상세 조회 (Flow collect 제거)
                android.util.Log.d("MainActivity", "=== 전월 입금 내역 상세 ===")
                val lastMonthDepositsList = bankTransactionDao.getBankTransactionsByDateRangeList(startOfLastMonth, endOfLastMonth)
                lastMonthDepositsList.filter { it.transactionType == "입금" }.forEach { deposit ->
                    android.util.Log.d("MainActivity", "전월 입금: ${deposit.transactionDate} - ${deposit.description} - ${deposit.amount}원")
                }
                android.util.Log.d("MainActivity", "전월 입금 총 건수: ${lastMonthDepositsList.filter { it.transactionType == "입금" }.size}건")
                
                // 4. 총 은행 잔고 조회 (항상 DB에서 최신 데이터)
                val totalBankBalance = bankBalanceDao.getTotalBankBalance() ?: 0L
                val bankBalanceCount = bankBalanceDao.getBankBalanceCount()
                
                android.util.Log.d("MainActivity", "은행 잔고 - 총액: $totalBankBalance, 계좌수: $bankBalanceCount")
                
                // 5. 메인 스레드에서 대시보드 업데이트
                withContext(Dispatchers.Main) {
                    try {
                        // 카드 사용 대시보드 업데이트
                        updateDashboard(monthlyBillAmount, totalCardAmount, lastMonthCardAmount)
                        
                        // 소득 대시보드 업데이트
                        updateIncomeDashboard(currentMonthIncome, lastMonthIncome)
                        
                        // 요약 정보 업데이트 (실시간 DB 데이터 기반)
                        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
                        val summaryText = buildString {
                            append("📊 실시간 데이터 요약 (DB 조회)\n\n")
                            append("💳 카드사용: ${formatter.format(totalCardAmount)}원 (${cardTransactionCount}건)\n")
                            append("💰 이번달 청구: ${formatter.format(monthlyBillAmount)}원\n")
                            append("💵 소득: ${formatter.format(currentMonthIncome)}원 (${incomeTransactionCount}건)\n")
                            if (totalBankBalance > 0) {
                                append("🏦 총 잔고: ${formatter.format(totalBankBalance)}원 (${bankBalanceCount}계좌)\n")
                            }
                            append("\n🔄 마지막 업데이트: ${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MM/dd HH:mm:ss"))}")
                        }
                        tvSummary.text = summaryText
                        
                        android.util.Log.d("MainActivity", "=== 메인화면 데이터 업데이트 완료 ===")
                        
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "UI 업데이트 중 오류: ${e.message}", e)
                        tvSummary.text = "❌ 데이터 로드 오류: ${e.message}"
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "데이터 로드 중 오류: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    tvSummary.text = "❌ 데이터 로드 오류: ${e.message}"
                }
            }
        }
    }
    
    /**
     * 거래별 이번달 청구금액 계산
     */
    private fun calculateMonthlyBillAmount(transaction: CardTransaction): Long {
        val amount = when {
            transaction.transactionType == "취소" -> -transaction.amount
            transaction.installment == "일시불" -> transaction.amount
            transaction.installment.contains("개월") -> {
                // 할부 거래의 첫 번째 달 금액 계산
                val installmentMonths = transaction.installment.replace("개월", "").toIntOrNull() ?: 1
                transaction.amount / installmentMonths // 첫 달 금액
            }
            else -> transaction.amount
        }
        println("DEBUG: ${transaction.merchant} - ${transaction.transactionType} - ${transaction.installment} - ${transaction.amount} -> ${amount}")
        return amount
    }
    
    override fun onResume() {
        super.onResume()
        android.util.Log.d("MainActivity", "onResume - 메인화면 재조회 시작")
        // 메인화면이 다시 표시될 때마다 항상 DB에서 최신 데이터 재조회
        loadDashboardData()
    }
    
    override fun onStart() {
        super.onStart()
        android.util.Log.d("MainActivity", "onStart - 메인화면 시작")
        // 앱이 시작될 때도 DB에서 최신 데이터 재조회
        loadDashboardData()
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
    
    /**
     * 카드 사용 대시보드 업데이트
     */
    private fun updateDashboard(monthlyBillAmount: Long, totalCardAmount: Long, lastMonthCardAmount: Long) {
        try {
            val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
            // 청구금액과 사용금액을 구분해서 표시
            val cardUsageText = "💳 이달 청구금액 ${formatter.format(monthlyBillAmount)}원 (사용 ${formatter.format(totalCardAmount)}원)"
            tvMonthlySpending.text = cardUsageText
            
            android.util.Log.d("MainActivity", "카드 사용 대시보드 업데이트: $cardUsageText")
            android.util.Log.d("MainActivity", "청구금액: ${formatter.format(monthlyBillAmount)}원, 사용금액: ${formatter.format(totalCardAmount)}원")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "카드 사용 대시보드 업데이트 오류: ${e.message}", e)
        }
    }
    
    /**
     * 소득 대시보드 업데이트 (개선된 로깅 및 검증)
     */
    private fun updateIncomeDashboard(currentMonthIncome: Long, lastMonthIncome: Long) {
        try {
            val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
            val incomeChange = currentMonthIncome - lastMonthIncome
            
            // 증감액 표시 로직 수정 (음수일 때는 - 기호 표시)
            val changeSign = if (incomeChange >= 0) "+" else ""
            val incomeText = "이달 소득금액 ${formatter.format(currentMonthIncome)}원 ($changeSign${formatter.format(incomeChange)}원)"
            
            // UI 업데이트 전 로깅
            android.util.Log.d("MainActivity", "=== 소득 대시보드 UI 업데이트 시작 ===")
            android.util.Log.d("MainActivity", "현재월 소득: ${formatter.format(currentMonthIncome)}원")
            android.util.Log.d("MainActivity", "전월 소득: ${formatter.format(lastMonthIncome)}원")
            android.util.Log.d("MainActivity", "증감액: $changeSign${formatter.format(incomeChange)}원")
            
            // UI 요소 업데이트
            tvMonthlyIncome.text = incomeText
            android.util.Log.d("MainActivity", "tvMonthlyIncome 업데이트: $incomeText")
            
            if (lastMonthIncome > 0) {
                val lastMonthText = "전월: ${formatter.format(lastMonthIncome)}원"
                tvIncomeChange.text = lastMonthText
                android.util.Log.d("MainActivity", "tvIncomeChange 업데이트: $lastMonthText")
                
                val changePercent = if (lastMonthIncome > 0) {
                    ((incomeChange.toDouble() / lastMonthIncome) * 100).toInt()
                } else 0
                
                val percentText = "변화율: ${changePercent}%"
                tvProgressPercent.text = percentText
                android.util.Log.d("MainActivity", "tvProgressPercent 업데이트: $percentText")
                
                android.util.Log.d("MainActivity", "소득 대시보드 업데이트 완료: 현재=${currentMonthIncome}, 전월=${lastMonthIncome}, 증감=${incomeChange}, 변화율=${changePercent}%")
            } else {
                tvIncomeChange.text = "전월: 0원"
                tvProgressPercent.text = "변화율: -"
                android.util.Log.d("MainActivity", "소득 대시보드 업데이트 완료: 현재=${currentMonthIncome}, 전월=0")
            }
            
            android.util.Log.d("MainActivity", "=== 소득 대시보드 UI 업데이트 완료 ===")
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "소득 대시보드 업데이트 오류: ${e.message}", e)
        }
    }
    
    /**
     * 테스트용 SMS 데이터 처리 함수 - 단계별 테스트
     */
    private fun testSmsParsing() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("MainActivity", "=== SMS 파싱 테스트 시작 ===")
                
                // 단계 1: 간단한 입금 SMS 테스트
                val simpleIncomeSms = "신한 10/11 21:54 100-***-159993 입금 2,500,000 잔액 3,700,000 급여"
                android.util.Log.d("MainActivity", "단계 1 - 간단한 입금 SMS 테스트: $simpleIncomeSms")
                
                val smsDataRepository = SmsDataRepository(this@MainActivity)
                val result1 = smsDataRepository.saveSmsData(simpleIncomeSms)
                
                android.util.Log.d("MainActivity", "단계 1 결과: 성공=${result1.isSuccess}, 메시지=${result1.message}")
                android.util.Log.d("MainActivity", "단계 1 - 카드거래 ID: ${result1.cardTransactionIds}")
                android.util.Log.d("MainActivity", "단계 1 - 수입거래 ID: ${result1.incomeTransactionIds}")
                android.util.Log.d("MainActivity", "단계 1 - 은행잔고 ID: ${result1.bankBalanceIds}")
                
                // 단계 2: 출금 SMS 테스트
                val withdrawalSms = "신한 10/11 21:54 100-***-159993 출금 3,500,000 잔액 1,200,000 신한카드"
                android.util.Log.d("MainActivity", "단계 2 - 출금 SMS 테스트: $withdrawalSms")
                
                val result2 = smsDataRepository.saveSmsData(withdrawalSms)
                
                android.util.Log.d("MainActivity", "단계 2 결과: 성공=${result2.isSuccess}, 메시지=${result2.message}")
                android.util.Log.d("MainActivity", "단계 2 - 카드거래 ID: ${result2.cardTransactionIds}")
                android.util.Log.d("MainActivity", "단계 2 - 수입거래 ID: ${result2.incomeTransactionIds}")
                android.util.Log.d("MainActivity", "단계 2 - 은행잔고 ID: ${result2.bankBalanceIds}")
                
                // 단계 3: 카드 거래 SMS 테스트
                val cardSms = "신한카드(1054)승인 신*진 42,820원(일시불)10/22 14:59 주식회사 이마트 누적1,903,674"
                android.util.Log.d("MainActivity", "단계 3 - 카드 거래 SMS 테스트: $cardSms")
                
                val result3 = smsDataRepository.saveSmsData(cardSms)
                
                android.util.Log.d("MainActivity", "단계 3 결과: 성공=${result3.isSuccess}, 메시지=${result3.message}")
                android.util.Log.d("MainActivity", "단계 3 - 카드거래 ID: ${result3.cardTransactionIds}")
                android.util.Log.d("MainActivity", "단계 3 - 수입거래 ID: ${result3.incomeTransactionIds}")
                android.util.Log.d("MainActivity", "단계 3 - 은행잔고 ID: ${result3.bankBalanceIds}")
                
                withContext(Dispatchers.Main) {
                    val message = "SMS 테스트 완료:\n입금: ${result1.incomeTransactionIds.size}건\n출금: ${result2.incomeTransactionIds.size}건\n카드: ${result3.cardTransactionIds.size}건"
                    android.widget.Toast.makeText(this@MainActivity, message, android.widget.Toast.LENGTH_LONG).show()
                    
                    // 데이터 새로고침
                    loadDashboardData()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "SMS 파싱 테스트 오류: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@MainActivity, 
                        "SMS 테스트 오류: ${e.message}", 
                        android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}