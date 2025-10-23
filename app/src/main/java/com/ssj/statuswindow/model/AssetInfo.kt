package com.ssj.statuswindow.model

import java.time.LocalDateTime

/**
 * 자산 정보 데이터 모델
 */
data class AssetInfo(
    val id: Long = 0,
    val assetType: AssetType,           // 자산 유형
    val name: String,                   // 자산명
    val value: Long,                    // 자산 가치
    val description: String = "",       // 설명
    val lastUpdated: LocalDateTime,     // 마지막 업데이트 시간
    val memo: String = ""               // 메모
)

/**
 * 자산 유형
 */
enum class AssetType {
    BANK_BALANCE,       // 은행잔고
    REAL_ESTATE,        // 부동산
    STOCK,             // 주식
    BOND,              // 채권
    FUND,              // 펀드
    CRYPTO,            // 암호화폐
    OTHER              // 기타
}

/**
 * 은행잔고 정보
 */
data class BankBalance(
    val id: Long = 0,
    val bankName: String,               // 은행명
    val accountNumber: String,          // 계좌번호 (마지막 4자리)
    val balance: Long,                  // 잔고
    val accountType: String,            // 계좌유형 (예적금, 입출금 등)
    val lastUpdated: LocalDateTime,
    val memo: String = ""
)

/**
 * 부동산 정보
 */
data class RealEstate(
    val id: Long = 0,
    val address: String,                // 주소
    val propertyType: String,          // 부동산 유형 (아파트, 빌라, 상가 등)
    val area: Double,                  // 면적 (제곱미터)
    val estimatedValue: Long,          // 추정 가치
    val purchasePrice: Long,           // 매입가
    val purchaseDate: LocalDateTime,   // 매입일
    val lastUpdated: LocalDateTime,
    val memo: String = ""
)

/**
 * 주식 포트폴리오 정보
 */
data class StockPortfolio(
    val id: Long = 0,
    val stockCode: String,             // 종목코드
    val stockName: String,             // 종목명
    val quantity: Int,                 // 보유수량
    val averagePrice: Long,            // 평균단가
    val currentPrice: Long,            // 현재가
    val totalValue: Long,              // 총 평가금액
    val profitLoss: Long,              // 손익
    val profitLossRate: Double,        // 손익률
    val lastUpdated: LocalDateTime,
    val memo: String = ""
)

/**
 * 한국 주식 정보
 */
data class KoreanStock(
    val code: String,                   // 종목코드
    val name: String,                   // 종목명
    val market: String,                 // 시장구분 (KOSPI, KOSDAQ, KONEX)
    val currentPrice: Long,             // 현재가
    val changeRate: Double,            // 등락률
    val volume: Long                   // 거래량
)
