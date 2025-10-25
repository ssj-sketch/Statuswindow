package com.ssj.statuswindow.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.database.StatusWindowDatabase
import com.ssj.statuswindow.database.entity.BankTransactionEntity
import com.ssj.statuswindow.util.NavigationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * 입출금내역 화면 (CardDetailsActivity와 동일한 구조)
 */
class BankTransactionActivity : AppCompatActivity() {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar
    private lateinit var navigationView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvTotalCount: TextView
    private lateinit var tvTotalDeposit: TextView
    private lateinit var tvTotalWithdrawal: TextView
    private lateinit var btnSortDropdown: Button
    private lateinit var btnDeleteAll: Button
    
    private lateinit var database: StatusWindowDatabase
    private val bankTransactions = mutableListOf<BankTransactionEntity>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank_transaction)
        
        try {
            // 데이터베이스 초기화
            database = StatusWindowDatabase.getDatabase(this)
            
            setupViews()
            setupToolbar()
            setupNavigation()
            loadBankTransactions()
            
        } catch (e: Exception) {
            android.util.Log.e("BankTransactionActivity", "초기화 오류: ${e.message}", e)
            // 오류 발생 시 기본 텍스트뷰 표시
            val errorView = TextView(this).apply {
                text = "입출금내역을 불러올 수 없습니다.\n오류: ${e.message}"
                textSize = 16f
                setPadding(32, 32, 32, 32)
            }
            setContentView(errorView)
        }
    }
    
    private fun setupViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        toolbar = findViewById(R.id.toolbar)
        navigationView = findViewById(R.id.navigationView)
        recyclerView = findViewById(R.id.recyclerView)
        tvTotalCount = findViewById(R.id.tvTotalCount)
        tvTotalDeposit = findViewById(R.id.tvTotalDeposit)
        tvTotalWithdrawal = findViewById(R.id.tvTotalWithdrawal)
        btnSortDropdown = findViewById(R.id.btnSortDropdown)
        btnDeleteAll = findViewById(R.id.btnDeleteAll)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "🏦 입출금내역"
    }
    
    private fun setupNavigation() {
        NavigationManager.setupNavigation(this, navigationView, drawerLayout, BankTransactionActivity::class.java)
        NavigationManager.setActiveMenuItem(navigationView, BankTransactionActivity::class.java)
    }
    
    private fun loadBankTransactions() {
        lifecycleScope.launch {
            try {
                val transactionList = withContext(Dispatchers.IO) {
                    database.bankTransactionDao().getAllBankTransactions()
                }
                
                transactionList.collect { transactions ->
                    bankTransactions.clear()
                    bankTransactions.addAll(transactions)
                    
                    withContext(Dispatchers.Main) {
                        updateSummary()
                        setupRecyclerView()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BankTransactionActivity", "데이터 로드 오류: ${e.message}", e)
                Toast.makeText(this@BankTransactionActivity, "데이터를 불러올 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateSummary() {
        val totalCount = bankTransactions.size
        val totalDeposit = bankTransactions.filter { it.transactionType == "입금" }.sumOf { it.amount }
        val totalWithdrawal = bankTransactions.filter { it.transactionType == "출금" }.sumOf { it.amount }
        
        tvTotalCount.text = "${totalCount}건"
        tvTotalDeposit.text = "${String.format("%,d", totalDeposit)}원"
        tvTotalWithdrawal.text = "${String.format("%,d", totalWithdrawal)}원"
    }
    
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        // TODO: BankTransactionAdapter 구현 필요
        // recyclerView.adapter = BankTransactionAdapter(bankTransactions)
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
            else -> super.onOptionsItemSelected(item)
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