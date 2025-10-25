package com.ssj.statuswindow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.databinding.ActivityIncomeDetailBinding
import com.ssj.statuswindow.model.IncomeInfo
import com.ssj.statuswindow.repo.IncomeRepository
import com.ssj.statuswindow.repo.database.SmsDataRepository
import com.ssj.statuswindow.database.StatusWindowDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class IncomeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncomeDetailBinding
    private lateinit var incomeAdapter: IncomeAdapter
    private lateinit var incomeRepo: IncomeRepository
    private lateinit var smsDataRepository: SmsDataRepository
    private lateinit var database: StatusWindowDatabase
    private val nf = NumberFormat.getNumberInstance(Locale.KOREA)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncomeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // IncomeRepository 초기화 (Context가 준비된 후)
        incomeRepo = IncomeRepository(this)
        smsDataRepository = SmsDataRepository(this)
        database = StatusWindowDatabase.getDatabase(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "수입 상세내역"

        setupRecyclerView()
        loadIncomeData()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.income_detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_refresh -> {
                loadIncomeData()
                Toast.makeText(this, "데이터를 새로고침했습니다.", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_export -> {
                Toast.makeText(this, "데이터 내보내기 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_settings -> {
                Toast.makeText(this, "설정 화면은 준비 중입니다.", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_help -> {
                Toast.makeText(this, "도움말: 수입 내역을 확인하고 관리할 수 있습니다.", Toast.LENGTH_LONG).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        incomeAdapter = IncomeAdapter()
        binding.recyclerViewIncome.apply {
            layoutManager = LinearLayoutManager(this@IncomeDetailActivity)
            adapter = incomeAdapter
        }

        // 수입 데이터 로드
        loadIncomeData()
    }
    
    private fun loadIncomeData() {
        lifecycleScope.launch {
            try {
                // Room 데이터베이스에서 수입 내역 조회
                val incomeTransactions = smsDataRepository.getIncomeTransactions()
                
                incomeTransactions.collect { transactions ->
                    // IncomeTransactionEntity를 IncomeInfo로 변환
                    val incomeInfos = transactions.map { entity ->
                        IncomeInfo(
                            amount = entity.amount,
                            source = entity.description,
                            bankName = entity.bankName,
                            transactionDate = entity.transactionDate,
                            description = entity.description,
                            isRecurring = entity.description.contains("급여")
                        )
                    }
                    
                    // null 값이 있는 경우 필터링
                    val validIncomes = incomeInfos.filter { it.transactionDate != null }
                    
                    incomeAdapter.submitList(validIncomes)
                    
                    // 데이터가 없을 때 안내 메시지 표시
                    if (validIncomes.isEmpty()) {
                        binding.tvMonthlyIncomeTotal.text = "0원"
                        binding.tvIncomeCount.text = "0건"
                        Toast.makeText(this@IncomeDetailActivity, "저장된 수입 내역이 없습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        updateIncomeSummary(validIncomes)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("IncomeDetailActivity", "수입 데이터 로드 오류: ${e.message}", e)
                Toast.makeText(this@IncomeDetailActivity, "수입 데이터 로드 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun addSampleIncomeData() {
        val currentDate = java.time.LocalDateTime.now()
        
        // 샘플 급여 데이터 추가 (실제 입금 내역이 있는 경우만)
        val sampleSalary = IncomeInfo(
            amount = 2500000L,
            source = "급여",
            bankName = "신한은행",
            transactionDate = currentDate.minusDays(5),
            description = "급여", // "급여"로 명시하여 급여로 인식
            isRecurring = true
        )
        incomeRepo.addIncome(sampleSalary)
        
        // 부업 수입은 실제 입금 내역이 있을 때만 추가
        // (현재는 샘플 데이터이므로 주석 처리)
        /*
        val sampleSideJob = IncomeInfo(
            amount = 500000L,
            source = "부업",
            bankName = "카카오뱅크",
            transactionDate = currentDate.minusDays(10),
            description = "프리랜서",
            isRecurring = false
        )
        incomeRepo.addIncome(sampleSideJob)
        */
    }

    private fun updateIncomeSummary(incomes: List<IncomeInfo> = emptyList()) {
        // DB에서 직접 조회
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bankTransactionDao = database.bankTransactionDao()
                
                // 현재 월의 시작과 끝 날짜 계산
                val now = java.time.LocalDateTime.now()
                val startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                val endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59)
                
                // DB에서 수입 데이터 조회
                val monthlyIncome = bankTransactionDao.getTotalAmountByDateRange(startOfMonth, endOfMonth) ?: 0L
                val monthlyIncomeCount = bankTransactionDao.getTransactionCountByDateRange(startOfMonth, endOfMonth)
                
                // 전체 기간 수입 데이터 조회
                val totalIncome = bankTransactionDao.getTotalAmount() ?: 0L
                val totalIncomeCount = bankTransactionDao.getTotalTransactionCount()
                
                // 메인 스레드에서 UI 업데이트
                withContext(Dispatchers.Main) {
                    binding.tvMonthlyIncomeTotal.text = "${nf.format(monthlyIncome)}원"
                    binding.tvIncomeCount.text = "${monthlyIncomeCount}건"
                    binding.tvTotalIncome.text = "${nf.format(totalIncome)}원"
                    binding.tvTotalIncomeCount.text = "${totalIncomeCount}건"
                    
                    // 급여/부업 구분 (Repository에서 DB 조회)
                    val salaryIncome = incomeRepo.getCurrentMonthSalaryIncome()
                    binding.tvSalaryIncome.text = "${nf.format(salaryIncome)}원"
                    
                    val sideJobIncome = incomeRepo.getCurrentMonthSideJobIncome()
                    binding.tvSideJobIncome.text = "${nf.format(sideJobIncome)}원"
                }
            } catch (e: Exception) {
                android.util.Log.e("IncomeDetailActivity", "DB 조회 오류: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    binding.tvMonthlyIncomeTotal.text = "오류"
                    binding.tvIncomeCount.text = "오류"
                    binding.tvTotalIncome.text = "오류"
                    binding.tvTotalIncomeCount.text = "오류"
                }
            }
        }
    }

    class IncomeAdapter : RecyclerView.Adapter<IncomeAdapter.IncomeViewHolder>() {

        private var incomes: List<IncomeInfo> = emptyList()
        private val nf = NumberFormat.getNumberInstance(Locale.KOREA)
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        fun submitList(list: List<IncomeInfo>) {
            incomes = list.sortedByDescending { it.transactionDate }
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomeViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(android.R.layout.simple_list_item_2, parent, false)
            return IncomeViewHolder(view)
        }

        override fun onBindViewHolder(holder: IncomeViewHolder, position: Int) {
            val income = incomes[position]
            holder.bind(income)
        }

        override fun getItemCount(): Int = incomes.size

        inner class IncomeViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            private val text1: TextView = itemView.findViewById(android.R.id.text1)
            private val text2: TextView = itemView.findViewById(android.R.id.text2)

            fun bind(income: IncomeInfo) {
                text1.text = "${income.source} - ${nf.format(income.amount)}원"
                val dateText = income.transactionDate?.let { date ->
                    dateFormat.format(Date.from(date.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                } ?: "날짜 없음"
                text2.text = "$dateText | ${income.bankName}"
            }
        }
    }
}
