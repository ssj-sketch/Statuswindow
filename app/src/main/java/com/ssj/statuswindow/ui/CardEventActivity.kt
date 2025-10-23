package com.ssj.statuswindow.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.ssj.statuswindow.R
import com.ssj.statuswindow.databinding.ActivityCardEventBinding
import com.ssj.statuswindow.model.CardTransaction
import com.ssj.statuswindow.repo.CardEventRepository
import com.ssj.statuswindow.ui.adapter.CardTransactionAdapter
import com.ssj.statuswindow.viewmodel.MainViewModel
import com.ssj.statuswindow.viewmodel.MainViewModelFactory
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CardEventActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCardEventBinding
    private val cardTransactionAdapter = CardTransactionAdapter()
    private val scope = MainScope()
    
    private val vm: MainViewModel by viewModels {
        MainViewModelFactory(CardEventRepository.instance(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeTransactions()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "카드 내역"
    }

    private fun setupRecyclerView() {
        binding.recyclerViewCardTransactions.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewCardTransactions.adapter = cardTransactionAdapter
        
        // 삭제 버튼 클릭 리스너
        cardTransactionAdapter.setOnDeleteClickListener { transaction ->
            showDeleteConfirmDialog(transaction)
        }
        
        // 아이템 클릭 리스너 (상세내용 표시)
        cardTransactionAdapter.setOnItemClickListener { transaction ->
            showTransactionDetailDialog(transaction)
        }
    }
    
    private fun observeTransactions() {
        scope.launch {
            vm.transactions.collectLatest { transactions ->
                cardTransactionAdapter.submitList(transactions)
                updateMonthlyTotal(transactions)
            }
        }
    }

    private fun updateMonthlyTotal(transactions: List<CardTransaction>) {
        val currentMonth = java.time.LocalDate.now().monthValue
        val currentYear = java.time.LocalDate.now().year
        
        val monthlyTransactions = transactions.filter { transaction ->
            transaction.transactionDate.monthValue == currentMonth && 
            transaction.transactionDate.year == currentYear
        }
        
        val totalAmount = monthlyTransactions.sumOf { it.amount }
        val transactionCount = monthlyTransactions.size
        
        binding.tvMonthlyTotal.text = "${String.format("%,d", totalAmount)}원"
        binding.tvMonthlyCount.text = "${transactionCount}건"
    }

    private fun showTransactionDetailDialog(transaction: CardTransaction) {
        val detailMessage = """
            가맹점: ${transaction.merchant}
            금액: ${String.format("%,d", transaction.amount)}원
            카드: ${transaction.cardType}(${transaction.cardNumber})
            사용자: ${transaction.user}
            거래일시: ${transaction.transactionDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}
            할부: ${transaction.installment}
            누적금액: ${String.format("%,d", transaction.cumulativeAmount)}원
            원본 SMS: ${transaction.originalText}
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("거래 상세내용")
            .setMessage(detailMessage)
            .setPositiveButton("확인", null)
            .show()
    }
    
    private fun showDeleteConfirmDialog(transaction: CardTransaction) {
        AlertDialog.Builder(this)
            .setTitle("거래 삭제")
            .setMessage("${transaction.merchant} 거래를 삭제하시겠습니까?\n금액: ${String.format("%,d", transaction.amount)}원")
            .setPositiveButton("삭제") { _, _ ->
                vm.removeTransaction(transaction)
                Snackbar.make(binding.root, "거래가 삭제되었습니다", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
