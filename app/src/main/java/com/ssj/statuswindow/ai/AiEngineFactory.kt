package com.ssj.statuswindow.ai

import com.ssj.statuswindow.model.CardTransaction
import com.ssj.statuswindow.model.BankBalance

/**
 * 국가별 AI 엔진 팩토리
 */
object AiEngineFactory {
    
    // 지원되는 국가 목록
    private val supportedCountries = mapOf(
        "KR" to "대한민국",
        "US" to "United States",
        "JP" to "日本",
        "CN" to "中国",
        "GB" to "United Kingdom",
        "DE" to "Deutschland",
        "FR" to "France",
        "CA" to "Canada",
        "AU" to "Australia"
    )
    
    // AI 엔진 인스턴스 캐시
    private val engineCache = mutableMapOf<String, CountrySpecificAiEngine>()
    
    /**
     * 국가 코드로 AI 엔진을 가져옴
     */
    fun getEngine(countryCode: String): CountrySpecificAiEngine? {
        // 캐시에서 확인
        engineCache[countryCode]?.let { return it }
        
        // 새 엔진 생성
        val engine = createEngine(countryCode)
        engine?.let { 
            engineCache[countryCode] = it
            it.initialize()
        }
        
        return engine
    }
    
    /**
     * SMS 텍스트를 분석하여 가장 적합한 국가의 AI 엔진을 반환
     */
    fun detectBestEngine(smsText: String): CountrySpecificAiEngine? {
        val trimmedText = smsText.trim()
        if (trimmedText.isBlank()) return null
        
        var bestEngine: CountrySpecificAiEngine? = null
        var bestScore = 0.0f
        
        // 지원되는 모든 국가의 엔진을 테스트
        for (countryCode in supportedCountries.keys) {
            val engine = getEngine(countryCode)
            if (engine != null) {
                val score = engine.getConfidenceScore(trimmedText)
                if (score > bestScore) {
                    bestScore = score
                    bestEngine = engine
                }
            }
        }
        
        // 최소 신뢰도 임계값 이상인 경우만 반환
        return if (bestScore >= 0.5f) bestEngine else null
    }
    
    /**
     * 국가 코드로 AI 엔진 생성
     */
    private fun createEngine(countryCode: String): CountrySpecificAiEngine? {
        return when (countryCode.uppercase()) {
            "KR" -> KoreanAiEngine()
            "US" -> AmericanAiEngine()
            "JP" -> JapaneseAiEngine() // 추후 구현
            "CN" -> ChineseAiEngine() // 추후 구현
            "GB" -> BritishAiEngine() // 추후 구현
            "DE" -> GermanAiEngine() // 추후 구현
            "FR" -> FrenchAiEngine() // 추후 구현
            "CA" -> CanadianAiEngine() // 추후 구현
            "AU" -> AustralianAiEngine() // 추후 구현
            else -> null
        }
    }
    
    /**
     * 지원되는 국가 목록 반환
     */
    fun getSupportedCountries(): Map<String, String> {
        return supportedCountries.toMap()
    }
    
    /**
     * 특정 국가가 지원되는지 확인
     */
    fun isCountrySupported(countryCode: String): Boolean {
        return supportedCountries.containsKey(countryCode.uppercase())
    }
    
    /**
     * 모든 엔진 정리
     */
    fun cleanupAllEngines() {
        engineCache.values.forEach { it.cleanup() }
        engineCache.clear()
    }
    
    /**
     * 특정 국가의 엔진 정리
     */
    fun cleanupEngine(countryCode: String) {
        engineCache[countryCode]?.cleanup()
        engineCache.remove(countryCode)
    }
}

/**
 * 일본용 AI 엔진 (추후 구현)
 */
class JapaneseAiEngine : CountrySpecificAiEngine {
    override fun getCountryCode(): String = "JP"
    override fun getCountryName(): String = "日本"
    
    override fun initialize() {}
    override fun cleanup() {}
    override fun extractCardTransaction(smsText: String): CardTransaction? = null
    override fun extractBankBalance(smsText: String): BankBalance? = null
    override fun extractSalaryInfo(smsText: String): SalaryInfo? = null
    override fun extractAutoTransferInfo(smsText: String): AutoTransferInfo? = null
    override fun getConfidenceScore(smsText: String): Float = 0.0f
}

/**
 * 중국용 AI 엔진 (추후 구현)
 */
class ChineseAiEngine : CountrySpecificAiEngine {
    override fun getCountryCode(): String = "CN"
    override fun getCountryName(): String = "中国"
    
    override fun initialize() {}
    override fun cleanup() {}
    override fun extractCardTransaction(smsText: String): CardTransaction? = null
    override fun extractBankBalance(smsText: String): BankBalance? = null
    override fun extractSalaryInfo(smsText: String): SalaryInfo? = null
    override fun extractAutoTransferInfo(smsText: String): AutoTransferInfo? = null
    override fun getConfidenceScore(smsText: String): Float = 0.0f
}

/**
 * 영국용 AI 엔진 (추후 구현)
 */
class BritishAiEngine : CountrySpecificAiEngine {
    override fun getCountryCode(): String = "GB"
    override fun getCountryName(): String = "United Kingdom"
    
    override fun initialize() {}
    override fun cleanup() {}
    override fun extractCardTransaction(smsText: String): CardTransaction? = null
    override fun extractBankBalance(smsText: String): BankBalance? = null
    override fun extractSalaryInfo(smsText: String): SalaryInfo? = null
    override fun extractAutoTransferInfo(smsText: String): AutoTransferInfo? = null
    override fun getConfidenceScore(smsText: String): Float = 0.0f
}

/**
 * 독일용 AI 엔진 (추후 구현)
 */
class GermanAiEngine : CountrySpecificAiEngine {
    override fun getCountryCode(): String = "DE"
    override fun getCountryName(): String = "Deutschland"
    
    override fun initialize() {}
    override fun cleanup() {}
    override fun extractCardTransaction(smsText: String): CardTransaction? = null
    override fun extractBankBalance(smsText: String): BankBalance? = null
    override fun extractSalaryInfo(smsText: String): SalaryInfo? = null
    override fun extractAutoTransferInfo(smsText: String): AutoTransferInfo? = null
    override fun getConfidenceScore(smsText: String): Float = 0.0f
}

/**
 * 프랑스용 AI 엔진 (추후 구현)
 */
class FrenchAiEngine : CountrySpecificAiEngine {
    override fun getCountryCode(): String = "FR"
    override fun getCountryName(): String = "France"
    
    override fun initialize() {}
    override fun cleanup() {}
    override fun extractCardTransaction(smsText: String): CardTransaction? = null
    override fun extractBankBalance(smsText: String): BankBalance? = null
    override fun extractSalaryInfo(smsText: String): SalaryInfo? = null
    override fun extractAutoTransferInfo(smsText: String): AutoTransferInfo? = null
    override fun getConfidenceScore(smsText: String): Float = 0.0f
}

/**
 * 캐나다용 AI 엔진 (추후 구현)
 */
class CanadianAiEngine : CountrySpecificAiEngine {
    override fun getCountryCode(): String = "CA"
    override fun getCountryName(): String = "Canada"
    
    override fun initialize() {}
    override fun cleanup() {}
    override fun extractCardTransaction(smsText: String): CardTransaction? = null
    override fun extractBankBalance(smsText: String): BankBalance? = null
    override fun extractSalaryInfo(smsText: String): SalaryInfo? = null
    override fun extractAutoTransferInfo(smsText: String): AutoTransferInfo? = null
    override fun getConfidenceScore(smsText: String): Float = 0.0f
}

/**
 * 호주용 AI 엔진 (추후 구현)
 */
class AustralianAiEngine : CountrySpecificAiEngine {
    override fun getCountryCode(): String = "AU"
    override fun getCountryName(): String = "Australia"
    
    override fun initialize() {}
    override fun cleanup() {}
    override fun extractCardTransaction(smsText: String): CardTransaction? = null
    override fun extractBankBalance(smsText: String): BankBalance? = null
    override fun extractSalaryInfo(smsText: String): SalaryInfo? = null
    override fun extractAutoTransferInfo(smsText: String): AutoTransferInfo? = null
    override fun getConfidenceScore(smsText: String): Float = 0.0f
}
