package com.ssj.statuswindow.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log

/**
 * 위치 권한 관리 유틸리티
 */
object LocationPermissionManager {
    
    private const val TAG = "LocationPermissionManager"
    
    // 위치 권한 요청 코드
    const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    
    // 필요한 위치 권한들
    private val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    /**
     * 위치 권한이 모두 허용되었는지 확인
     */
    fun hasLocationPermission(context: Context): Boolean {
        return LOCATION_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 위치 권한이 부분적으로 허용되었는지 확인 (COARSE_LOCATION만 허용)
     */
    fun hasPartialLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 위치 권한 요청이 필요한지 확인
     */
    fun shouldRequestLocationPermission(context: Context): Boolean {
        return LOCATION_PERMISSIONS.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(
                context as Activity, 
                permission
            )
        }
    }
    
    /**
     * 위치 권한 요청
     */
    fun requestLocationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                activity,
                LOCATION_PERMISSIONS,
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    /**
     * 위치 권한 요청 결과 처리
     */
    fun handleLocationPermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): PermissionResult {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            val partialGranted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            
            return when {
                allGranted -> PermissionResult.ALL_GRANTED
                partialGranted -> PermissionResult.PARTIAL_GRANTED
                else -> PermissionResult.DENIED
            }
        }
        
        return PermissionResult.UNKNOWN
    }
    
    /**
     * 위치 설정으로 이동
     */
    fun openLocationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening location settings", e)
        }
    }
    
    /**
     * 앱 설정으로 이동 (권한 설정 페이지)
     */
    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app settings", e)
        }
    }
    
    /**
     * 위치 권한 상태 반환
     */
    fun getLocationPermissionStatus(context: Context): LocationPermissionStatus {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        return when {
            fineLocationGranted && coarseLocationGranted -> LocationPermissionStatus.FULL_ACCESS
            coarseLocationGranted -> LocationPermissionStatus.PARTIAL_ACCESS
            else -> LocationPermissionStatus.NO_ACCESS
        }
    }
    
    /**
     * 위치 권한 설명 텍스트 반환
     */
    fun getLocationPermissionDescription(context: Context): String {
        val status = getLocationPermissionStatus(context)
        
        return when (status) {
            LocationPermissionStatus.FULL_ACCESS -> 
                "위치 권한이 허용되어 정확한 국가 감지가 가능합니다."
            LocationPermissionStatus.PARTIAL_ACCESS -> 
                "기본 위치 권한만 허용되어 대략적인 국가 감지가 가능합니다."
            LocationPermissionStatus.NO_ACCESS -> 
                "위치 권한이 필요합니다. 국가를 자동으로 감지하여 적절한 SMS 파싱 엔진을 선택합니다."
        }
    }
}

/**
 * 위치 권한 요청 결과
 */
enum class PermissionResult {
    ALL_GRANTED,      // 모든 권한 허용
    PARTIAL_GRANTED,  // 일부 권한 허용
    DENIED,           // 권한 거부
    UNKNOWN           // 알 수 없음
}

/**
 * 위치 권한 상태
 */
enum class LocationPermissionStatus {
    FULL_ACCESS,     // 정확한 위치 접근 가능
    PARTIAL_ACCESS,  // 대략적인 위치 접근 가능
    NO_ACCESS        // 위치 접근 불가
}
