package com.ssj.statuswindow.ui

import android.content.Intent
import android.os.Bundle
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.ssj.statuswindow.databinding.ActivitySettingsBinding
import com.ssj.statuswindow.util.SettingsPreferences
import com.ssj.statuswindow.util.NotificationHistoryPermissionManager
import com.ssj.statuswindow.util.PermissionStatus
import com.ssj.statuswindow.util.LocationPermissionManager
import com.ssj.statuswindow.util.LocationPermissionStatus
import com.ssj.statuswindow.service.DeviceCountryDetectionService
import com.ssj.statuswindow.util.SmsParser
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.util.Log

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settingsPreferences: SettingsPreferences
    private lateinit var countryDetectionService: DeviceCountryDetectionService
    
    companion object {
        private const val TAG = "SettingsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsPreferences = SettingsPreferences.getInstance(this)
        countryDetectionService = DeviceCountryDetectionService(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "설정"

        // SMS 파서 초기화
        SmsParser.initialize(this)

        setupSlider()
        setupClickListeners()
        updatePermissionStatus()
        updateLocationPermissionStatus()
        updateDetectedCountry()
        
        // Android 13 미만에서는 알림 히스토리 섹션 숨기기
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            binding.layoutNotificationHistory.visibility = android.view.View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupSlider() {
        val currentMinutes = settingsPreferences.getDuplicateDetectionMinutes()
        binding.sliderDuplicateMinutes.value = currentMinutes.toFloat()
        updateMinutesText(currentMinutes)

        binding.sliderDuplicateMinutes.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val minutes = value.toInt()
                updateMinutesText(minutes)
                settingsPreferences.setDuplicateDetectionMinutes(minutes)
            }
        }
    }

    private fun setupClickListeners() {
        // 알림 접근 권한 설정 버튼
        binding.btnNotificationAccess.setOnClickListener {
            NotificationHistoryPermissionManager.requestNotificationAccessPermission(this)
            Snackbar.make(binding.root, "알림 접근 권한 설정 화면으로 이동합니다", Snackbar.LENGTH_SHORT).show()
        }
        
        // 알림 히스토리 권한 설정 버튼 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            binding.btnNotificationHistory.setOnClickListener {
                NotificationHistoryPermissionManager.requestNotificationHistoryPermission(this)
                Snackbar.make(binding.root, "알림 히스토리 권한 설정 화면으로 이동합니다", Snackbar.LENGTH_SHORT).show()
            }
        }
        
        // 위치 권한 설정 버튼
        binding.btnLocationPermission.setOnClickListener {
            if (LocationPermissionManager.hasLocationPermission(this)) {
                LocationPermissionManager.openLocationSettings(this)
                Snackbar.make(binding.root, "위치 설정 화면으로 이동합니다", Snackbar.LENGTH_SHORT).show()
            } else {
                LocationPermissionManager.requestLocationPermission(this)
            }
        }
        
        // 국가 새로고침 버튼
        binding.btnRefreshCountry.setOnClickListener {
            refreshCountryDetection()
        }
        
        // 수동 국가 선택 버튼
        binding.btnManualCountrySelect.setOnClickListener {
            showCountrySelectionDialog()
        }
        
        // 디버그 로그 버튼
        binding.btnDebugLogs.setOnClickListener {
            startActivity(Intent(this, DebugLogActivity::class.java))
        }
    }
    
    /**
     * 권한 상태 업데이트
     */
    private fun updatePermissionStatus() {
        val permissionStatus = NotificationHistoryPermissionManager.getPermissionStatus(this)
        
        // 알림 접근 권한 상태
        if (permissionStatus.hasNotificationAccess) {
            binding.tvNotificationAccessStatus.text = "✅ 허용됨"
            binding.tvNotificationAccessStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            binding.btnNotificationAccess.text = "재설정"
        } else {
            binding.tvNotificationAccessStatus.text = "❌ 거부됨"
            binding.tvNotificationAccessStatus.setTextColor(getColor(android.R.color.holo_red_dark))
            binding.btnNotificationAccess.text = "설정"
        }
        
        // 알림 히스토리 권한 상태 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (permissionStatus.hasNotificationHistory) {
                binding.tvNotificationHistoryStatus.text = "✅ 허용됨"
                binding.tvNotificationHistoryStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                binding.btnNotificationHistory.text = "재설정"
            } else {
                binding.tvNotificationHistoryStatus.text = "❌ 거부됨"
                binding.tvNotificationHistoryStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                binding.btnNotificationHistory.text = "설정"
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 설정 화면에서 돌아왔을 때 권한 상태 다시 확인
        updatePermissionStatus()
        updateLocationPermissionStatus()
        updateDetectedCountry()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == LocationPermissionManager.LOCATION_PERMISSION_REQUEST_CODE) {
            val result = LocationPermissionManager.handleLocationPermissionResult(
                requestCode, permissions, grantResults
            )
            
            when (result) {
                com.ssj.statuswindow.util.PermissionResult.ALL_GRANTED -> {
                    Snackbar.make(binding.root, "위치 권한이 허용되었습니다", Snackbar.LENGTH_SHORT).show()
                    updateLocationPermissionStatus()
                    refreshCountryDetection()
                }
                com.ssj.statuswindow.util.PermissionResult.PARTIAL_GRANTED -> {
                    Snackbar.make(binding.root, "기본 위치 권한이 허용되었습니다", Snackbar.LENGTH_SHORT).show()
                    updateLocationPermissionStatus()
                    refreshCountryDetection()
                }
                com.ssj.statuswindow.util.PermissionResult.DENIED -> {
                    Snackbar.make(binding.root, "위치 권한이 거부되었습니다. 수동으로 국가를 선택해주세요", Snackbar.LENGTH_LONG).show()
                    updateLocationPermissionStatus()
                }
                else -> {
                    updateLocationPermissionStatus()
                }
            }
        }
    }
    
    /**
     * 위치 권한 상태 업데이트
     */
    private fun updateLocationPermissionStatus() {
        val status = LocationPermissionManager.getLocationPermissionStatus(this)
        
        when (status) {
            LocationPermissionStatus.FULL_ACCESS -> {
                binding.tvLocationPermissionStatus.text = "✅ 정확한 위치 허용됨"
                binding.tvLocationPermissionStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                binding.btnLocationPermission.text = "재설정"
            }
            LocationPermissionStatus.PARTIAL_ACCESS -> {
                binding.tvLocationPermissionStatus.text = "⚠️ 대략적인 위치 허용됨"
                binding.tvLocationPermissionStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
                binding.btnLocationPermission.text = "재설정"
            }
            LocationPermissionStatus.NO_ACCESS -> {
                binding.tvLocationPermissionStatus.text = "❌ 위치 권한 거부됨"
                binding.tvLocationPermissionStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                binding.btnLocationPermission.text = "설정"
            }
        }
    }
    
    /**
     * 감지된 국가 정보 업데이트
     */
    private fun updateDetectedCountry() {
        try {
            val currentCountryCode = SmsParser.getCurrentCountryCode()
            val countryInfo = countryDetectionService.getCountryInfo(currentCountryCode)
            
            if (countryInfo != null) {
                binding.tvDetectedCountry.text = "${countryInfo.countryName} (${countryInfo.countryCode})"
            } else {
                binding.tvDetectedCountry.text = "알 수 없음"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating detected country", e)
            binding.tvDetectedCountry.text = "오류 발생"
        }
    }
    
    /**
     * 국가 감지 새로고침
     */
    private fun refreshCountryDetection() {
        try {
            val detectedCountry = countryDetectionService.detectCountry()
            SmsParser.setCountryCode(detectedCountry)
            updateDetectedCountry()
            
            Snackbar.make(binding.root, "국가 감지를 새로고침했습니다: $detectedCountry", Snackbar.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing country detection", e)
            Snackbar.make(binding.root, "국가 감지 중 오류가 발생했습니다", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 국가 선택 다이얼로그 표시
     */
    private fun showCountrySelectionDialog() {
        val supportedCountries = SmsParser.getSupportedCountries()
        val countryNames = supportedCountries.values.toTypedArray()
        val countryCodes = supportedCountries.keys.toTypedArray()
        
        val currentCountryCode = SmsParser.getCurrentCountryCode()
        val currentIndex = countryCodes.indexOf(currentCountryCode)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("국가 선택")
            .setSingleChoiceItems(countryNames, currentIndex) { dialog, which ->
                val selectedCountryCode = countryCodes[which]
                SmsParser.setCountryCode(selectedCountryCode)
                updateDetectedCountry()
                
                Snackbar.make(binding.root, "국가가 변경되었습니다: ${countryNames[which]}", Snackbar.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun updateMinutesText(minutes: Int) {
        binding.tvDuplicateMinutes.text = "${minutes}분"
    }
}
