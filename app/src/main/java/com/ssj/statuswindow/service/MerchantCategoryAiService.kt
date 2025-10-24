package com.ssj.statuswindow.service

import android.content.Context
import android.util.Log
import java.util.*

/**
 * 언어별 AI를 사용한 가맹점 카테고리 추론 서비스
 */
class MerchantCategoryAiService(private val context: Context) {
    
    companion object {
        private const val TAG = "MerchantCategoryAi"
        
        // 한국어 카테고리 매핑
        private val KOREAN_CATEGORIES = mapOf(
            // 의료/건강
            "병원" to "의료",
            "의료" to "의료",
            "약국" to "의료",
            "치과" to "의료",
            "한의원" to "의료",
            "카톨릭대병원" to "의료",
            
            // 식품/외식
            "이마트" to "식품",
            "스타벅스" to "외식",
            "맥도날드" to "외식",
            "버거킹" to "외식",
            "피자" to "외식",
            "치킨" to "외식",
            "카페" to "외식",
            "레스토랑" to "외식",
            "식당" to "외식",
            
            // 교통/이동
            "지하철" to "교통",
            "버스" to "교통",
            "택시" to "교통",
            "기차" to "교통",
            "항공" to "교통",
            "주유소" to "교통",
            "주차" to "교통",
            
            // 쇼핑/생활
            "백화점" to "쇼핑",
            "마트" to "쇼핑",
            "편의점" to "쇼핑",
            "온라인" to "쇼핑",
            "쿠팡" to "쇼핑",
            "네이버" to "쇼핑",
            "아마존" to "쇼핑",
            
            // 통신/IT
            "통신" to "통신",
            "SKT" to "통신",
            "KT" to "통신",
            "LG" to "통신",
            "인터넷" to "통신",
            "모바일" to "통신",
            
            // 교육/문화
            "학교" to "교육",
            "학원" to "교육",
            "도서관" to "교육",
            "영화" to "문화",
            "공연" to "문화",
            "박물관" to "문화",
            "놀이공원" to "문화",
            
            // 금융/보험
            "은행" to "금융",
            "카드" to "금융",
            "보험" to "금융",
            "증권" to "금융",
            
            // 기타
            "주유" to "기타",
            "세탁" to "기타",
            "미용" to "기타",
            "헬스" to "기타"
        )
        
        // 영어 카테고리 매핑
        private val ENGLISH_CATEGORIES = mapOf(
            // Medical/Health
            "hospital" to "Medical",
            "medical" to "Medical",
            "pharmacy" to "Medical",
            "dental" to "Medical",
            "clinic" to "Medical",
            
            // Food/Dining
            "restaurant" to "Dining",
            "cafe" to "Dining",
            "coffee" to "Dining",
            "starbucks" to "Dining",
            "mcdonalds" to "Dining",
            "burger" to "Dining",
            "pizza" to "Dining",
            "food" to "Dining",
            
            // Transportation
            "taxi" to "Transportation",
            "uber" to "Transportation",
            "lyft" to "Transportation",
            "gas" to "Transportation",
            "fuel" to "Transportation",
            "parking" to "Transportation",
            "metro" to "Transportation",
            "bus" to "Transportation",
            
            // Shopping
            "store" to "Shopping",
            "shop" to "Shopping",
            "mall" to "Shopping",
            "amazon" to "Shopping",
            "online" to "Shopping",
            "retail" to "Shopping",
            
            // Communication/IT
            "phone" to "Communication",
            "mobile" to "Communication",
            "internet" to "Communication",
            "telecom" to "Communication",
            "verizon" to "Communication",
            "att" to "Communication",
            
            // Education/Culture
            "school" to "Education",
            "university" to "Education",
            "library" to "Education",
            "movie" to "Culture",
            "theater" to "Culture",
            "museum" to "Culture",
            "entertainment" to "Culture",
            
            // Financial
            "bank" to "Financial",
            "credit" to "Financial",
            "insurance" to "Financial",
            "investment" to "Financial"
        )
        
        // 일본어 카테고리 매핑
        private val JAPANESE_CATEGORIES = mapOf(
            // 医療/健康
            "病院" to "医療",
            "医療" to "医療",
            "薬局" to "医療",
            "歯科" to "医療",
            
            // 飲食
            "レストラン" to "飲食",
            "カフェ" to "飲食",
            "コーヒー" to "飲食",
            "スターバックス" to "飲食",
            "マクドナルド" to "飲食",
            "ピザ" to "飲食",
            
            // 交通
            "タクシー" to "交通",
            "電車" to "交通",
            "バス" to "交通",
            "ガソリン" to "交通",
            "駐車場" to "交通",
            
            // ショッピング
            "店" to "ショッピング",
            "ショップ" to "ショッピング",
            "モール" to "ショッピング",
            "オンライン" to "ショッピング",
            
            // 通信
            "電話" to "通信",
            "モバイル" to "通信",
            "インターネット" to "通信",
            
            // 教育/文化
            "学校" to "教育",
            "大学" to "教育",
            "図書館" to "教育",
            "映画" to "文化",
            "劇場" to "文化",
            "博物館" to "文化",
            
            // 金融
            "銀行" to "金融",
            "クレジット" to "金融",
            "保険" to "金融"
        )
    }
    
    /**
     * 가맹점명으로부터 카테고리를 AI 추론
     */
    fun inferCategory(merchant: String, language: String = "ko"): String {
        try {
            val normalizedMerchant = merchant.lowercase(Locale.getDefault())
            
            return when (language.lowercase()) {
                "ko", "korean", "kr" -> inferKoreanCategory(normalizedMerchant)
                "en", "english", "us" -> inferEnglishCategory(normalizedMerchant)
                "ja", "japanese", "jp" -> inferJapaneseCategory(normalizedMerchant)
                else -> inferKoreanCategory(normalizedMerchant) // 기본값
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inferring category for merchant: $merchant", e)
            return "기타"
        }
    }
    
    /**
     * 한국어 카테고리 추론
     */
    private fun inferKoreanCategory(merchant: String): String {
        // 정확한 매칭
        KOREAN_CATEGORIES.forEach { (keyword, category) ->
            if (merchant.contains(keyword.lowercase())) {
                return category
            }
        }
        
        // 부분 매칭 (더 정교한 AI 로직)
        return when {
            merchant.contains("병원") || merchant.contains("의료") || merchant.contains("약국") -> "의료"
            merchant.contains("이마트") || merchant.contains("마트") || merchant.contains("편의점") -> "식품"
            merchant.contains("스타벅스") || merchant.contains("카페") || merchant.contains("커피") -> "외식"
            merchant.contains("택시") || merchant.contains("지하철") || merchant.contains("버스") -> "교통"
            merchant.contains("백화점") || merchant.contains("쇼핑") || merchant.contains("온라인") -> "쇼핑"
            merchant.contains("통신") || merchant.contains("인터넷") || merchant.contains("모바일") -> "통신"
            merchant.contains("학교") || merchant.contains("학원") || merchant.contains("교육") -> "교육"
            merchant.contains("영화") || merchant.contains("공연") || merchant.contains("문화") -> "문화"
            merchant.contains("은행") || merchant.contains("카드") || merchant.contains("금융") -> "금융"
            else -> "기타"
        }
    }
    
    /**
     * 영어 카테고리 추론
     */
    private fun inferEnglishCategory(merchant: String): String {
        ENGLISH_CATEGORIES.forEach { (keyword, category) ->
            if (merchant.contains(keyword.lowercase())) {
                return category
            }
        }
        
        return when {
            merchant.contains("hospital") || merchant.contains("medical") || merchant.contains("pharmacy") -> "Medical"
            merchant.contains("restaurant") || merchant.contains("cafe") || merchant.contains("coffee") -> "Dining"
            merchant.contains("taxi") || merchant.contains("uber") || merchant.contains("gas") -> "Transportation"
            merchant.contains("store") || merchant.contains("shop") || merchant.contains("amazon") -> "Shopping"
            merchant.contains("phone") || merchant.contains("mobile") || merchant.contains("internet") -> "Communication"
            merchant.contains("school") || merchant.contains("university") || merchant.contains("education") -> "Education"
            merchant.contains("movie") || merchant.contains("theater") || merchant.contains("entertainment") -> "Culture"
            merchant.contains("bank") || merchant.contains("credit") || merchant.contains("financial") -> "Financial"
            else -> "Other"
        }
    }
    
    /**
     * 일본어 카테고리 추론
     */
    private fun inferJapaneseCategory(merchant: String): String {
        JAPANESE_CATEGORIES.forEach { (keyword, category) ->
            if (merchant.contains(keyword)) {
                return category
            }
        }
        
        return when {
            merchant.contains("病院") || merchant.contains("医療") || merchant.contains("薬局") -> "医療"
            merchant.contains("レストラン") || merchant.contains("カフェ") || merchant.contains("コーヒー") -> "飲食"
            merchant.contains("タクシー") || merchant.contains("電車") || merchant.contains("バス") -> "交通"
            merchant.contains("店") || merchant.contains("ショップ") || merchant.contains("オンライン") -> "ショッピング"
            merchant.contains("電話") || merchant.contains("モバイル") || merchant.contains("インターネット") -> "通信"
            merchant.contains("学校") || merchant.contains("大学") || merchant.contains("教育") -> "教育"
            merchant.contains("映画") || merchant.contains("劇場") || merchant.contains("文化") -> "文化"
            merchant.contains("銀行") || merchant.contains("クレジット") || merchant.contains("金融") -> "金融"
            else -> "その他"
        }
    }
    
    /**
     * 지원되는 언어 목록 반환
     */
    fun getSupportedLanguages(): List<String> {
        return listOf("ko", "en", "ja")
    }
    
    /**
     * 언어별 카테고리 목록 반환
     */
    fun getCategoriesByLanguage(language: String): List<String> {
        return when (language.lowercase()) {
            "ko", "korean", "kr" -> KOREAN_CATEGORIES.values.distinct()
            "en", "english", "us" -> ENGLISH_CATEGORIES.values.distinct()
            "ja", "japanese", "jp" -> JAPANESE_CATEGORIES.values.distinct()
            else -> KOREAN_CATEGORIES.values.distinct()
        }
    }
}
