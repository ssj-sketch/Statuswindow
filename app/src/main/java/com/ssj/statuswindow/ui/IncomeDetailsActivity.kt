package com.ssj.statuswindow.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.database.StatusWindowDatabase
import com.ssj.statuswindow.database.entity.BankTransactionEntity
import com.ssj.statuswindow.ui.adapter.IncomeDetailAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*

/**
 * 소득 상세페이지
 */
class IncomeDetailsActivity : AppCompatActivity() {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar
    private lateinit var navigationView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvSummary: TextView
    private lateinit var btnDeleteAll: Button
    
    private lateinit var database: StatusWindowDatabase
    private lateinit var adapter: IncomeDetailAdapter
    private val transactions = mutableListOf<BankTransactionEntity>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_income_details)
        
        // 데이터베이스 초기화
        database = StatusWindowDatabase.getDatabase(this)
        
        setupViews()
        setupToolbar()
        setupNavigation()
        setupRecyclerView()
        setupClickListeners()
        loadIncomeDetails()
    }
    
    private fun setupViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        toolbar = findViewById(R.id.toolbar)
        navigationView = findViewById(R.id.navigationView)
        recyclerView = findViewById(R.id.recyclerView)
        tvSummary = findViewById(R.id.tvSummary)
        btnDeleteAll = findViewById(R.id.btnDeleteAll)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
        supportActionBar?.title = "소득 상세내역"
    }
    
    private fun setupNavigation() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    finish() // 메인으로 돌아가기
                    true
                }
                R.id.nav_card_details -> {
                    // 카드 사용내역 페이지로 이동
                    startActivity(android.content.Intent(this, CardDetailsActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_bank_transaction -> {
                    // 입출금내역 페이지로 이동
                    startActivity(android.content.Intent(this, BankTransactionActivity::class.java))
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
    
    private fun setupRecyclerView() {
        adapter = IncomeDetailAdapter(transactions) { transaction ->
            showDeleteConfirmationDialog(transaction)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    private fun setupClickListeners() {
        btnDeleteAll.setOnClickListener {
            showDeleteAllConfirmationDialog()
        }
    }
    
    private fun showDeleteAllConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("전체 삭제 확인")
            .setMessage("모든 소득내역을 삭제하시겠습니까?\n\n이 작업은 되돌릴 수 없습니다.")
            .setPositiveButton("삭제") { _, _ ->
                deleteAllIncomeDetails()
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun showDeleteConfirmationDialog(transaction: BankTransactionEntity) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("소득 삭제 확인")
            .setMessage("이 소득을 삭제하시겠습니까?\n\n${transaction.description} - ${NumberFormat.getNumberInstance(Locale.KOREA).format(transaction.amount)}원")
            .setPositiveButton("삭제") { _, _ ->
                deleteIncomeDetail(transaction)
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun deleteAllIncomeDetails() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bankTransactionDao = database.bankTransactionDao()
                
                // 입금 거래만 삭제
                val allTransactions = bankTransactionDao.getAllBankTransactions()
                allTransactions.collect { transactionList ->
                    val incomeTransactions = transactionList.filter { it.transactionType == "입금" }
                    for (transaction in incomeTransactions) {
                        bankTransactionDao.deleteBankTransaction(transaction)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    // UI 업데이트
                    transactions.clear()
                    adapter.notifyDataSetChanged()
                    tvSummary.text = "📊 소득 상세내역 요약\n\n총 소득: 0건\n총 소득금액: 0원"
                    
                    android.widget.Toast.makeText(this@IncomeDetailsActivity, "모든 소득내역이 삭제되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@IncomeDetailsActivity, "삭제 오류: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun deleteIncomeDetail(transaction: BankTransactionEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bankTransactionDao = database.bankTransactionDao()
                
                // 개별 데이터 삭제
                bankTransactionDao.deleteBankTransaction(transaction)
                
                withContext(Dispatchers.Main) {
                    // UI 업데이트
                    transactions.remove(transaction)
                    adapter.notifyDataSetChanged()
                    updateSummary(transactions)
                    
                    android.widget.Toast.makeText(this@IncomeDetailsActivity, "소득이 삭제되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@IncomeDetailsActivity, "삭제 오류: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun loadIncomeDetails() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bankTransactionDao = database.bankTransactionDao()
                val allTransactions = bankTransactionDao.getAllBankTransactions()
                
                allTransactions.collect { transactionList ->
                    // 입금 거래만 필터링
                    val incomeTransactions = transactionList.filter { it.transactionType == "입금" }
                    
                    withContext(Dispatchers.Main) {
                        transactions.clear()
                        transactions.addAll(incomeTransactions)
                        adapter.notifyDataSetChanged()
                        updateSummary(incomeTransactions)
                    }
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    tvSummary.text = "❌ 데이터 로드 오류: ${e.message}"
                }
            }
        }
    }
    
    private fun updateSummary(transactionList: List<BankTransactionEntity>) {
        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        
        val totalIncome = transactionList.sumOf { it.amount }
        val bankStats = transactionList.groupBy { it.bankName }
        
        val summary = StringBuilder()
        summary.append("📊 소득 상세내역 요약\n\n")
        summary.append("총 소득 건수: ${transactionList.size}건\n")
        summary.append("총 소득금액: ${formatter.format(totalIncome)}원\n\n")
        
        summary.append("🏦 은행별 소득내역\n")
        summary.append("-".repeat(30)).append("\n")
        
        bankStats.forEach { (bankName, transactions) ->
            val bankIncome = transactions.sumOf { it.amount }
            summary.append("${bankName}: ${transactions.size}건, ${formatter.format(bankIncome)}원\n")
        }
        
        tvSummary.text = summary.toString()
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
