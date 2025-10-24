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
import com.ssj.statuswindow.database.entity.CreditCardUsageEntity
import com.ssj.statuswindow.ui.adapter.CreditCardDetailAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*

/**
 * 카드 사용내역 상세페이지
 */
class CardDetailsActivity : AppCompatActivity() {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar
    private lateinit var navigationView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvSummary: TextView
    private lateinit var btnDeleteAll: Button
    
    private lateinit var database: StatusWindowDatabase
    private lateinit var adapter: CreditCardDetailAdapter
    private val transactions = mutableListOf<CreditCardUsageEntity>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_details)
        
        // 데이터베이스 초기화
        database = StatusWindowDatabase.getDatabase(this)
        
        setupViews()
        setupToolbar()
        setupNavigation()
        setupRecyclerView()
        setupClickListeners()
        loadCardDetails()
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
        supportActionBar?.title = "카드 사용내역"
    }
    
    private fun setupNavigation() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    finish() // 메인으로 돌아가기
                    true
                }
                R.id.nav_card_details -> {
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
        adapter = CreditCardDetailAdapter(transactions)
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
            .setMessage("모든 카드 사용내역을 삭제하시겠습니까?\n\n이 작업은 되돌릴 수 없습니다.")
            .setPositiveButton("삭제") { _, _ ->
                deleteAllCardDetails()
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun deleteAllCardDetails() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val creditCardUsageDao = database.creditCardUsageDao()
                val cardTransactionDao = database.cardTransactionDao()
                
                // 모든 데이터 삭제
                creditCardUsageDao.deleteAllCreditCardUsage()
                cardTransactionDao.deleteAllCardTransactions()
                
                withContext(Dispatchers.Main) {
                    // UI 업데이트
                    transactions.clear()
                    adapter.notifyDataSetChanged()
                    tvSummary.text = "📊 카드 사용내역 요약\n\n총 거래: 0건\n총 사용금액: 0원\n총 청구금액: 0원"
                    
                    android.widget.Toast.makeText(this@CardDetailsActivity, "모든 데이터가 삭제되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@CardDetailsActivity, "삭제 오류: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun loadCardDetails() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val creditCardUsageDao = database.creditCardUsageDao()
                val allTransactions = creditCardUsageDao.getAllCreditCardUsage()
                
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
    
    private fun updateSummary(transactionList: List<CreditCardUsageEntity>) {
        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        
        val totalAmount = transactionList.sumOf { it.amount }
        val totalBillingAmount = transactionList.sumOf { it.billingAmount }
        val categoryStats = transactionList.groupBy { it.merchantCategory }
        
        val summary = StringBuilder()
        summary.append("📊 카드 사용내역 요약\n\n")
        summary.append("총 거래 건수: ${transactionList.size}건\n")
        summary.append("총 사용금액: ${formatter.format(totalAmount)}원\n")
        summary.append("총 청구금액: ${formatter.format(totalBillingAmount)}원\n\n")
        
        summary.append("📈 카테고리별 사용내역\n")
        summary.append("-".repeat(30)).append("\n")
        
        categoryStats.forEach { (category, transactions) ->
            val categoryAmount = transactions.sumOf { it.amount }
            val categoryCount = transactions.size
            summary.append("${category}: ${categoryCount}건, ${formatter.format(categoryAmount)}원\n")
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
