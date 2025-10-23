package com.ssj.statuswindow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ssj.statuswindow.databinding.ActivityIncomeDetailBinding
import com.ssj.statuswindow.model.IncomeInfo
import com.ssj.statuswindow.repo.IncomeRepository
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class IncomeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncomeDetailBinding
    private lateinit var incomeAdapter: IncomeAdapter
    private lateinit var incomeRepo: IncomeRepository
    private val nf = NumberFormat.getNumberInstance(Locale.KOREA)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncomeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // IncomeRepository 초기화 (Context가 준비된 후)
        incomeRepo = IncomeRepository(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "수입 상세내역"

        setupRecyclerView()
        updateIncomeSummary()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
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
        val incomes = incomeRepo.getAllIncomes()
        
        // 샘플 데이터 추가 비활성화 (실제 데이터만 사용)
        // if (incomes.isEmpty()) {
        //     addSampleIncomeData()
        // }
        
        val updatedIncomes = incomeRepo.getAllIncomes()
        incomeAdapter.submitList(updatedIncomes)
        
        // 데이터가 없을 때 안내 메시지 표시
        if (updatedIncomes.isEmpty()) {
            binding.tvMonthlyIncomeTotal.text = "0원"
            binding.tvIncomeCount.text = "0건"
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

    private fun updateIncomeSummary() {
        // 이번달 수입
        val monthlyIncome = incomeRepo.getCurrentMonthIncome()
        binding.tvMonthlyIncomeTotal.text = "${nf.format(monthlyIncome)}원"
        
        val monthlyIncomeCount = incomeRepo.getAllIncomes().count { income ->
            val today = java.time.LocalDate.now()
            income.transactionDate.year == today.year &&
            income.transactionDate.monthValue == today.monthValue
        }
        binding.tvIncomeCount.text = "${monthlyIncomeCount}건"
        
        // 누적 수입
        val totalIncome = incomeRepo.getTotalIncome()
        binding.tvTotalIncome.text = "${nf.format(totalIncome)}원"
        
        val totalIncomeCount = incomeRepo.getAllIncomes().size
        binding.tvTotalIncomeCount.text = "${totalIncomeCount}건"
        
        // 급여/부업 구분
        val salaryIncome = incomeRepo.getCurrentMonthSalaryIncome()
        binding.tvSalaryIncome.text = "${nf.format(salaryIncome)}원"
        
        val sideJobIncome = incomeRepo.getCurrentMonthSideJobIncome()
        binding.tvSideJobIncome.text = "${nf.format(sideJobIncome)}원"
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
                text2.text = "${dateFormat.format(Date.from(income.transactionDate.atZone(java.time.ZoneId.systemDefault()).toInstant()))} | ${income.bankName}"
            }
        }
    }
}
