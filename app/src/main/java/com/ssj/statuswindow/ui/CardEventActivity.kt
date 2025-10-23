package com.ssj.statuswindow.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssj.statuswindow.databinding.ActivityCardEventBinding
import com.ssj.statuswindow.model.CardTransaction
import com.ssj.statuswindow.ui.adapter.CardTransactionAdapter

class CardEventActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCardEventBinding
    private val cardTransactionAdapter = CardTransactionAdapter()
    private val cardTransactions = mutableListOf<CardTransaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        updateMonthlyTotal()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "카드 내역"
    }

    private fun setupRecyclerView() {
        binding.recyclerViewCardTransactions.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewCardTransactions.adapter = cardTransactionAdapter
        
        // TODO: 실제 데이터 로드
        updateCardTransactionList()
    }

    private fun updateCardTransactionList() {
        cardTransactionAdapter.submitList(cardTransactions)
    }

    private fun updateMonthlyTotal() {
        val currentMonth = java.time.LocalDate.now().monthValue
        val currentYear = java.time.LocalDate.now().year
        
        val monthlyTransactions = cardTransactions.filter { transaction ->
            transaction.transactionDate.monthValue == currentMonth && 
            transaction.transactionDate.year == currentYear
        }
        
        val totalAmount = monthlyTransactions.sumOf { it.amount }
        val transactionCount = monthlyTransactions.size
        
        binding.tvMonthlyTotal.text = "${String.format("%,d", totalAmount)}원"
        binding.tvMonthlyCount.text = "${transactionCount}건"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
