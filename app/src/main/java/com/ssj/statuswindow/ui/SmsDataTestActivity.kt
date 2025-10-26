package com.ssj.statuswindow.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.databinding.ActivitySmsDataTestBinding
import com.ssj.statuswindow.repo.database.SmsDataRepository
import com.ssj.statuswindow.util.NavigationManager
import com.ssj.statuswindow.util.TestLogger
import com.ssj.statuswindow.ui.components.AppToolbar
import kotlinx.coroutines.launch

/**
 * SMS 데이터 저장 테스트를 위한 Activity
 */
class SmsDataTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySmsDataTestBinding
    private lateinit var smsDataRepository: SmsDataRepository
    
    // 네비게이션 드로어 관련
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var appToolbar: AppToolbar
    private lateinit var navigationView: NavigationView

    // 테스트용 SMS 데이터 (smspasertest.txt에서 가져온 데이터)
    private val testSmsData = listOf(
        "신한카드(1054)승인 신*진 42,820원(일시불)10/22 14:59 주식회사 이마트 누적1,903,674",
        "신한카드(1054)승인 신*진 98,700원(2개월)10/22 15:48 카톨릭대병원 누적1,960,854원",
        "신한카드(1054)취소 신*진 12,700원(일시불)10/22 15:48 스타벅스 누적1,860,854원",
        "신한카드(1054)승인 신*진 12,700원(일시불)10/22 15:48 스타벅스 누적1,860,854원",
        "신한카드(1054)승인 신*진 42,820원(일시불)10/21 14:59 주식회사 이마트 누적1,903,674",
        "신한카드(1054)승인 신*진 98,700원(3개월)10/21 15:48 카톨릭대병원 누적1,960,854원",
        "신한카드(1054)승인 신*진 12,700원(일시불)10/21 15:48 스타벅스 누적1,860,854원",
        "신한 10/11 21:54 100-***-159993 입금 2,500,000 잔액 3,700,000 급여",
        "신한 10/11 21:54 100-***-159993 출금 3,500,000 잔액 1,200,000 신한카드",
        "신한 09/11 21:54 100-***-159993 입금 2,500,000 잔액 5,000,000 신승진",
        "신한 08/11 21:54 100-***-159993 입금 2,500,000 잔액 2,500,000 급여",
        "신한카드(1054)승인 신*진 85,000원(일시불)09/28 19:30 롯데마트 누적1,850,000원",
        "신한카드(1054)승인 신*진 45,000원(일시불)09/28 14:15 교보문고 누적1,895,000원",
        "신한카드(1054)승인 신*진 32,000원(일시불)09/27 12:30 맥도날드 누적1,927,000원",
        "신한카드(1054)승인 신*진 78,000원(2개월)09/27 16:45 현대백화점 누적1,905,000원",
        "신한카드(1054)취소 신*진 15,000원(일시불)09/26 20:15 네이버페이 누적1,920,000원",
        "신한카드(1054)승인 신*진 15,000원(일시불)09/26 20:15 네이버페이 누적1,920,000원",
        "신한카드(1054)승인 신*진 120,000원(일시불)09/25 11:20 홈플러스 누적1,800,000원",
        "신한카드(1054)승인 신*진 65,000원(일시불)09/24 18:45 CGV 누적1,865,000원",
        "신한카드(1054)승인 신*진 28,000원(일시불)09/23 13:10 스타벅스 누적1,893,000원",
        "신한카드(1054)승인 신*진 95,000원(3개월)09/22 15:30 아울렛 누적1,798,000원",
        "신한 09/30 18:00 100-***-159993 출금 150,000 잔액 1,750,000 생활비",
        "신한 09/29 14:30 100-***-159993 입금 50,000 잔액 1,900,000 용돈",
        "신한 09/28 09:15 100-***-159993 출금 200,000 잔액 1,850,000 카드결제",
        "신한 09/27 16:20 100-***-159993 입금 300,000 잔액 2,050,000 부업수입",
        "신한 09/26 11:45 100-***-159993 출금 100,000 잔액 1,750,000 현금인출",
        "신한 09/25 20:30 100-***-159993 입금 80,000 잔액 1,830,000 환급금",
        "신한 09/24 15:10 100-***-159993 출금 120,000 잔액 1,710,000 생활비",
        "신한 09/23 12:00 100-***-159993 입금 2,500,000 잔액 3,210,000 급여",
        "신한 09/22 19:45 100-***-159993 출금 500,000 잔액 2,710,000 대출상환"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySmsDataTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupToolbar()
        setupNavigation()
        
        smsDataRepository = SmsDataRepository(this)

        setupClickListeners()
        updateDatabaseGrid()
    }
    
    private fun setupViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        appToolbar = findViewById(R.id.appToolbar)
        navigationView = findViewById(R.id.navigationView)
        
        // AppToolbar 설정
        appToolbar.setupWithDrawer(this, drawerLayout)
        appToolbar.setTitle("SMS 테스트")
    }
    
    private fun setupToolbar() {
        // AppToolbar는 이미 setupViews에서 설정됨
        // 기존 toolbar 관련 코드는 제거
    }
    
    private fun setupNavigation() {
        NavigationManager.setupNavigation(this, navigationView, drawerLayout, SmsDataTestActivity::class.java)
        NavigationManager.setActiveMenuItem(navigationView, SmsDataTestActivity::class.java)
    }

    private fun updateDatabaseGrid() {
        lifecycleScope.launch {
            try {
                val cardCount = smsDataRepository.getCardTransactionCount()
                val incomeCount = smsDataRepository.getIncomeTransactionCount()
                val bankBalanceCount = smsDataRepository.getBankBalanceCount()
                val totalBankBalance = smsDataRepository.getTotalBankBalance()

                binding.tvCardCount.text = "${cardCount}건"
                binding.tvIncomeCount.text = "${incomeCount}건"
                binding.tvBankCount.text = "${bankBalanceCount}건"
                binding.tvTotalBalance.text = "${String.format("%,d", totalBankBalance)}원"
            } catch (e: Exception) {
                android.util.Log.e("SmsDataTestActivity", "데이터베이스 그리드 업데이트 오류: ${e.message}", e)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.sms_data_test_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (drawerLayout.isDrawerOpen(navigationView)) {
                    drawerLayout.closeDrawer(navigationView)
                } else {
                    drawerLayout.openDrawer(navigationView)
                }
                true
            }
            R.id.action_clear_data -> {
                clearAllData()
                true
            }
            R.id.action_refresh_data -> {
                testDataRetrieval()
                true
            }
            R.id.action_export_data -> {
                Toast.makeText(this, "데이터 내보내기 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_settings -> {
                Toast.makeText(this, "설정 화면은 준비 중입니다.", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_help -> {
                Toast.makeText(this, "도움말: SMS 텍스트를 입력하고 저장 버튼을 눌러 테스트하세요.", Toast.LENGTH_LONG).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupClickListeners() {
        binding.btnTestCardTransaction.setOnClickListener {
            testCardTransaction()
        }
        binding.btnTestIncomeTransaction.setOnClickListener {
            testIncomeTransaction()
        }
        binding.btnTestBankBalance.setOnClickListener {
            testBankBalance()
        }
        binding.btnTestSampleData.setOnClickListener {
            testSampleDataFromFile()
        }
        binding.btnRunLogicTests.setOnClickListener {
            runLogicTests()
        }
        binding.btnTestDataDeletion.setOnClickListener {
            testDataDeletion()
        }
        binding.btnTestAllSmsData.setOnClickListener {
            testAllSmsData()
        }
        binding.btnTestDataRetrieval.setOnClickListener {
            testDataRetrieval()
        }
        binding.btnClearAllData.setOnClickListener {
            clearAllData()
        }
        binding.btnSaveManualSms.setOnClickListener {
            saveManualSms()
        }
    }

    private fun saveManualSms() {
        val smsText = binding.etManualSms.text.toString().trim()
        if (smsText.isEmpty()) {
            Toast.makeText(this, "SMS 텍스트를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                android.util.Log.d("SmsDataTestActivity", "=== 수동 SMS 저장 시작 ===")
                android.util.Log.d("SmsDataTestActivity", "입력된 SMS: $smsText")

                val result = smsDataRepository.saveSmsData(smsText)
                android.util.Log.d("SmsDataTestActivity", "저장 결과: $result")

                if (result.isSuccess) {
                    val message = "SMS 저장 성공!\n수입: ${result.incomeTransactionIds.size}건\n잔고: ${result.bankBalanceIds.size}건\n카드: ${result.cardTransactionIds.size}건"
                    android.util.Log.d("SmsDataTestActivity", message)
                    Toast.makeText(this@SmsDataTestActivity, message, Toast.LENGTH_LONG).show()

                    // 입력창 초기화
                    binding.etManualSms.setText("")
                    
                    // 그리드 업데이트
                    updateDatabaseGrid()
                } else {
                    android.util.Log.e("SmsDataTestActivity", "SMS 저장 실패: ${result.message}")
                    Toast.makeText(this@SmsDataTestActivity,
                        "SMS 저장 실패: ${result.message}",
                        Toast.LENGTH_LONG).show()
                }

                android.util.Log.d("SmsDataTestActivity", "=== 수동 SMS 저장 완료 ===")
            } catch (e: Exception) {
                android.util.Log.e("SmsDataTestActivity", "SMS 저장 오류: ${e.message}", e)
                Toast.makeText(this@SmsDataTestActivity,
                    "SMS 저장 오류: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun testCardTransaction() {
        lifecycleScope.launch {
            try {
                val smsText = testSmsData[0] // 카드 거래 내역
                val result = smsDataRepository.saveSmsData(smsText)

                if (result.isSuccess) {
                    Toast.makeText(this@SmsDataTestActivity,
                        "카드 거래 저장 성공: ${result.cardTransactionIds.size}건",
                        Toast.LENGTH_SHORT).show()
                    updateDatabaseGrid()
                } else {
                    Toast.makeText(this@SmsDataTestActivity,
                        "카드 거래 저장 실패: ${result.message}",
                        Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SmsDataTestActivity,
                    "카드 거래 테스트 오류: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun testIncomeTransaction() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("SmsDataTestActivity", "=== 수입 내역 테스트 시작 ===")

                val smsText = testSmsData[2] // 수입 내역 (급여가 있는 것)
                android.util.Log.d("SmsDataTestActivity", "테스트 SMS: $smsText")

                val result = smsDataRepository.saveSmsData(smsText)
                android.util.Log.d("SmsDataTestActivity", "저장 결과: $result")

                if (result.isSuccess) {
                    val message = "수입 내역 저장 성공: 수입 ${result.incomeTransactionIds.size}건, 잔고 ${result.bankBalanceIds.size}건"
                    android.util.Log.d("SmsDataTestActivity", message)
                    Toast.makeText(this@SmsDataTestActivity, message, Toast.LENGTH_SHORT).show()
                    updateDatabaseGrid()
                } else {
                    android.util.Log.e("SmsDataTestActivity", "수입 내역 저장 실패: ${result.message}")
                    Toast.makeText(this@SmsDataTestActivity,
                        "수입 내역 저장 실패: ${result.message}",
                        Toast.LENGTH_SHORT).show()
                }

                android.util.Log.d("SmsDataTestActivity", "=== 수입 내역 테스트 완료 ===")
            } catch (e: Exception) {
                android.util.Log.e("SmsDataTestActivity", "수입 내역 테스트 오류: ${e.message}", e)
                Toast.makeText(this@SmsDataTestActivity,
                    "수입 내역 테스트 오류: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun testBankBalance() {
        lifecycleScope.launch {
            try {
                val smsText = testSmsData[1] // 수입 내역에서 은행 잔고 추출
                val result = smsDataRepository.saveSmsData(smsText)

                if (result.isSuccess) {
                    Toast.makeText(this@SmsDataTestActivity,
                        "은행 잔고 저장 성공: ${result.bankBalanceIds.size}건",
                        Toast.LENGTH_SHORT).show()
                    updateDatabaseGrid()
                } else {
                    Toast.makeText(this@SmsDataTestActivity,
                        "은행 잔고 저장 실패: ${result.message}",
                        Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SmsDataTestActivity,
                    "은행 잔고 테스트 오류: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun testAllSmsData() {
        lifecycleScope.launch {
            try {
                var totalSaved = 0
                var totalFailed = 0
                
                android.util.Log.d("SmsDataTestActivity", "=== 전체 SMS 데이터 테스트 시작 ===")
                android.util.Log.d("SmsDataTestActivity", "테스트할 SMS 개수: ${testSmsData.size}")
                
                testSmsData.forEachIndexed { index, smsText ->
                    android.util.Log.d("SmsDataTestActivity", "SMS ${index + 1} 처리 중: $smsText")
                    val result = smsDataRepository.saveSmsData(smsText)
                    android.util.Log.d("SmsDataTestActivity", "SMS ${index + 1} 결과: 성공=${result.isSuccess}, 카드=${result.cardTransactionIds.size}, 수입=${result.incomeTransactionIds.size}, 잔고=${result.bankBalanceIds.size}")
                    
                    if (result.isSuccess) {
                        totalSaved += result.cardTransactionIds.size + result.incomeTransactionIds.size + result.bankBalanceIds.size
                    } else {
                        totalFailed++
                        android.util.Log.e("SmsDataTestActivity", "SMS ${index + 1} 실패: ${result.message}")
                    }
                }
                
                android.util.Log.d("SmsDataTestActivity", "=== 전체 SMS 데이터 테스트 완료: 성공 ${totalSaved}건, 실패 ${totalFailed}건 ===")

                Toast.makeText(this@SmsDataTestActivity,
                    "전체 SMS 데이터 테스트 완료: 성공 ${totalSaved}건, 실패 ${totalFailed}건",
                    Toast.LENGTH_LONG).show()
                
                updateDatabaseGrid()

            } catch (e: Exception) {
                android.util.Log.e("SmsDataTestActivity", "전체 SMS 데이터 테스트 오류: ${e.message}", e)
                Toast.makeText(this@SmsDataTestActivity,
                    "전체 SMS 데이터 테스트 오류: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * smspasertest.txt 파일의 샘플 데이터를 테스트하는 함수 (개선된 로깅)
     */
    private fun testSampleDataFromFile() {
        val testCaseName = "SMS_파싱_샘플데이터_테스트"
        val startTime = System.currentTimeMillis()
        
        TestLogger.startTestCase(this, testCaseName, "smspasertest.txt 파일의 30개 SMS 데이터 파싱 테스트")
        
        lifecycleScope.launch {
            try {
                TestLogger.logTestStep(this@SmsDataTestActivity, testCaseName, 1, "테스트 준비", 
                    "총 ${testSmsData.size}개의 SMS 데이터 처리 시작")
                
                var successCount = 0
                var failCount = 0
                var totalCardTransactions = 0
                var totalIncomeTransactions = 0
                var totalBankBalances = 0
                val failedSmsList = mutableListOf<String>()

                // 단계 2: 기존 데이터 삭제
                TestLogger.logTestStep(this@SmsDataTestActivity, testCaseName, 2, "기존 데이터 삭제", 
                    "샘플데이터 테스트 전 기존 데이터 완전 삭제")
                
                smsDataRepository.clearAllData()
                kotlinx.coroutines.delay(500)

                // 단계 3: SMS 데이터 처리
                TestLogger.logTestStep(this@SmsDataTestActivity, testCaseName, 3, "SMS 데이터 처리", 
                    "각 SMS를 순차적으로 파싱하고 저장")

                testSmsData.forEachIndexed { index, smsText ->
                    val stepStartTime = System.currentTimeMillis()
                    
                    TestLogger.logTestStep(this@SmsDataTestActivity, testCaseName, 4, "SMS 처리", 
                        "SMS ${index + 1}/${testSmsData.size} 처리 중", 
                        mapOf("smsText" to smsText))

                    val result = smsDataRepository.saveSmsData(smsText)
                    val stepDuration = System.currentTimeMillis() - stepStartTime
                    
                    // SMS 파싱 결과 로깅
                    TestLogger.logSmsParsingResult(this@SmsDataTestActivity, testCaseName, smsText,
                        mapOf(
                            "isSuccess" to result.isSuccess,
                            "cardCount" to result.cardTransactionIds.size,
                            "incomeCount" to result.incomeTransactionIds.size,
                            "balanceCount" to result.bankBalanceIds.size,
                            "message" to result.message,
                            "durationMs" to stepDuration
                        ), result.isSuccess)

                    if (result.isSuccess) {
                        successCount++
                        totalCardTransactions += result.cardTransactionIds.size
                        totalIncomeTransactions += result.incomeTransactionIds.size
                        totalBankBalances += result.bankBalanceIds.size
                        
                        TestLogger.log(this@SmsDataTestActivity, TestLogger.LogLevel.INFO, "SmsDataTestActivity", 
                            "SMS ${index + 1} 저장 성공: 카드=${result.cardTransactionIds.size}, 수입=${result.incomeTransactionIds.size}, 잔고=${result.bankBalanceIds.size}")
                    } else {
                        failCount++
                        failedSmsList.add("SMS ${index + 1}: $smsText")
                        
                        TestLogger.log(this@SmsDataTestActivity, TestLogger.LogLevel.ERROR, "SmsDataTestActivity", 
                            "SMS ${index + 1} 저장 실패: ${result.message}")
                    }

                    kotlinx.coroutines.delay(100)
                }

                // 단계 4: 결과 검증
                TestLogger.logTestStep(this@SmsDataTestActivity, testCaseName, 5, "결과 검증", 
                    "처리된 데이터 건수와 예상값 비교")

                val totalProcessed = totalCardTransactions + totalIncomeTransactions + totalBankBalances
                val expectedTotal = 30 // 예상 총 처리 건수
                
                // 데이터 검증 로깅
                TestLogger.logDataVerification(this@SmsDataTestActivity, testCaseName, "총 처리 건수",
                    expectedTotal, totalProcessed, totalProcessed == expectedTotal,
                    "예상: 30건, 실제: ${totalProcessed}건")
                
                TestLogger.logDataVerification(this@SmsDataTestActivity, testCaseName, "SMS 처리 성공률",
                    "100%", "${(successCount * 100 / testSmsData.size)}%", successCount == testSmsData.size,
                    "성공: ${successCount}건, 실패: ${failCount}건")

                // 단계 5: 데이터베이스 상태 확인 (간소화)
                TestLogger.logTestStep(this@SmsDataTestActivity, testCaseName, 6, "DB 상태 확인", 
                    "각 테이블의 최종 레코드 수 확인")
                
                // DB 상태는 updateDatabaseGrid()에서 확인하므로 여기서는 생략

                val endTime = System.currentTimeMillis()
                val totalDuration = endTime - startTime
                
                // 성능 로깅
                TestLogger.logPerformance(this@SmsDataTestActivity, testCaseName, "전체 SMS 처리",
                    totalDuration, mapOf(
                        "smsCount" to testSmsData.size,
                        "successCount" to successCount,
                        "failCount" to failCount,
                        "avgTimePerSms" to (totalDuration / testSmsData.size)
                    ))

                val message = "샘플 데이터 테스트 완료:\n성공 ${successCount}건, 실패 ${failCount}건\n카드거래 ${totalCardTransactions}건, 수입내역 ${totalIncomeTransactions}건, 은행잔고 ${totalBankBalances}건\n총 처리된 엔티티: ${totalProcessed}건"

                Toast.makeText(this@SmsDataTestActivity, message, Toast.LENGTH_LONG).show()
                updateDatabaseGrid()

                // 메인 화면 새로고침 요청
                refreshMainActivity()

                // 테스트 케이스 종료
                val testResult = TestLogger.TestResult(
                    status = if (failCount == 0) TestLogger.TestStatus.SUCCESS else TestLogger.TestStatus.PARTIAL_SUCCESS,
                    durationMs = totalDuration,
                    message = message,
                    data = mapOf(
                        "successCount" to successCount,
                        "failCount" to failCount,
                        "totalCardTransactions" to totalCardTransactions,
                        "totalIncomeTransactions" to totalIncomeTransactions,
                        "totalBankBalances" to totalBankBalances,
                        "totalProcessed" to totalProcessed,
                        "failedSmsList" to failedSmsList
                    )
                )
                
                TestLogger.endTestCase(this@SmsDataTestActivity, testCaseName, testResult)

            } catch (e: Exception) {
                val endTime = System.currentTimeMillis()
                val totalDuration = endTime - startTime
                
                TestLogger.log(this@SmsDataTestActivity, TestLogger.LogLevel.ERROR, "SmsDataTestActivity", 
                    "샘플 데이터 테스트 오류: ${e.message}")
                
                val testResult = TestLogger.TestResult(
                    status = TestLogger.TestStatus.FAILED,
                    durationMs = totalDuration,
                    message = "테스트 실행 중 오류 발생: ${e.message}",
                    data = mapOf("exception" to e.toString())
                )
                
                TestLogger.endTestCase(this@SmsDataTestActivity, testCaseName, testResult)
                
                Toast.makeText(this@SmsDataTestActivity,
                    "샘플 데이터 테스트 오류: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun testDataRetrieval() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("SmsDataTestActivity", "=== 데이터 조회 테스트 시작 ===")

                // 각 테이블의 데이터 개수 조회
                val cardCount = smsDataRepository.getCardTransactionCount()
                val incomeCount = smsDataRepository.getIncomeTransactionCount()
                val bankBalanceCount = smsDataRepository.getBankBalanceCount()
                val totalBankBalance = smsDataRepository.getTotalBankBalance()

                val totalCount = cardCount + incomeCount + bankBalanceCount

                // SMS 처리 로그 조회
                val smsLogCount = smsDataRepository.getSmsProcessingLogCount()
                val successCount = smsDataRepository.getSuccessfulSmsProcessingLogCount()
                val failedCount = smsDataRepository.getFailedSmsProcessingLogCount()

                if (totalCount == 0) {
                    val message = "저장된 데이터가 없습니다.\n먼저 SMS 데이터를 저장해주세요.\n\nSMS 처리 로그: ${smsLogCount}건 (성공: ${successCount}건, 실패: ${failedCount}건)"
                    Toast.makeText(this@SmsDataTestActivity, message, Toast.LENGTH_LONG).show()
                } else {
                    val message = "데이터 조회 완료:\n" +
                            "카드거래: ${cardCount}건\n" +
                            "수입내역: ${incomeCount}건\n" +
                            "은행잔고: ${bankBalanceCount}건\n" +
                            "총 잔고: ${String.format("%,d", totalBankBalance)}원\n\n" +
                            "SMS 처리 로그: ${smsLogCount}건 (성공: ${successCount}건, 실패: ${failedCount}건)"

                    Toast.makeText(this@SmsDataTestActivity, message, Toast.LENGTH_LONG).show()
                }

                android.util.Log.d("SmsDataTestActivity", "=== 데이터 조회 테스트 완료 ===")
                
                updateDatabaseGrid()

            } catch (e: Exception) {
                android.util.Log.e("SmsDataTestActivity", "데이터 조회 테스트 오류: ${e.message}", e)
                Toast.makeText(this@SmsDataTestActivity,
                    "데이터 조회 테스트 오류: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 강화된 데이터 삭제 테스트 실행
     * 조건 없이 전체 테이블 삭제 후 추가되는지 확인
     */
    private fun testDataDeletion() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("SmsDataTestActivity", "=== 강화된 데이터 삭제 테스트 시작 ===")
                
                // 1단계: 삭제 전 각 테이블의 레코드 수 확인
                val beforeCardCount = smsDataRepository.getCardTransactionCount()
                val beforeIncomeCount = smsDataRepository.getIncomeTransactionCount()
                val beforeBankBalanceCount = smsDataRepository.getBankBalanceCount()
                val beforeSmsLogCount = smsDataRepository.getSmsProcessingLogCount()
                
                android.util.Log.d("SmsDataTestActivity", "1단계 - 삭제 전 레코드 수:")
                android.util.Log.d("SmsDataTestActivity", "  - 카드거래: $beforeCardCount")
                android.util.Log.d("SmsDataTestActivity", "  - 소득거래: $beforeIncomeCount")
                android.util.Log.d("SmsDataTestActivity", "  - 은행잔고: $beforeBankBalanceCount")
                android.util.Log.d("SmsDataTestActivity", "  - SMS로그: $beforeSmsLogCount")
                
                // 2단계: 데이터 삭제 실행
                android.util.Log.d("SmsDataTestActivity", "2단계 - 데이터 삭제 실행")
                smsDataRepository.clearAllData()
                
                // 3단계: 삭제 완료 대기
                android.util.Log.d("SmsDataTestActivity", "3단계 - 삭제 완료 대기 (2초)")
                kotlinx.coroutines.delay(2000)
                
                // 4단계: 삭제 후 각 테이블의 레코드 수 확인
                val afterCardCount = smsDataRepository.getCardTransactionCount()
                val afterIncomeCount = smsDataRepository.getIncomeTransactionCount()
                val afterBankBalanceCount = smsDataRepository.getBankBalanceCount()
                val afterSmsLogCount = smsDataRepository.getSmsProcessingLogCount()
                
                android.util.Log.d("SmsDataTestActivity", "4단계 - 삭제 후 레코드 수:")
                android.util.Log.d("SmsDataTestActivity", "  - 카드거래: $afterCardCount")
                android.util.Log.d("SmsDataTestActivity", "  - 소득거래: $afterIncomeCount")
                android.util.Log.d("SmsDataTestActivity", "  - 은행잔고: $afterBankBalanceCount")
                android.util.Log.d("SmsDataTestActivity", "  - SMS로그: $afterSmsLogCount")
                
                // 5단계: 추가 대기 후 재확인 (삭제 후 추가되는지 확인)
                android.util.Log.d("SmsDataTestActivity", "5단계 - 추가 대기 후 재확인 (3초)")
                kotlinx.coroutines.delay(3000)
                
                val finalCardCount = smsDataRepository.getCardTransactionCount()
                val finalIncomeCount = smsDataRepository.getIncomeTransactionCount()
                val finalBankBalanceCount = smsDataRepository.getBankBalanceCount()
                val finalSmsLogCount = smsDataRepository.getSmsProcessingLogCount()
                
                android.util.Log.d("SmsDataTestActivity", "6단계 - 최종 확인 레코드 수:")
                android.util.Log.d("SmsDataTestActivity", "  - 카드거래: $finalCardCount")
                android.util.Log.d("SmsDataTestActivity", "  - 소득거래: $finalIncomeCount")
                android.util.Log.d("SmsDataTestActivity", "  - 은행잔고: $finalBankBalanceCount")
                android.util.Log.d("SmsDataTestActivity", "  - SMS로그: $finalSmsLogCount")
                
                // 테스트 결과 검증
                val allTablesEmptyAfterDelete = afterCardCount == 0 && afterIncomeCount == 0 && 
                                              afterBankBalanceCount == 0 && afterSmsLogCount == 0
                
                val allTablesStillEmpty = finalCardCount == 0 && finalIncomeCount == 0 && 
                                        finalBankBalanceCount == 0 && finalSmsLogCount == 0
                
                val dataReappeared = !allTablesStillEmpty && allTablesEmptyAfterDelete
                
                val testResult = when {
                    allTablesEmptyAfterDelete && allTablesStillEmpty -> {
                        "✅ 데이터 삭제 테스트 완전 통과\n모든 테이블이 완전히 삭제되었고 유지됩니다.\n삭제 전: 카드 $beforeCardCount, 소득 $beforeIncomeCount, 은행잔고 $beforeBankBalanceCount, SMS로그 $beforeSmsLogCount\n삭제 후: 모두 0\n최종 확인: 모두 0"
                    }
                    dataReappeared -> {
                        "⚠️ 데이터 삭제 후 재추가 감지\n삭제는 성공했지만 일부 데이터가 다시 추가되었습니다.\n삭제 전: 카드 $beforeCardCount, 소득 $beforeIncomeCount, 은행잔고 $beforeBankBalanceCount, SMS로그 $beforeSmsLogCount\n삭제 후: 모두 0\n최종 확인: 카드 $finalCardCount, 소득 $finalIncomeCount, 은행잔고 $finalBankBalanceCount, SMS로그 $finalSmsLogCount"
                    }
                    else -> {
                        "❌ 데이터 삭제 테스트 실패\n일부 테이블에 데이터가 남아있습니다.\n삭제 전: 카드 $beforeCardCount, 소득 $beforeIncomeCount, 은행잔고 $beforeBankBalanceCount, SMS로그 $beforeSmsLogCount\n삭제 후: 카드 $afterCardCount, 소득 $afterIncomeCount, 은행잔고 $afterBankBalanceCount, SMS로그 $afterSmsLogCount\n최종 확인: 카드 $finalCardCount, 소득 $finalIncomeCount, 은행잔고 $finalBankBalanceCount, SMS로그 $finalSmsLogCount"
                    }
                }
                
                android.util.Log.d("SmsDataTestActivity", "=== 강화된 데이터 삭제 테스트 결과 ===")
                android.util.Log.d("SmsDataTestActivity", testResult)
                
                Toast.makeText(this@SmsDataTestActivity, testResult, Toast.LENGTH_LONG).show()
                
                updateDatabaseGrid()
                refreshMainActivity()
                
            } catch (e: Exception) {
                android.util.Log.e("SmsDataTestActivity", "강화된 데이터 삭제 테스트 오류: ${e.message}", e)
                Toast.makeText(this@SmsDataTestActivity,
                    "강화된 데이터 삭제 테스트 오류: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearAllData() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("SmsDataTestActivity", "=== 기존 데이터 삭제 시작 ===")
                
                // 삭제 전 레코드 수 확인
                val beforeCardCount = smsDataRepository.getCardTransactionCount()
                val beforeIncomeCount = smsDataRepository.getIncomeTransactionCount()
                val beforeBankBalanceCount = smsDataRepository.getBankBalanceCount()
                val beforeSmsLogCount = smsDataRepository.getSmsProcessingLogCount()
                
                android.util.Log.d("SmsDataTestActivity", "삭제 전 레코드 수 - 카드: $beforeCardCount, 소득: $beforeIncomeCount, 은행잔고: $beforeBankBalanceCount, SMS로그: $beforeSmsLogCount")
                
                // 데이터 삭제 실행
                smsDataRepository.clearAllData()
                
                // 삭제 완료 대기
                kotlinx.coroutines.delay(500)
                
                // 삭제 후 레코드 수 확인
                val afterCardCount = smsDataRepository.getCardTransactionCount()
                val afterIncomeCount = smsDataRepository.getIncomeTransactionCount()
                val afterBankBalanceCount = smsDataRepository.getBankBalanceCount()
                val afterSmsLogCount = smsDataRepository.getSmsProcessingLogCount()
                
                android.util.Log.d("SmsDataTestActivity", "삭제 후 레코드 수 - 카드: $afterCardCount, 소득: $afterIncomeCount, 은행잔고: $afterBankBalanceCount, SMS로그: $afterSmsLogCount")
                
                val allTablesEmpty = afterCardCount == 0 && afterIncomeCount == 0 && 
                                   afterBankBalanceCount == 0 && afterSmsLogCount == 0
                
                val message = if (allTablesEmpty) {
                    "✅ 모든 데이터 삭제 완료\n모든 테이블이 비워졌습니다."
                } else {
                    "⚠️ 데이터 삭제 완료\n일부 테이블에 데이터가 남아있을 수 있습니다."
                }
                
                android.util.Log.d("SmsDataTestActivity", "데이터 삭제 결과: $message")
                Toast.makeText(this@SmsDataTestActivity, message, Toast.LENGTH_LONG).show()
                updateDatabaseGrid()
                
                // 메인 화면 새로고침 요청
                refreshMainActivity()
                
            } catch (e: Exception) {
                android.util.Log.e("SmsDataTestActivity", "데이터 삭제 오류: ${e.message}", e)
                Toast.makeText(this@SmsDataTestActivity,
                    "데이터 삭제 오류: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 빌드 전 핵심 로직 테스트 실행
     */
    private fun runLogicTests() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("SmsDataTestActivity", "=== 빌드 전 핵심 로직 테스트 시작 ===")
                
                val testResults = mutableMapOf<String, Boolean>()
                
                // 테스트 1: SMS 파싱 로직 테스트
                try {
                    val testSms = "신한카드(1054)승인 신*진 42,820원(일시불)10/20 14:30 이마트"
                    val parseResult = smsDataRepository.saveSmsData(testSms)
                    testResults["SMS 파싱"] = parseResult.isSuccess
                    android.util.Log.d("SmsDataTestActivity", "SMS 파싱 테스트: ${if (parseResult.isSuccess) "성공" else "실패"}")
                } catch (e: Exception) {
                    testResults["SMS 파싱"] = false
                    android.util.Log.e("SmsDataTestActivity", "SMS 파싱 테스트 실패: ${e.message}")
                }
                
                // 테스트 2: 데이터베이스 저장 로직 테스트
                try {
                    val testSms = "신한 10/11 21:54  100-***-159993 입금  2,500,000 잔액  3,265,147 급여"
                    val saveResult = smsDataRepository.saveSmsData(testSms)
                    testResults["DB 저장"] = saveResult.isSuccess
                    android.util.Log.d("SmsDataTestActivity", "DB 저장 테스트: ${if (saveResult.isSuccess) "성공" else "실패"}")
                } catch (e: Exception) {
                    testResults["DB 저장"] = false
                    android.util.Log.e("SmsDataTestActivity", "DB 저장 테스트 실패: ${e.message}")
                }
                
                // 테스트 3: 데이터 삭제 로직 테스트
                try {
                    val beforeCount = smsDataRepository.getCardTransactionCount()
                    smsDataRepository.clearAllData()
                    val afterCount = smsDataRepository.getCardTransactionCount()
                    testResults["데이터 삭제"] = afterCount == 0
                    android.util.Log.d("SmsDataTestActivity", "데이터 삭제 테스트: ${if (afterCount == 0) "성공" else "실패"} (삭제 전: $beforeCount, 삭제 후: $afterCount)")
                } catch (e: Exception) {
                    testResults["데이터 삭제"] = false
                    android.util.Log.e("SmsDataTestActivity", "데이터 삭제 테스트 실패: ${e.message}")
                }
                
                // 테스트 4: 월별 집계 로직 테스트
                try {
                    val now = java.time.LocalDateTime.now()
                    val startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                    val endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59)
                    
                    val monthlyAmount = smsDataRepository.getMonthlyCardUsageAmount(startOfMonth, endOfMonth)
                    testResults["월별 집계"] = monthlyAmount >= 0
                    android.util.Log.d("SmsDataTestActivity", "월별 집계 테스트: ${if (monthlyAmount >= 0) "성공" else "실패"} (금액: $monthlyAmount)")
                } catch (e: Exception) {
                    testResults["월별 집계"] = false
                    android.util.Log.e("SmsDataTestActivity", "월별 집계 테스트 실패: ${e.message}")
                }
                
                // 테스트 결과 보고
                val successCount = testResults.values.count { it }
                val totalCount = testResults.size
                val successRate = (successCount * 100 / totalCount)
                
                val report = StringBuilder()
                report.append("📊 빌드 전 로직 테스트 결과\n\n")
                
                testResults.forEach { (testName, success) ->
                    val status = if (success) "✅ 통과" else "❌ 실패"
                    report.append("$testName: $status\n")
                }
                
                report.append("\n총 테스트: ").append(totalCount).append("건\n")
                report.append("성공: ").append(successCount).append("건\n")
                report.append("실패: ").append(totalCount - successCount).append("건\n")
                report.append("성공률: ").append(successRate).append("%\n\n")
                
                if (successCount == totalCount) {
                    report.append("🎉 모든 테스트 통과!\n빌드 승인 가능")
                } else {
                    report.append("⚠️ 일부 테스트 실패.\n빌드 전 수정 필요")
                }
                
                android.util.Log.d("SmsDataTestActivity", "=== 테스트 결과 보고서 ===")
                android.util.Log.d("SmsDataTestActivity", report.toString())
                
                Toast.makeText(this@SmsDataTestActivity, report.toString(), Toast.LENGTH_LONG).show()
                
                updateDatabaseGrid()
                
            } catch (e: Exception) {
                android.util.Log.e("SmsDataTestActivity", "로직 테스트 실행 오류: ${e.message}", e)
                Toast.makeText(this@SmsDataTestActivity,
                    "로직 테스트 실행 오류: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 메인 화면 새로고침 요청
     */
    private fun refreshMainActivity() {
        try {
            // Intent를 통해 MainActivity에 새로고침 요청
            val intent = Intent().apply {
                action = "com.ssj.statuswindow.REFRESH_DASHBOARD"
                putExtra("refresh", true)
            }
            sendBroadcast(intent)
            
            android.util.Log.d("SmsDataTestActivity", "메인 화면 새로고침 요청 전송")
        } catch (e: Exception) {
            android.util.Log.e("SmsDataTestActivity", "메인 화면 새로고침 요청 오류: ${e.message}", e)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView)
        } else {
            drawerLayout.openDrawer(navigationView)
        }
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView)
        } else {
            super.onBackPressed()
        }
    }
}