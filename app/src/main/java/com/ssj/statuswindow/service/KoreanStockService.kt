package com.ssj.statuswindow.service

import com.ssj.statuswindow.model.KoreanStock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 한국 주식 정보 서비스
 */
object KoreanStockService {
    
    // 주요 한국 주식 종목 데이터 (실제로는 API에서 가져와야 함)
    private val majorStocks = listOf(
        KoreanStock("005930", "삼성전자", "KOSPI", 70000L, 1.2, 1000000L),
        KoreanStock("000660", "SK하이닉스", "KOSPI", 120000L, -0.8, 500000L),
        KoreanStock("035420", "NAVER", "KOSPI", 180000L, 2.1, 300000L),
        KoreanStock("035720", "카카오", "KOSPI", 45000L, -1.5, 800000L),
        KoreanStock("207940", "삼성바이오로직스", "KOSPI", 850000L, 0.5, 100000L),
        KoreanStock("006400", "삼성SDI", "KOSPI", 400000L, 1.8, 200000L),
        KoreanStock("051910", "LG화학", "KOSPI", 450000L, -0.3, 150000L),
        KoreanStock("068270", "셀트리온", "KOSPI", 180000L, 0.9, 250000L),
        KoreanStock("323410", "카카오뱅크", "KOSDAQ", 35000L, 2.5, 600000L),
        KoreanStock("086520", "에코프로", "KOSDAQ", 250000L, 3.2, 100000L),
        KoreanStock("247540", "에코프로비엠", "KOSDAQ", 180000L, 1.8, 80000L),
        KoreanStock("091990", "셀트리온헬스케어", "KOSDAQ", 120000L, -0.5, 120000L),
        KoreanStock("066570", "LG전자", "KOSPI", 95000L, 0.7, 300000L),
        KoreanStock("003550", "LG", "KOSPI", 85000L, -0.2, 200000L),
        KoreanStock("012330", "현대모비스", "KOSPI", 220000L, 1.1, 180000L),
        KoreanStock("000270", "기아", "KOSPI", 45000L, 2.3, 400000L),
        KoreanStock("105560", "KB금융", "KOSPI", 55000L, 0.4, 250000L),
        KoreanStock("055550", "신한지주", "KOSPI", 40000L, 0.8, 300000L),
        KoreanStock("086790", "하나금융지주", "KOSPI", 45000L, 0.6, 200000L),
        KoreanStock("003670", "포스코홀딩스", "KOSPI", 380000L, -1.2, 100000L)
    )
    
    /**
     * 주식 검색 (종목명 또는 종목코드)
     */
    suspend fun searchStocks(query: String): List<KoreanStock> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        
        val filteredStocks = majorStocks.filter { stock ->
            stock.name.contains(query, ignoreCase = true) || 
            stock.code.contains(query, ignoreCase = true)
        }
        
        // 실제로는 API 호출로 구현해야 함
        return@withContext filteredStocks.take(10) // 최대 10개만 반환
    }
    
    /**
     * 종목코드로 주식 정보 조회
     */
    suspend fun getStockByCode(code: String): KoreanStock? = withContext(Dispatchers.IO) {
        return@withContext majorStocks.find { it.code == code }
    }
    
    /**
     * 실시간 주가 조회 (실제로는 API 연동 필요)
     */
    suspend fun getCurrentPrice(code: String): Long? = withContext(Dispatchers.IO) {
        try {
            // 실제 구현에서는 금융 API 호출
            // 예: 한국투자증권 API, 네이버 금융 API 등
            val stock = majorStocks.find { it.code == code }
            return@withContext stock?.currentPrice
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
    
    /**
     * 인기 종목 조회
     */
    suspend fun getPopularStocks(): List<KoreanStock> = withContext(Dispatchers.IO) {
        return@withContext majorStocks.take(10)
    }
    
    /**
     * 시장구분별 종목 조회
     */
    suspend fun getStocksByMarket(market: String): List<KoreanStock> = withContext(Dispatchers.IO) {
        return@withContext majorStocks.filter { it.market == market }
    }
}
