package com.ssj.statuswindow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.ssj.statuswindow.R
import com.ssj.statuswindow.databinding.ActivityAssetManagementBinding
import com.ssj.statuswindow.model.AssetType
import com.ssj.statuswindow.model.BankBalance
import com.ssj.statuswindow.model.KoreanStock
import com.ssj.statuswindow.model.RealEstate
import com.ssj.statuswindow.model.StockPortfolio
import com.ssj.statuswindow.repo.AssetRepository
import com.ssj.statuswindow.service.KoreanStockService
import com.ssj.statuswindow.service.RealEstateService
import com.ssj.statuswindow.ui.adapter.BankBalanceAdapter
import com.ssj.statuswindow.ui.adapter.RealEstateAdapter
import com.ssj.statuswindow.ui.adapter.StockPortfolioAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDateTime
import java.util.Locale

class AssetManagementActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAssetManagementBinding
    private val assetRepo = AssetRepository.getInstance()
    private val scope = CoroutineScope(Dispatchers.Main)
    private val nf = NumberFormat.getIntegerInstance(Locale.KOREA)
    
    // 탭별 RecyclerView
    private lateinit var bankBalanceAdapter: BankBalanceAdapter
    private lateinit var realEstateAdapter: RealEstateAdapter
    private lateinit var stockPortfolioAdapter: StockPortfolioAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssetManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupTabs()
        setupRecyclerViews()
        setupFloatingActionButton()
        observeData()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "자산 관리"
    }
    
    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("은행잔고"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("부동산"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("주식"))
        
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showBankBalanceTab()
                    1 -> showRealEstateTab()
                    2 -> showStockTab()
                }
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
        })
        
        // 기본 탭 선택
        showBankBalanceTab()
    }
    
    private fun setupRecyclerViews() {
        // 은행잔고 RecyclerView
        bankBalanceAdapter = BankBalanceAdapter { bankBalance ->
            showBankBalanceEditDialog(bankBalance)
        }
        binding.recyclerViewBankBalance.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewBankBalance.adapter = bankBalanceAdapter
        
        // 부동산 RecyclerView
        realEstateAdapter = RealEstateAdapter { realEstate ->
            showRealEstateEditDialog(realEstate)
        }
        binding.recyclerViewRealEstate.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewRealEstate.adapter = realEstateAdapter
        
        // 주식 포트폴리오 RecyclerView
        stockPortfolioAdapter = StockPortfolioAdapter { stockPortfolio ->
            showStockPortfolioEditDialog(stockPortfolio)
        }
        binding.recyclerViewStock.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewStock.adapter = stockPortfolioAdapter
    }
    
    private fun setupFloatingActionButton() {
        binding.fabAdd.setOnClickListener {
            when (binding.tabLayout.selectedTabPosition) {
                0 -> showBankBalanceEditDialog()
                1 -> showRealEstateEditDialog()
                2 -> showStockPortfolioEditDialog()
            }
        }
    }
    
    private fun observeData() {
        scope.launch {
            assetRepo.bankBalances.collect { bankBalances ->
                bankBalanceAdapter.submitList(bankBalances)
                updateTotalAssetValue()
            }
        }
        
        scope.launch {
            assetRepo.realEstates.collect { realEstates ->
                realEstateAdapter.submitList(realEstates)
                updateTotalAssetValue()
            }
        }
        
        scope.launch {
            assetRepo.stockPortfolios.collect { stockPortfolios ->
                stockPortfolioAdapter.submitList(stockPortfolios)
                updateTotalAssetValue()
            }
        }
    }
    
    private fun updateTotalAssetValue() {
        val totalValue = assetRepo.getTotalAssetValue()
        binding.tvTotalAssetValue.text = "${nf.format(totalValue)}원"
    }
    
    private fun showBankBalanceTab() {
        binding.recyclerViewBankBalance.visibility = View.VISIBLE
        binding.recyclerViewRealEstate.visibility = View.GONE
        binding.recyclerViewStock.visibility = View.GONE
    }
    
    private fun showRealEstateTab() {
        binding.recyclerViewBankBalance.visibility = View.GONE
        binding.recyclerViewRealEstate.visibility = View.VISIBLE
        binding.recyclerViewStock.visibility = View.GONE
    }
    
    private fun showStockTab() {
        binding.recyclerViewBankBalance.visibility = View.GONE
        binding.recyclerViewRealEstate.visibility = View.GONE
        binding.recyclerViewStock.visibility = View.VISIBLE
    }
    
    // 은행잔고 편집 다이얼로그
    private fun showBankBalanceEditDialog(bankBalance: BankBalance? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_bank_balance, null)
        
        val etBankName = dialogView.findViewById<EditText>(R.id.etBankName)
        val etAccountNumber = dialogView.findViewById<EditText>(R.id.etAccountNumber)
        val etBalance = dialogView.findViewById<EditText>(R.id.etBalance)
        val etAccountType = dialogView.findViewById<EditText>(R.id.etAccountType)
        val etMemo = dialogView.findViewById<EditText>(R.id.etMemo)
        
        // 기존 데이터가 있으면 입력
        bankBalance?.let {
            etBankName.setText(it.bankName)
            etAccountNumber.setText(it.accountNumber)
            etBalance.setText(it.balance.toString())
            etAccountType.setText(it.accountType)
            etMemo.setText(it.memo)
        }
        
        AlertDialog.Builder(this)
            .setTitle(if (bankBalance == null) "은행잔고 추가" else "은행잔고 수정")
            .setView(dialogView)
            .setPositiveButton("저장") { _, _ ->
                val newBankBalance = BankBalance(
                    id = bankBalance?.id ?: 0,
                    bankName = etBankName.text.toString(),
                    accountNumber = etAccountNumber.text.toString(),
                    balance = etBalance.text.toString().toLongOrNull() ?: 0L,
                    accountType = etAccountType.text.toString(),
                    lastUpdated = LocalDateTime.now(),
                    memo = etMemo.text.toString()
                )
                assetRepo.addBankBalance(newBankBalance)
            }
            .setNegativeButton("취소", null)
            .setNeutralButton("삭제") { _, _ ->
                bankBalance?.let {
                    assetRepo.removeBankBalance(it.id)
                }
            }
            .show()
    }
    
    // 부동산 편집 다이얼로그
    private fun showRealEstateEditDialog(realEstate: RealEstate? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_real_estate, null)
        
        val etAddress = dialogView.findViewById<EditText>(R.id.etAddress)
        val etPropertyType = dialogView.findViewById<EditText>(R.id.etPropertyType)
        val etArea = dialogView.findViewById<EditText>(R.id.etArea)
        val etEstimatedValue = dialogView.findViewById<EditText>(R.id.etEstimatedValue)
        val etPurchasePrice = dialogView.findViewById<EditText>(R.id.etPurchasePrice)
        val etMemo = dialogView.findViewById<EditText>(R.id.etMemo)
        val btnSearchValue = dialogView.findViewById<MaterialButton>(R.id.btnSearchValue)
        
        // 기존 데이터가 있으면 입력
        realEstate?.let {
            etAddress.setText(it.address)
            etPropertyType.setText(it.propertyType)
            etArea.setText(it.area.toString())
            etEstimatedValue.setText(it.estimatedValue.toString())
            etPurchasePrice.setText(it.purchasePrice.toString())
            etMemo.setText(it.memo)
        }
        
        // 부동산 가치 조회 버튼
        btnSearchValue.setOnClickListener {
            val address = etAddress.text.toString()
            if (address.isNotBlank()) {
                scope.launch {
                    val realEstateValue = RealEstateService.getPropertyValue(address)
                    realEstateValue?.let {
                        etEstimatedValue.setText(it.estimatedValue.toString())
                        etPropertyType.setText(it.propertyType)
                        etArea.setText(it.area.toString())
                    }
                }
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle(if (realEstate == null) "부동산 추가" else "부동산 수정")
            .setView(dialogView)
            .setPositiveButton("저장") { _, _ ->
                val newRealEstate = RealEstate(
                    id = realEstate?.id ?: 0,
                    address = etAddress.text.toString(),
                    propertyType = etPropertyType.text.toString(),
                    area = etArea.text.toString().toDoubleOrNull() ?: 0.0,
                    estimatedValue = etEstimatedValue.text.toString().toLongOrNull() ?: 0L,
                    purchasePrice = etPurchasePrice.text.toString().toLongOrNull() ?: 0L,
                    purchaseDate = realEstate?.purchaseDate ?: LocalDateTime.now(),
                    lastUpdated = LocalDateTime.now(),
                    memo = etMemo.text.toString()
                )
                assetRepo.addRealEstate(newRealEstate)
            }
            .setNegativeButton("취소", null)
            .setNeutralButton("삭제") { _, _ ->
                realEstate?.let {
                    assetRepo.removeRealEstate(it.id)
                }
            }
            .show()
    }
    
    // 주식 포트폴리오 편집 다이얼로그
    private fun showStockPortfolioEditDialog(stockPortfolio: StockPortfolio? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_stock_portfolio, null)
        
        val etStockCode = dialogView.findViewById<EditText>(R.id.etStockCode)
        val etStockName = dialogView.findViewById<EditText>(R.id.etStockName)
        val etQuantity = dialogView.findViewById<EditText>(R.id.etQuantity)
        val etAveragePrice = dialogView.findViewById<EditText>(R.id.etAveragePrice)
        val etCurrentPrice = dialogView.findViewById<EditText>(R.id.etCurrentPrice)
        val etMemo = dialogView.findViewById<EditText>(R.id.etMemo)
        val btnSearchStock = dialogView.findViewById<MaterialButton>(R.id.btnSearchStock)
        
        // 기존 데이터가 있으면 입력
        stockPortfolio?.let {
            etStockCode.setText(it.stockCode)
            etStockName.setText(it.stockName)
            etQuantity.setText(it.quantity.toString())
            etAveragePrice.setText(it.averagePrice.toString())
            etCurrentPrice.setText(it.currentPrice.toString())
            etMemo.setText(it.memo)
        }
        
        // 주식 검색 버튼
        btnSearchStock.setOnClickListener {
            val query = etStockCode.text.toString().ifBlank { etStockName.text.toString() }
            if (query.isNotBlank()) {
                scope.launch {
                    val stocks = KoreanStockService.searchStocks(query)
                    if (stocks.isNotEmpty()) {
                        val stock = stocks.first()
                        etStockCode.setText(stock.code)
                        etStockName.setText(stock.name)
                        etCurrentPrice.setText(stock.currentPrice.toString())
                    }
                }
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle(if (stockPortfolio == null) "주식 포트폴리오 추가" else "주식 포트폴리오 수정")
            .setView(dialogView)
            .setPositiveButton("저장") { _, _ ->
                val quantity = etQuantity.text.toString().toIntOrNull() ?: 0
                val averagePrice = etAveragePrice.text.toString().toLongOrNull() ?: 0L
                val currentPrice = etCurrentPrice.text.toString().toLongOrNull() ?: 0L
                val totalValue = quantity * currentPrice
                val profitLoss = (currentPrice - averagePrice) * quantity
                val profitLossRate = if (averagePrice > 0) (currentPrice - averagePrice).toDouble() / averagePrice * 100 else 0.0
                
                val newStockPortfolio = StockPortfolio(
                    id = stockPortfolio?.id ?: 0,
                    stockCode = etStockCode.text.toString(),
                    stockName = etStockName.text.toString(),
                    quantity = quantity,
                    averagePrice = averagePrice,
                    currentPrice = currentPrice,
                    totalValue = totalValue,
                    profitLoss = profitLoss,
                    profitLossRate = profitLossRate,
                    lastUpdated = LocalDateTime.now(),
                    memo = etMemo.text.toString()
                )
                assetRepo.addStockPortfolio(newStockPortfolio)
            }
            .setNegativeButton("취소", null)
            .setNeutralButton("삭제") { _, _ ->
                stockPortfolio?.let {
                    assetRepo.removeStockPortfolio(it.id)
                }
            }
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
