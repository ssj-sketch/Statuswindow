package com.ssj.statuswindow.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 국가별 날짜 형식 처리 유틸리티
 */
object CountryDateFormatter {
    
    /**
     * 국가별 날짜 형식 매핑
     */
    private val COUNTRY_DATE_FORMATS = mapOf(
        // 한국: MM/dd HH:mm (월/일 시:분)
        "KR" to CountryDateConfig(
            smsPattern = "MM/dd HH:mm",
            fullPattern = "yyyy/MM/dd HH:mm",
            description = "한국 형식: 월/일 시:분"
        ),
        
        // 미국: MM/dd HH:mm (월/일 시:분) - 한국과 동일하지만 해석이 다름
        "US" to CountryDateConfig(
            smsPattern = "MM/dd HH:mm", 
            fullPattern = "yyyy/MM/dd HH:mm",
            description = "미국 형식: 월/일 시:분"
        ),
        
        // 일본: MM/dd HH:mm (월/일 시:분)
        "JP" to CountryDateConfig(
            smsPattern = "MM/dd HH:mm",
            fullPattern = "yyyy/MM/dd HH:mm", 
            description = "일본 형식: 월/일 시:분"
        ),
        
        // 중국: MM/dd HH:mm (월/일 시:분)
        "CN" to CountryDateConfig(
            smsPattern = "MM/dd HH:mm",
            fullPattern = "yyyy/MM/dd HH:mm",
            description = "중국 형식: 월/일 시:분"
        ),
        
        // 영국: dd/MM HH:mm (일/월 시:분)
        "GB" to CountryDateConfig(
            smsPattern = "dd/MM HH:mm",
            fullPattern = "yyyy/dd/MM HH:mm",
            description = "영국 형식: 일/월 시:분"
        ),
        
        // 독일: dd.MM HH:mm (일.월 시:분)
        "DE" to CountryDateConfig(
            smsPattern = "dd.MM HH:mm",
            fullPattern = "yyyy.dd.MM HH:mm",
            description = "독일 형식: 일.월 시:분"
        ),
        
        // 프랑스: dd/MM HH:mm (일/월 시:분)
        "FR" to CountryDateConfig(
            smsPattern = "dd/MM HH:mm",
            fullPattern = "yyyy/dd/MM HH:mm",
            description = "프랑스 형식: 일/월 시:분"
        ),
        
        // 캐나다: MM/dd HH:mm (월/일 시:분)
        "CA" to CountryDateConfig(
            smsPattern = "MM/dd HH:mm",
            fullPattern = "yyyy/MM/dd HH:mm",
            description = "캐나다 형식: 월/일 시:분"
        ),
        
        // 호주: dd/MM HH:mm (일/월 시:분)
        "AU" to CountryDateConfig(
            smsPattern = "dd/MM HH:mm",
            fullPattern = "yyyy/dd/MM HH:mm",
            description = "호주 형식: 일/월 시:분"
        ),
        
        // 싱가포르: dd/MM HH:mm (일/월 시:분)
        "SG" to CountryDateConfig(
            smsPattern = "dd/MM HH:mm",
            fullPattern = "yyyy/dd/MM HH:mm",
            description = "싱가포르 형식: 일/월 시:분"
        )
    )
    
    /**
     * 국가 코드에 따른 날짜 파싱
     */
    fun parseDateByCountry(countryCode: String, dateStr: String, timeStr: String): LocalDateTime {
        val config = COUNTRY_DATE_FORMATS[countryCode] ?: COUNTRY_DATE_FORMATS["KR"]!!
        
        return try {
            val currentYear = LocalDateTime.now().year
            val dateTimeStr = when (countryCode) {
                "KR", "US", "JP", "CN", "CA" -> {
                    // MM/dd 형식: 월/일
                    "$currentYear/$dateStr $timeStr"
                }
                "GB", "FR", "AU", "SG" -> {
                    // dd/MM 형식: 일/월 -> MM/dd로 변환
                    val parts = dateStr.split("/")
                    if (parts.size == 2) {
                        "$currentYear/${parts[1]}/${parts[0]} $timeStr"
                    } else {
                        "$currentYear/$dateStr $timeStr"
                    }
                }
                "DE" -> {
                    // dd.MM 형식: 일.월 -> MM/dd로 변환
                    val parts = dateStr.split(".")
                    if (parts.size == 2) {
                        "$currentYear/${parts[1]}/${parts[0]} $timeStr"
                    } else {
                        "$currentYear/$dateStr $timeStr"
                    }
                }
                else -> {
                    // 기본값: MM/dd 형식
                    "$currentYear/$dateStr $timeStr"
                }
            }
            
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
            LocalDateTime.parse(dateTimeStr, formatter)
            
        } catch (e: DateTimeParseException) {
            android.util.Log.w("CountryDateFormatter", "날짜 파싱 실패: $dateStr $timeStr (국가: $countryCode)", e)
            LocalDateTime.now()
        }
    }
    
    /**
     * 국가별 날짜 형식 정보 가져오기
     */
    fun getCountryDateConfig(countryCode: String): CountryDateConfig {
        return COUNTRY_DATE_FORMATS[countryCode] ?: COUNTRY_DATE_FORMATS["KR"]!!
    }
    
    /**
     * 지원되는 국가 코드 목록
     */
    fun getSupportedCountries(): List<String> {
        return COUNTRY_DATE_FORMATS.keys.toList()
    }
}

/**
 * 국가별 날짜 형식 설정
 */
data class CountryDateConfig(
    val smsPattern: String,      // SMS에서 사용되는 패턴
    val fullPattern: String,     // 전체 날짜 형식 패턴
    val description: String      // 형식 설명
)
