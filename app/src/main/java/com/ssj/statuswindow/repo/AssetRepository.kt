package com.ssj.statuswindow.repo

import com.ssj.statuswindow.model.AssetInfo
import com.ssj.statuswindow.model.AssetType
import com.ssj.statuswindow.model.BankBalance
import com.ssj.statuswindow.model.RealEstate
import com.ssj.statuswindow.model.StockPortfolio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 자산 관리 Repository
 */
class AssetRepository {
    companion object {
        @Volatile
        private var INSTANCE: AssetRepository? = null
        
        fun getInstance(): AssetRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AssetRepository().also { INSTANCE = it }
            }
        }
    }
    
    // 은행잔고 데이터
    private val _bankBalances = MutableStateFlow<List<BankBalance>>(emptyList())
    val bankBalances: StateFlow<List<BankBalance>> = _bankBalances.asStateFlow()
    
    // 부동산 데이터
    private val _realEstates = MutableStateFlow<List<RealEstate>>(emptyList())
    val realEstates: StateFlow<List<RealEstate>> = _realEstates.asStateFlow()
    
    // 주식 포트폴리오 데이터
    private val _stockPortfolios = MutableStateFlow<List<StockPortfolio>>(emptyList())
    val stockPortfolios: StateFlow<List<StockPortfolio>> = _stockPortfolios.asStateFlow()
    
    // 전체 자산 데이터
    private val _allAssets = MutableStateFlow<List<AssetInfo>>(emptyList())
    val allAssets: StateFlow<List<AssetInfo>> = _allAssets.asStateFlow()
    
    /**
     * 은행잔고 추가
     */
    fun addBankBalance(bankBalance: BankBalance) {
        val currentList = _bankBalances.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == bankBalance.id }
        
        if (existingIndex >= 0) {
            currentList[existingIndex] = bankBalance
        } else {
            currentList.add(bankBalance.copy(id = System.currentTimeMillis()))
        }
        
        _bankBalances.value = currentList
        updateAllAssets()
    }
    
    /**
     * 부동산 추가
     */
    fun addRealEstate(realEstate: RealEstate) {
        val currentList = _realEstates.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == realEstate.id }
        
        if (existingIndex >= 0) {
            currentList[existingIndex] = realEstate
        } else {
            currentList.add(realEstate.copy(id = System.currentTimeMillis()))
        }
        
        _realEstates.value = currentList
        updateAllAssets()
    }
    
    /**
     * 주식 포트폴리오 추가
     */
    fun addStockPortfolio(stockPortfolio: StockPortfolio) {
        val currentList = _stockPortfolios.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == stockPortfolio.id }
        
        if (existingIndex >= 0) {
            currentList[existingIndex] = stockPortfolio
        } else {
            currentList.add(stockPortfolio.copy(id = System.currentTimeMillis()))
        }
        
        _stockPortfolios.value = currentList
        updateAllAssets()
    }
    
    /**
     * 은행잔고 삭제
     */
    fun removeBankBalance(id: Long) {
        val currentList = _bankBalances.value.toMutableList()
        currentList.removeAll { it.id == id }
        _bankBalances.value = currentList
        updateAllAssets()
    }
    
    /**
     * 부동산 삭제
     */
    fun removeRealEstate(id: Long) {
        val currentList = _realEstates.value.toMutableList()
        currentList.removeAll { it.id == id }
        _realEstates.value = currentList
        updateAllAssets()
    }
    
    /**
     * 주식 포트폴리오 삭제
     */
    fun removeStockPortfolio(id: Long) {
        val currentList = _stockPortfolios.value.toMutableList()
        currentList.removeAll { it.id == id }
        _stockPortfolios.value = currentList
        updateAllAssets()
    }
    
    /**
     * 전체 자산 업데이트
     */
    private fun updateAllAssets() {
        val allAssetsList = mutableListOf<AssetInfo>()
        
        // 은행잔고 추가
        _bankBalances.value.forEach { bankBalance ->
            allAssetsList.add(
                AssetInfo(
                    id = bankBalance.id,
                    assetType = AssetType.BANK_BALANCE,
                    name = "${bankBalance.bankName} ${bankBalance.accountNumber}",
                    value = bankBalance.balance,
                    description = bankBalance.accountType,
                    lastUpdated = bankBalance.lastUpdated,
                    memo = bankBalance.memo
                )
            )
        }
        
        // 부동산 추가
        _realEstates.value.forEach { realEstate ->
            allAssetsList.add(
                AssetInfo(
                    id = realEstate.id,
                    assetType = AssetType.REAL_ESTATE,
                    name = realEstate.address,
                    value = realEstate.estimatedValue,
                    description = "${realEstate.propertyType} ${realEstate.area}㎡",
                    lastUpdated = realEstate.lastUpdated,
                    memo = realEstate.memo
                )
            )
        }
        
        // 주식 포트폴리오 추가
        _stockPortfolios.value.forEach { stock ->
            allAssetsList.add(
                AssetInfo(
                    id = stock.id,
                    assetType = AssetType.STOCK,
                    name = stock.stockName,
                    value = stock.totalValue,
                    description = "${stock.quantity}주 (${stock.profitLossRate}%)",
                    lastUpdated = stock.lastUpdated,
                    memo = stock.memo
                )
            )
        }
        
        _allAssets.value = allAssetsList
    }
    
    /**
     * 총 자산 가치 계산
     */
    fun getTotalAssetValue(): Long {
        return _allAssets.value.sumOf { it.value }
    }
    
    /**
     * 자산 유형별 총 가치 계산
     */
    fun getAssetValueByType(assetType: AssetType): Long {
        return _allAssets.value.filter { it.assetType == assetType }.sumOf { it.value }
    }
}
