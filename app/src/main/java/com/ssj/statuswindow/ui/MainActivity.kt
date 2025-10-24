package com.ssj.statuswindow.ui

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.util.SmsParser
import com.ssj.statuswindow.model.CardTransaction
import com.ssj.statuswindow.database.StatusWindowDatabase
import com.ssj.statuswindow.database.entity.CardTransactionEntity
import com.ssj.statuswindow.database.entity.CreditCardUsageEntity
import com.ssj.statuswindow.service.MerchantCategoryAiService
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
    private lateinit var btnTestSms: Button
    private lateinit var btnInputSms: Button
    private lateinit var btnShowCreditCardTable: Button
    private lateinit var btnViewDetails: Button
    private lateinit var btnViewIncomeDetails: Button
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_main)
            
            // 데이터베이스 초기화
            database = StatusWindowDatabase.getDatabase(this)
            
            // 카테고리 AI 서비스 초기화
            categoryAiService = MerchantCategoryAiService(this)
            
            // 앱 시작 시 기존 데이터 초기화 (선택사항)
            // clearAllData()
            
            setupViews()
            setupToolbar()
            setupNavigation()
            setupClickListeners()
            
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
        btnTestSms = findViewById(R.id.btnTestSms)
        btnInputSms = findViewById(R.id.btnInputSms)
        btnShowCreditCardTable = findViewById(R.id.btnShowCreditCardTable)
        btnViewDetails = findViewById(R.id.btnViewDetails)
        btnViewIncomeDetails = findViewById(R.id.btnViewIncomeDetails)
        tvMonthlySpending = findViewById(R.id.tvMonthlySpending)
        tvMonthlyIncome = findViewById(R.id.tvMonthlyIncome)
        tvIncomeChange = findViewById(R.id.tvIncomeChange)
        tvIncomeChangePercent = findViewById(R.id.tvIncomeChangePercent)
        progressSpending = findViewById(R.id.progressSpending)
        tvProgressPercent = findViewById(R.id.tvProgressPercent)
        tvSummary = findViewById(R.id.tvSummary)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
    }
    
    private fun setupNavigation() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    // 현재 페이지
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_card_details -> {
                    // 카드 상세페이지로 이동
                    startActivity(Intent(this, CardDetailsActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_bank_transaction -> {
                    // 입출금내역 페이지로 이동
                    startActivity(Intent(this, BankTransactionActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_card_table -> {
                    showCreditCardTable()
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_category_analysis -> {
                    // 카테고리 분석 페이지로 이동
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_monthly_report -> {
                    // 월별 리포트 페이지로 이동
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_settings -> {
                    // 설정 페이지로 이동
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_about -> {
                    // 앱 정보 페이지로 이동
                    drawerLayout.closeDrawers()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupClickListeners() {
        btnTestSms.setOnClickListener {
            testSmsParsing()
        }
        
        btnInputSms.setOnClickListener {
            showSmsInputDialog()
        }
        
        btnShowCreditCardTable.setOnClickListener {
            showCreditCardTable()
        }
        
        btnViewDetails.setOnClickListener {
            startActivity(Intent(this, CardDetailsActivity::class.java))
        }
        
        btnViewIncomeDetails.setOnClickListener {
            // 소득 상세보기 페이지로 이동
            startActivity(Intent(this, IncomeDetailsActivity::class.java))
        }
    }
    
    private fun testSmsParsing() {
        try {
            // 샘플테스트 전에 기존 데이터 확인
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val cardTransactionDao = database.cardTransactionDao()
                    val existingCount = cardTransactionDao.getCardTransactionCount()
                    
                    withContext(Dispatchers.Main) {
                        if (existingCount > 0) {
                            // 기존 데이터가 있으면 사용자에게 확인
                            showTestDataConfirmationDialog()
                        } else {
                            // 기존 데이터가 없으면 바로 테스트 실행
                            executeTestSmsParsing()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        executeTestSmsParsing()
                    }
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            updateSummary("❌ 오류 발생: ${e.message}")
        }
    }
    
    private fun showTestDataConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("샘플 테스트 데이터")
            .setMessage("기존 데이터가 있습니다.\n\n샘플 테스트를 실행하시겠습니까?\n\n- 기존 데이터 유지: 중복 검사 후 추가\n- 기존 데이터 삭제: 모든 데이터 초기화 후 테스트")
            .setPositiveButton("기존 데이터 유지") { _, _ ->
                executeTestSmsParsing()
            }
            .setNeutralButton("기존 데이터 삭제") { _, _ ->
                clearAllDataAndTest()
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun clearAllDataAndTest() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 모든 데이터 삭제
                database.cardTransactionDao().deleteAllCardTransactions()
                database.creditCardUsageDao().deleteAllCreditCardUsage()
                database.bankTransactionDao().deleteAllBankTransactions()
                
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@MainActivity, "기존 데이터가 삭제되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                    executeTestSmsParsing()
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
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
    
    private fun showSmsInputDialog() {
        val dialog = SmsInputDialog(this) { smsText ->
            parseSmsData(smsText)
        }
        dialog.show()
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
                    val existingCount = cardTransactionDao.getCardTransactionCountByOriginalText(transaction.originalText)
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
                    val existingCount = creditCardUsageDao.getCreditCardUsageCountByOriginalText(transaction.originalText)
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
                    
                    // AI로 카테고리 추론 (한국어 기본)
                    val inferredCategory = categoryAiService.inferCategory(transaction.merchant, "ko")
                    
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
                    val existingCount = bankTransactionDao.getBankTransactionCountByOriginalText(incomeTransaction.originalText)
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
                    
                            // 메인 스레드에서 UI 업데이트
                            withContext(Dispatchers.Main) {
                                val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
                                summary.append("카드사용 총액: ${formatter.format(totalAmount)}원 (DB 쿼리)\n")
                                summary.append("이번달 청구금액: ${formatter.format(monthlyBillAmount)}원 (DB 쿼리)\n")
                                summary.append("파싱된 거래 수: ${cardTransactions.size}건\n")
                                
                                // 소득 정보도 추가
                                val totalIncome = incomeTransactions.sumOf { transaction -> transaction.amount }
                                summary.append("소득 총액: ${formatter.format(totalIncome)}원\n\n")
                                
                                // 각 거래별 상세 정보 표시 (메모리 계산으로 비교)
                                summary.append("=== 거래 상세 (메모리 계산) ===\n")
                                cardTransactions.forEachIndexed { index, transaction ->
                                    val billAmount = calculateMonthlyBillAmount(transaction)
                                    summary.append("${index + 1}. ${transaction.merchant} - ${transaction.transactionType} - ${transaction.installment} - ${formatter.format(transaction.amount)}원 → ${formatter.format(billAmount)}원\n")
                                }
                                
                                tvSummary.text = summary.toString()
                                
                                // 대시보드 업데이트 추가 (DB에서 읽어온 값 사용)
                                updateDashboard(monthlyBillAmount, totalAmount)
                                
                                // 소득금액도 업데이트 (입출금내역에서 입금만)
                                val bankTransactionDao = database.bankTransactionDao()
                                val currentMonthIncome = bankTransactionDao.getTotalAmountByDateRange(startOfMonth, endOfMonth) ?: 0L
                                val lastMonth = java.time.LocalDateTime.now().minusMonths(1)
                                val startOfLastMonth = lastMonth.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                                val endOfLastMonth = lastMonth.withDayOfMonth(lastMonth.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59)
                                val lastMonthIncome = bankTransactionDao.getTotalAmountByDateRange(startOfLastMonth, endOfLastMonth) ?: 0L
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
    private fun showCreditCardTable() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val creditCardUsageDao = database.creditCardUsageDao()
                val allCreditCardUsage = creditCardUsageDao.getAllCreditCardUsage()
                
                // Flow를 collect하여 데이터 가져오기
                allCreditCardUsage.collect { creditCardList ->
                    withContext(Dispatchers.Main) {
                        displayCreditCardTable(creditCardList)
                    }
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    tvSummary.text = "❌ 신용카드 테이블 조회 오류: ${e.message}"
                }
            }
        }
    }
    
    /**
     * 신용카드 테이블 데이터 표시
     */
    private fun displayCreditCardTable(creditCardList: List<CreditCardUsageEntity>) {
        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("MM/dd HH:mm")
        
        val tableText = StringBuilder()
        tableText.append("💳 신용카드 사용내역 테이블\n")
        tableText.append("=".repeat(50)).append("\n\n")
        
        if (creditCardList.isEmpty()) {
            tableText.append("저장된 신용카드 사용내역이 없습니다.\n")
            tableText.append("먼저 '샘플 테스트' 또는 '직접 입력'을 실행해주세요.")
        } else {
            // 테이블 헤더
            tableText.append("ID | 카드명 | 거래타입 | 금액 | 할부 | 월납부 | 가맹점 | 거래일시\n")
            tableText.append("-".repeat(80)).append("\n")
            
            // 테이블 데이터
            creditCardList.forEach { creditCard ->
                tableText.append("${creditCard.id} | ")
                tableText.append("${creditCard.cardName} | ")
                tableText.append("${creditCard.transactionType} | ")
                tableText.append("${formatter.format(creditCard.amount)}원 | ")
                tableText.append("${creditCard.installment} | ")
                tableText.append("${formatter.format(creditCard.monthlyPayment)}원 | ")
                tableText.append("${creditCard.merchant} | ")
                tableText.append("${creditCard.transactionDate.format(dateFormatter)}\n")
            }
            
            // 통계 정보
            tableText.append("\n📊 통계 정보\n")
            tableText.append("-".repeat(30)).append("\n")
            tableText.append("총 거래 건수: ${creditCardList.size}건\n")
            
            val totalAmount = creditCardList.sumOf { it.amount }
            val totalMonthlyPayment = creditCardList.sumOf { it.monthlyPayment }
            
            tableText.append("총 사용금액: ${formatter.format(totalAmount)}원\n")
            tableText.append("총 월납부금액: ${formatter.format(totalMonthlyPayment)}원\n")
            
            // 카드별 통계
            val cardStats = creditCardList.groupBy { it.cardName }
            tableText.append("\n💳 카드별 사용내역\n")
            tableText.append("-".repeat(30)).append("\n")
            
            cardStats.forEach { (cardName, transactions) ->
                val cardTotalAmount = transactions.sumOf { it.amount }
                val cardMonthlyPayment = transactions.sumOf { it.monthlyPayment }
                tableText.append("${cardName}: ${transactions.size}건, ${formatter.format(cardTotalAmount)}원, 월납부 ${formatter.format(cardMonthlyPayment)}원\n")
            }
        }
        
        tvSummary.text = tableText.toString()
    }
    
    /**
     * 대시보드 업데이트 (애니메이션 포함)
     */
    private fun updateDashboard(monthlyBillAmount: Long, totalAmount: Long) {
        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        
        // 이달 소비금액 텍스트 업데이트 (결제금액 총액 사용)
        val spendingText = "이달 소비금액 ${formatter.format(totalAmount)}원 (전월 0원)"
        animateTextChange(tvMonthlySpending, spendingText)
        
        // 진행률 계산 (예: 월 예산 500,000원 기준)
        val monthlyBudget = 500000L // 월 예산 설정
        val progressPercent = if (monthlyBudget > 0) {
            ((totalAmount.toFloat() / monthlyBudget) * 100).toInt().coerceIn(0, 100)
        } else 0
        
        // 진행률 바 애니메이션 (색상 포함)
        animateProgressBarWithColor(progressSpending, progressPercent)
        
        // 진행률 텍스트 애니메이션
        val progressText = "${progressPercent}%"
        animateTextChange(tvProgressPercent, progressText)
        
        // 색상 변경 애니메이션
        animateColorChange(tvMonthlySpending, progressPercent)
    }
    
    /**
     * 소득금액 대시보드 업데이트 (애니메이션 포함)
     */
    private fun updateIncomeDashboard(currentMonthIncome: Long, lastMonthIncome: Long) {
        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        
        // 이달 소득금액 텍스트 업데이트 (애니메이션)
        val incomeChange = currentMonthIncome - lastMonthIncome
        val incomeText = "이달 소득금액 ${formatter.format(currentMonthIncome)}원 (+${formatter.format(incomeChange)}원)"
        animateTextChange(tvMonthlyIncome, incomeText)
        
        // 전월 소득금액 텍스트 업데이트
        val lastMonthText = "전월: ${formatter.format(lastMonthIncome)}원"
        animateTextChange(tvIncomeChange, lastMonthText)
        
        // 증가율 계산 및 표시
        val changePercent = if (lastMonthIncome > 0) {
            ((incomeChange.toFloat() / lastMonthIncome) * 100).toInt()
        } else 0
        
        val changePercentText = if (changePercent >= 0) "+${changePercent}%" else "${changePercent}%"
        animateTextChange(tvIncomeChangePercent, changePercentText)
        
        // 증가율에 따른 색상 변경
        val color = if (changePercent >= 0) {
            android.R.color.holo_green_dark
        } else {
            android.R.color.holo_red_dark
        }
        tvIncomeChangePercent.setTextColor(resources.getColor(color, null))
    }
    
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
     * 앱 시작 시 기존 데이터로 대시보드 초기화
     */
    private fun loadDashboardData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cardTransactionDao = database.cardTransactionDao()
                
                // 현재 월의 시작과 끝 날짜 계산
                val now = java.time.LocalDateTime.now()
                val startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                val endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59)
                
                // DB에서 현재 월 데이터 조회
                val monthlyBillAmount = cardTransactionDao.getMonthlyBillAmount(startOfMonth, endOfMonth) ?: 0L
                val totalAmount = cardTransactionDao.getTotalCardUsageAmount(startOfMonth, endOfMonth) ?: 0L
                
                // 소득 데이터 조회 (입출금내역에서 입금만)
                val bankTransactionDao = database.bankTransactionDao()
                val currentMonthIncome = bankTransactionDao.getTotalAmountByDateRange(startOfMonth, endOfMonth) ?: 0L
                
                // 전월 소득 데이터 조회
                val lastMonth = now.minusMonths(1)
                val startOfLastMonth = lastMonth.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                val endOfLastMonth = lastMonth.withDayOfMonth(lastMonth.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59)
                val lastMonthIncome = bankTransactionDao.getTotalAmountByDateRange(startOfLastMonth, endOfLastMonth) ?: 0L
                
                // 메인 스레드에서 대시보드 업데이트
                withContext(Dispatchers.Main) {
                    if (monthlyBillAmount > 0 || totalAmount > 0) {
                        // 데이터가 있으면 대시보드 업데이트
                        updateDashboard(monthlyBillAmount, totalAmount)
                        updateIncomeDashboard(currentMonthIncome, lastMonthIncome)
                        
                        // 요약 정보도 업데이트
                        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
                        val summaryText = "📊 기존 데이터 로드 완료\n\n" +
                                "카드사용 총액: ${formatter.format(totalAmount)}원\n" +
                                "이번달 청구금액: ${formatter.format(monthlyBillAmount)}원"
                        tvSummary.text = summaryText
                    } else {
                        // 데이터가 없으면 기본 메시지
                        tvSummary.text = "📊 파싱 결과 요약\n\n총 거래: 0건\n카드 거래: 0건\n은행 거래: 0건"
                    }
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    tvSummary.text = "❌ 대시보드 로드 오류: ${e.message}"
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
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(navigationView)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}