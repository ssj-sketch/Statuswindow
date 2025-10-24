package com.ssj.statuswindow.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ssj.statuswindow.R
import com.ssj.statuswindow.databinding.ActivitySmsDataTestBinding
import com.ssj.statuswindow.repo.database.SmsDataRepository
import kotlinx.coroutines.launch

/**
 * SMS 데이터 저장 테스트를 위한 Activity
 */
class SmsDataTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySmsDataTestBinding
    private lateinit var smsDataRepository: SmsDataRepository

    // 테스트용 SMS 데이터
    private val testSmsData = listOf(
        "신한카드(1054)승인 신*진 98,700원(일시불)10/13 15:48 가톨릭대병원 누적1,960,854원",
        "신한 10/11 21:55  100-***-159993 입금   1,000,000 잔액  4,265,147",
        "신한 10/11 21:54  100-***-159993 입금  2,500,000 잔액  3,265,147 급여",
        "신한 09/11 21:54  100-***-159993 입금 급여  2,500,000 잔액  5,265,147 급여",
        "신한 08/11 21:54  100-***-159993 입금 급여  2,500,000 잔액  4,265,147 급여",
        "신한 07/11 21:54  100-***-159993 입금 급여  2,500,000 잔액  3,265,147 급여"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySmsDataTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 툴바 설정
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        smsDataRepository = SmsDataRepository(this)

        setupClickListeners()
        updateDatabaseGrid()
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
                onBackPressed()
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

    private fun clearAllData() {
        lifecycleScope.launch {
            try {
                smsDataRepository.clearAllData()
                Toast.makeText(this@SmsDataTestActivity,
                    "모든 데이터 삭제 완료",
                    Toast.LENGTH_SHORT).show()
                updateDatabaseGrid()
            } catch (e: Exception) {
                Toast.makeText(this@SmsDataTestActivity,
                    "데이터 삭제 오류: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}