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
import com.ssj.statuswindow.ui.adapter.BankTransactionAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*

/**
 * 입출금내역 상세페이지
 */
class BankTransactionActivity : AppCompatActivity() {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar
    private lateinit var navigationView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvSummary: TextView
    private lateinit var btnDeleteAll: Button
    
    private lateinit var database: StatusWindowDatabase
    private lateinit var adapter: BankTransactionAdapter
    private val transactions = mutableListOf<BankTransactionEntity>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank_transaction)
        
        // 데이터베이스 초기화
        database = StatusWindowDatabase.getDatabase(this)
        
        setupViews()
        setupToolbar()
        setupNavigation()
        setupRecyclerView()
        setupClickListeners()
        loadBankTransactions()
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
        supportActionBar?.title = "입출금내역"
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
                    // 현재 페이지
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_card_table -> {
                    // 카드 테이블 페이지로 이동
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
    
    private fun setupRecyclerView() {
        adapter = BankTransactionAdapter(transactions) { transaction ->
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
            .setMessage("모든 입출금내역을 삭제하시겠습니까?\n\n이 작업은 되돌릴 수 없습니다.")
            .setPositiveButton("삭제") { _, _ ->
                deleteAllBankTransactions()
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun showDeleteConfirmationDialog(transaction: BankTransactionEntity) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("거래 삭제 확인")
            .setMessage("이 거래를 삭제하시겠습니까?\n\n${transaction.description} - ${NumberFormat.getNumberInstance(Locale.KOREA).format(transaction.amount)}원")
            .setPositiveButton("삭제") { _, _ ->
                deleteBankTransaction(transaction)
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun deleteAllBankTransactions() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bankTransactionDao = database.bankTransactionDao()
                
                // 모든 데이터 삭제
                bankTransactionDao.deleteAllBankTransactions()
                
                withContext(Dispatchers.Main) {
                    // UI 업데이트
                    transactions.clear()
                    adapter.notifyDataSetChanged()
                    tvSummary.text = "📊 입출금내역 요약\n\n총 거래: 0건\n총 입금: 0원\n총 출금: 0원"
                    
                    android.widget.Toast.makeText(this@BankTransactionActivity, "모든 데이터가 삭제되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@BankTransactionActivity, "삭제 오류: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun deleteBankTransaction(transaction: BankTransactionEntity) {
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
                    
                    android.widget.Toast.makeText(this@BankTransactionActivity, "거래가 삭제되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@BankTransactionActivity, "삭제 오류: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun loadBankTransactions() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bankTransactionDao = database.bankTransactionDao()
                val allTransactions = bankTransactionDao.getAllBankTransactions()
                
                allTransactions.collect { transactionList ->
                    withContext(Dispatchers.Main) {
                        transactions.clear()
                        transactions.addAll(transactionList)
                        adapter.notifyDataSetChanged()
                        updateSummary(transactionList)
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
        
        val totalDeposit = transactionList.filter { it.transactionType == "입금" }.sumOf { it.amount }
        val totalWithdrawal = transactionList.filter { it.transactionType == "출금" }.sumOf { it.amount }
        val netAmount = totalDeposit - totalWithdrawal
        val bankStats = transactionList.groupBy { it.bankName }
        
        val summary = StringBuilder()
        summary.append("📊 입출금내역 요약\n\n")
        summary.append("총 거래 건수: ${transactionList.size}건\n")
        summary.append("총 입금: ${formatter.format(totalDeposit)}원\n")
        summary.append("총 출금: ${formatter.format(totalWithdrawal)}원\n")
        summary.append("순수입: ${formatter.format(netAmount)}원\n\n")
        
        summary.append("🏦 은행별 거래내역\n")
        summary.append("-".repeat(30)).append("\n")
        
        bankStats.forEach { (bankName, transactions) ->
            val bankDeposit = transactions.filter { it.transactionType == "입금" }.sumOf { it.amount }
            val bankWithdrawal = transactions.filter { it.transactionType == "출금" }.sumOf { it.amount }
            val bankNet = bankDeposit - bankWithdrawal
            summary.append("${bankName}: ${transactions.size}건, 입금 ${formatter.format(bankDeposit)}원, 출금 ${formatter.format(bankWithdrawal)}원, 순수입 ${formatter.format(bankNet)}원\n")
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
