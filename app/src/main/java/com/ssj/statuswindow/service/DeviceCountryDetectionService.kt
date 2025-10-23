package com.ssj.statuswindow.service

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
import java.util.Locale
import java.io.IOException

/**
 * 디바이스 정보를 활용한 국가 감지 서비스
 */
class DeviceCountryDetectionService(private val context: Context) {
    
    companion object {
        private const val TAG = "DeviceCountryDetection"
        
        // 국가 코드 매핑
        private val COUNTRY_CODE_MAPPING = mapOf(
            "KR" to "대한민국",
            "US" to "United States", 
            "JP" to "日本",
            "CN" to "中国",
            "GB" to "United Kingdom",
            "DE" to "Deutschland",
            "FR" to "France",
            "CA" to "Canada",
            "AU" to "Australia",
            "SG" to "Singapore",
            "TH" to "Thailand",
            "VN" to "Vietnam",
            "PH" to "Philippines",
            "MY" to "Malaysia",
            "ID" to "Indonesia"
        )
        
        // 언어별 국가 우선순위
        private val LANGUAGE_COUNTRY_PRIORITY = mapOf(
            "ko" to listOf("KR", "US", "JP", "CN"),
            "en" to listOf("US", "GB", "CA", "AU", "SG"),
            "ja" to listOf("JP", "US", "KR"),
            "zh" to listOf("CN", "SG", "TW", "HK"),
            "de" to listOf("DE", "AT", "CH"),
            "fr" to listOf("FR", "CA", "BE", "CH"),
            "es" to listOf("ES", "MX", "AR", "US"),
            "pt" to listOf("BR", "PT"),
            "it" to listOf("IT", "CH"),
            "ru" to listOf("RU", "KZ", "BY"),
            "ar" to listOf("SA", "AE", "EG"),
            "hi" to listOf("IN", "US"),
            "th" to listOf("TH", "US"),
            "vi" to listOf("VN", "US"),
            "ms" to listOf("MY", "SG"),
            "id" to listOf("ID", "MY")
        )
    }
    
    /**
     * 디바이스 정보를 종합하여 최적의 국가 코드를 반환
     */
    fun detectCountry(): String {
        val detectionResults = mutableMapOf<String, Float>()
        
        // 1. 디바이스 로케일 기반 감지
        val localeResult = detectFromLocale()
        if (localeResult.isNotEmpty()) {
            detectionResults[localeResult] = 0.8f
        }
        
        // 2. 위치 정보 기반 감지 (권한이 있는 경우)
        if (hasLocationPermission()) {
            val locationResult = detectFromLocation()
            if (locationResult.isNotEmpty()) {
                detectionResults[locationResult] = 0.9f
            }
        }
        
        // 3. 시스템 설정 기반 감지
        val systemResult = detectFromSystemSettings()
        if (systemResult.isNotEmpty()) {
            detectionResults[systemResult] = 0.7f
        }
        
        // 4. 네트워크 정보 기반 감지
        val networkResult = detectFromNetwork()
        if (networkResult.isNotEmpty()) {
            detectionResults[networkResult] = 0.6f
        }
        
        // 가장 높은 신뢰도를 가진 국가 코드 반환
        val bestCountry = detectionResults.maxByOrNull { it.value }?.key ?: "KR" // 기본값: 한국
        
        Log.d(TAG, "Detected country: $bestCountry with confidence: ${detectionResults[bestCountry]}")
        Log.d(TAG, "All detection results: $detectionResults")
        
        return bestCountry
    }
    
    /**
     * 디바이스 로케일을 기반으로 국가 감지
     */
    private fun detectFromLocale(): String {
        try {
            val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale
            }
            
            val language = locale.language
            val country = locale.country
            
            Log.d(TAG, "Device locale: language=$language, country=$country")
            
            // 국가 코드가 명시적으로 설정된 경우
            if (country.isNotEmpty() && COUNTRY_CODE_MAPPING.containsKey(country)) {
                return country
            }
            
            // 언어 기반으로 우선순위 국가 선택
            val priorityCountries = LANGUAGE_COUNTRY_PRIORITY[language]
            if (priorityCountries != null && priorityCountries.isNotEmpty()) {
                return priorityCountries[0] // 첫 번째 우선순위 국가 반환
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting from locale", e)
        }
        
        return ""
    }
    
    /**
     * 위치 정보를 기반으로 국가 감지
     */
    private fun detectFromLocation(): String {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            
            // GPS 위치 확인
            val gpsLocation = getLastKnownLocation(locationManager, LocationManager.GPS_PROVIDER)
            if (gpsLocation != null) {
                return getCountryFromLocation(gpsLocation)
            }
            
            // 네트워크 위치 확인
            val networkLocation = getLastKnownLocation(locationManager, LocationManager.NETWORK_PROVIDER)
            if (networkLocation != null) {
                return getCountryFromLocation(networkLocation)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting from location", e)
        }
        
        return ""
    }
    
    /**
     * 시스템 설정을 기반으로 국가 감지
     */
    private fun detectFromSystemSettings(): String {
        try {
            // 시스템 국가 설정 확인
            val systemCountry = Settings.System.getString(context.contentResolver, "locale")
            if (systemCountry != null) {
                val parts = systemCountry.split("_")
                if (parts.size >= 2) {
                    val countryCode = parts[1]
                    if (COUNTRY_CODE_MAPPING.containsKey(countryCode)) {
                        return countryCode
                    }
                }
            }
            
            // SIM 카드 국가 코드 확인
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
            val simCountry = telephonyManager.simCountryIso
            if (simCountry.isNotEmpty()) {
                val countryCode = simCountry.uppercase()
                if (COUNTRY_CODE_MAPPING.containsKey(countryCode)) {
                    return countryCode
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting from system settings", e)
        }
        
        return ""
    }
    
    /**
     * 네트워크 정보를 기반으로 국가 감지
     */
    private fun detectFromNetwork(): String {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = connectivityManager.activeNetworkInfo
            
            if (activeNetwork != null && activeNetwork.isConnected) {
                // 네트워크 운영자 정보 확인
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
                val networkCountry = telephonyManager.networkCountryIso
                if (networkCountry.isNotEmpty()) {
                    val countryCode = networkCountry.uppercase()
                    if (COUNTRY_CODE_MAPPING.containsKey(countryCode)) {
                        return countryCode
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting from network", e)
        }
        
        return ""
    }
    
    /**
     * 위치 권한이 있는지 확인
     */
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 마지막으로 알려진 위치 가져오기
     */
    private fun getLastKnownLocation(locationManager: LocationManager, provider: String): Location? {
        return try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                locationManager.getLastKnownLocation(provider)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last known location", e)
            null
        }
    }
    
    /**
     * 위치 좌표를 기반으로 국가 코드 반환
     */
    private fun getCountryFromLocation(location: Location): String {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            
            if (addresses != null && addresses.isNotEmpty()) {
                val countryCode = addresses[0].countryCode
                if (countryCode != null && COUNTRY_CODE_MAPPING.containsKey(countryCode)) {
                    Log.d(TAG, "Detected country from location: $countryCode")
                    return countryCode
                }
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "Error getting country from location", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error getting country from location", e)
        }
        
        return ""
    }
    
    /**
     * 지원되는 국가 목록 반환
     */
    fun getSupportedCountries(): Map<String, String> {
        return COUNTRY_CODE_MAPPING
    }
    
    /**
     * 특정 국가가 지원되는지 확인
     */
    fun isCountrySupported(countryCode: String): Boolean {
        return COUNTRY_CODE_MAPPING.containsKey(countryCode)
    }
    
    /**
     * 현재 감지된 국가의 상세 정보 반환
     */
    fun getCountryInfo(countryCode: String): CountryInfo? {
        val countryName = COUNTRY_CODE_MAPPING[countryCode] ?: return null
        
        return CountryInfo(
            countryCode = countryCode,
            countryName = countryName,
            language = getPrimaryLanguage(countryCode),
            currency = getCurrency(countryCode),
            timeZone = getTimeZone(countryCode)
        )
    }
    
    /**
     * 국가별 주요 언어 반환
     */
    private fun getPrimaryLanguage(countryCode: String): String {
        return when (countryCode) {
            "KR" -> "ko"
            "US", "GB", "CA", "AU" -> "en"
            "JP" -> "ja"
            "CN" -> "zh"
            "DE" -> "de"
            "FR" -> "fr"
            "ES" -> "es"
            "IT" -> "it"
            "RU" -> "ru"
            "AR" -> "ar"
            "IN" -> "hi"
            "TH" -> "th"
            "VN" -> "vi"
            "MY" -> "ms"
            "ID" -> "id"
            else -> "en"
        }
    }
    
    /**
     * 국가별 통화 반환
     */
    private fun getCurrency(countryCode: String): String {
        return when (countryCode) {
            "KR" -> "KRW"
            "US" -> "USD"
            "JP" -> "JPY"
            "CN" -> "CNY"
            "GB" -> "GBP"
            "DE", "FR", "IT", "ES" -> "EUR"
            "CA" -> "CAD"
            "AU" -> "AUD"
            "SG" -> "SGD"
            "TH" -> "THB"
            "VN" -> "VND"
            "PH" -> "PHP"
            "MY" -> "MYR"
            "ID" -> "IDR"
            else -> "USD"
        }
    }
    
    /**
     * 국가별 시간대 반환
     */
    private fun getTimeZone(countryCode: String): String {
        return when (countryCode) {
            "KR" -> "Asia/Seoul"
            "US" -> "America/New_York"
            "JP" -> "Asia/Tokyo"
            "CN" -> "Asia/Shanghai"
            "GB" -> "Europe/London"
            "DE" -> "Europe/Berlin"
            "FR" -> "Europe/Paris"
            "CA" -> "America/Toronto"
            "AU" -> "Australia/Sydney"
            "SG" -> "Asia/Singapore"
            "TH" -> "Asia/Bangkok"
            "VN" -> "Asia/Ho_Chi_Minh"
            "PH" -> "Asia/Manila"
            "MY" -> "Asia/Kuala_Lumpur"
            "ID" -> "Asia/Jakarta"
            else -> "UTC"
        }
    }
}

/**
 * 국가 정보 데이터 클래스
 */
data class CountryInfo(
    val countryCode: String,
    val countryName: String,
    val language: String,
    val currency: String,
    val timeZone: String
)
