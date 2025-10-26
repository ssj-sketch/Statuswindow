package com.ssj.statuswindow.util

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Environment
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * 엑셀 내보내기 공통 관리자
 * 모든 액티비티에서 동일한 방식으로 엑셀 내보내기를 사용할 수 있도록 모듈화
 */
class ExcelExportManager(private val activity: Activity) {
    
    companion object {
        private const val STORAGE_PERMISSION_REQUEST_CODE = 1001
    }
    
    /**
     * 엑셀 내보내기 시작
     * @param fileName 엑셀 파일명 (확장자 제외)
     * @param headers 테이블 헤더 배열
     * @param dataRows 데이터 행들의 배열 (각 행은 문자열 배열)
     * @param onSuccess 성공 시 콜백
     * @param onError 실패 시 콜백
     */
    fun exportToExcel(
        fileName: String,
        headers: Array<String>,
        dataRows: List<Array<String>>,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        android.util.Log.d("ExcelExportManager", "엑셀 내보내기 시작: $fileName")
        
        // 데이터 검증
        if (dataRows.isEmpty()) {
            android.util.Log.w("ExcelExportManager", "엑셀 내보낼 데이터가 없습니다")
            Toast.makeText(activity, "내보낼 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
            onError("데이터가 없습니다")
            return
        }
        
        try {
            // Android 11+ (API 30+)에서는 저장소 권한이 다르게 처리됨
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                android.util.Log.d("ExcelExportManager", "Android 11+ 감지, 앱별 저장소 사용")
                // Android 11+에서는 MANAGE_EXTERNAL_STORAGE 권한 또는 앱별 저장소 사용
                performExcelExport(fileName, headers, dataRows, onSuccess, onError)
            } else {
                android.util.Log.d("ExcelExportManager", "Android 10 이하, 저장소 권한 확인")
                // Android 10 이하에서는 기존 저장소 권한 사용
                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                    android.util.Log.d("ExcelExportManager", "저장소 권한 필요, 다이얼로그 표시")
                    showStoragePermissionDialog(fileName, headers, dataRows, onSuccess, onError)
                    return
                }
                android.util.Log.d("ExcelExportManager", "저장소 권한 있음, 엑셀 내보내기 진행")
                performExcelExport(fileName, headers, dataRows, onSuccess, onError)
            }
        } catch (e: Exception) {
            android.util.Log.e("ExcelExportManager", "엑셀 다운로드 초기화 오류: ${e.message}", e)
            Toast.makeText(activity, "엑셀 다운로드 초기화 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            onError("초기화 오류: ${e.message}")
        }
    }
    
    /**
     * 저장소 권한 요청 다이얼로그 표시
     */
    private fun showStoragePermissionDialog(
        fileName: String,
        headers: Array<String>,
        dataRows: List<Array<String>>,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setTitle("저장소 권한 필요")
            .setMessage("엑셀 파일을 다운로드 폴더에 저장하기 위해 저장소 접근 권한이 필요합니다.\n\n" +
                       "• 파일 저장 위치: /Download/\n" +
                       "• 파일 형식: .xlsx (Excel)\n" +
                       "• 파일명: ${fileName}_날짜시간.xlsx")
            .setPositiveButton("권한 허용") { _, _ ->
                ActivityCompat.requestPermissions(activity, 
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_REQUEST_CODE)
                
                // 권한 요청 후 콜백 저장 (실제 권한 결과는 onRequestPermissionsResult에서 처리)
                pendingExport = PendingExport(fileName, headers, dataRows, onSuccess, onError)
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(activity, "엑셀 내보내기가 취소되었습니다.", Toast.LENGTH_SHORT).show()
                onError("사용자 취소")
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * 권한 요청 결과 처리
     */
    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        when (requestCode) {
            STORAGE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    android.util.Log.d("ExcelExportManager", "저장소 권한 허용됨, 엑셀 내보내기 진행")
                    pendingExport?.let { pending ->
                        performExcelExport(pending.fileName, pending.headers, pending.dataRows, pending.onSuccess, pending.onError)
                        pendingExport = null
                    }
                } else {
                    android.util.Log.w("ExcelExportManager", "저장소 권한 거부됨")
                    showPermissionDeniedDialog()
                    pendingExport?.let { pending ->
                        pending.onError("권한 거부됨")
                        pendingExport = null
                    }
                }
            }
        }
    }
    
    /**
     * 권한 거부 시 다이얼로그 표시
     */
    private fun showPermissionDeniedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setTitle("저장소 권한 거부됨")
            .setMessage("엑셀 파일을 저장하려면 저장소 권한이 필요합니다.\n\n" +
                       "설정에서 권한을 허용하시겠습니까?")
            .setPositiveButton("설정으로 이동") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    /**
     * 앱 설정 화면 열기
     */
    private fun openAppSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }
    
    /**
     * 실제 엑셀 파일 생성 및 저장
     */
    private fun performExcelExport(
        fileName: String,
        headers: Array<String>,
        dataRows: List<Array<String>>,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        android.util.Log.d("ExcelExportManager", "performExcelExport 시작 - 데이터 개수: ${dataRows.size}")
        
        (activity as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
            var workbook: XSSFWorkbook? = null
            try {
                android.util.Log.d("ExcelExportManager", "Apache POI 워크북 생성 시작")
                workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("데이터")
                
                // 헤더 스타일 생성
                val headerStyle = workbook.createCellStyle()
                val headerFont = workbook.createFont()
                headerFont.bold = true
                headerFont.fontHeightInPoints = 12
                headerStyle.setFont(headerFont)
                headerStyle.fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.BLUE.index
                headerStyle.fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
                headerStyle.borderTop = BorderStyle.THIN
                headerStyle.borderBottom = BorderStyle.THIN
                headerStyle.borderLeft = BorderStyle.THIN
                headerStyle.borderRight = BorderStyle.THIN
                headerStyle.alignment = HorizontalAlignment.CENTER
                headerStyle.verticalAlignment = VerticalAlignment.CENTER
                
                // 데이터 스타일 생성
                val dataStyle = workbook.createCellStyle()
                dataStyle.borderTop = BorderStyle.THIN
                dataStyle.borderBottom = BorderStyle.THIN
                dataStyle.borderLeft = BorderStyle.THIN
                dataStyle.borderRight = BorderStyle.THIN
                dataStyle.alignment = HorizontalAlignment.CENTER
                dataStyle.verticalAlignment = VerticalAlignment.CENTER
                
                // 헤더 행 생성
                val headerRow = sheet.createRow(0)
                headers.forEachIndexed { index, header ->
                    val cell = headerRow.createCell(index)
                    cell.setCellValue(header)
                    cell.cellStyle = headerStyle
                }
                
                // 데이터 행 생성
                dataRows.forEachIndexed { rowIndex, rowData ->
                    val row = sheet.createRow(rowIndex + 1)
                    rowData.forEachIndexed { colIndex, data ->
                        val cell = row.createCell(colIndex)
                        cell.setCellValue(data)
                        cell.cellStyle = dataStyle
                    }
                }
                
                android.util.Log.d("ExcelExportManager", "컬럼 너비 설정")
                // 컬럼 너비 설정 (고정 너비 사용)
                headers.forEachIndexed { index, _ ->
                    try {
                        val columnWidth = when (index) {
                            0 -> 4000  // 첫 번째 컬럼
                            1 -> 2000  // 두 번째 컬럼
                            2 -> 3000  // 세 번째 컬럼
                            3 -> 3000  // 네 번째 컬럼
                            4 -> 4000  // 다섯 번째 컬럼
                            5 -> 3000  // 여섯 번째 컬럼
                            else -> 3000
                        }
                        sheet.setColumnWidth(index, columnWidth)
                        android.util.Log.d("ExcelExportManager", "컬럼 $index 너비 설정: $columnWidth")
                    } catch (e: Exception) {
                        android.util.Log.w("ExcelExportManager", "컬럼 $index 너비 설정 실패: ${e.message}")
                        sheet.setColumnWidth(index, 3000)
                    }
                }
                
                android.util.Log.d("ExcelExportManager", "파일 저장 시작")
                // 파일 저장
                val fullFileName = "${fileName}_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.xlsx"
                
                val file = try {
                    // 모든 Android 버전에서 공용 다운로드 폴더 사용
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    android.util.Log.d("ExcelExportManager", "공용 다운로드 폴더 사용: ${downloadsDir.absolutePath}")
                    
                    if (!downloadsDir.exists()) {
                        android.util.Log.d("ExcelExportManager", "다운로드 폴더 생성 시도")
                        downloadsDir.mkdirs()
                    }
                    
                    File(downloadsDir, fullFileName)
                } catch (e: Exception) {
                    android.util.Log.e("ExcelExportManager", "공용 다운로드 폴더 접근 실패: ${e.message}", e)
                    try {
                        // 폴백 1: 앱별 다운로드 폴더 사용
                        val appDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        android.util.Log.d("ExcelExportManager", "앱별 다운로드 폴더 사용: ${appDir?.absolutePath}")
                        File(appDir, fullFileName)
                    } catch (e2: Exception) {
                        android.util.Log.e("ExcelExportManager", "앱별 다운로드 폴더도 실패: ${e2.message}", e2)
                        // 폴백 2: 앱 내부 저장소 사용
                        android.util.Log.d("ExcelExportManager", "폴백: 앱 내부 저장소 사용")
                        File(activity.filesDir, fullFileName)
                    }
                }
                
                android.util.Log.d("ExcelExportManager", "파일 저장 경로: ${file.absolutePath}")
                
                // 파일 저장
                try {
                    FileOutputStream(file).use { outputStream ->
                        workbook?.write(outputStream)
                        android.util.Log.d("ExcelExportManager", "파일 쓰기 완료")
                    }
                    
                    workbook?.close()
                    workbook = null
                    android.util.Log.d("ExcelExportManager", "워크북 정리 완료")
                } catch (e: Exception) {
                    android.util.Log.e("ExcelExportManager", "파일 저장 실패: ${e.message}", e)
                    throw e
                }
                
                android.util.Log.d("ExcelExportManager", "엑셀 파일 생성 완료: ${file.absolutePath}")
                
                withContext(Dispatchers.Main) {
                    val message = if (file.absolutePath.contains("Download")) {
                        "✅ 엑셀 파일이 다운로드 폴더에 저장되었습니다!\n\n" +
                        "📁 위치: 다운로드 폴더\n" +
                        "📄 파일명: $fullFileName\n\n" +
                        "경로: ${file.absolutePath}"
                    } else if (file.absolutePath.contains("files")) {
                        "✅ 엑셀 파일이 앱별 저장소에 저장되었습니다!\n\n" +
                        "📁 위치: 앱 전용 폴더\n" +
                        "📄 파일명: $fullFileName\n\n" +
                        "파일 관리자에서 확인하세요."
                    } else {
                        "✅ 엑셀 파일이 저장되었습니다!\n\n" +
                        "📄 파일명: $fullFileName\n\n" +
                        "경로: ${file.absolutePath}"
                    }
                    Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                    onSuccess(file.absolutePath)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("ExcelExportManager", "엑셀 파일 생성 오류: ${e.message}", e)
                e.printStackTrace()
                
                // 워크북 정리
                try {
                    workbook?.close()
                } catch (closeException: Exception) {
                    android.util.Log.w("ExcelExportManager", "워크북 닫기 실패: ${closeException.message}")
                }
                
                withContext(Dispatchers.Main) {
                    val errorMessage = when {
                        e.message?.contains("autoSizeColumn") == true -> "엑셀 컬럼 크기 조정 오류"
                        e.message?.contains("Permission") == true -> "파일 저장 권한 오류"
                        e.message?.contains("File") == true -> "파일 저장 오류"
                        else -> "엑셀 파일 생성 중 오류가 발생했습니다"
                    }
                    
                    Toast.makeText(
                        activity,
                        "$errorMessage\n\n오류: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    onError("${e.message}")
                }
            }
        }
    }
    
    /**
     * 권한 요청 대기 중인 내보내기 정보
     */
    private data class PendingExport(
        val fileName: String,
        val headers: Array<String>,
        val dataRows: List<Array<String>>,
        val onSuccess: (String) -> Unit,
        val onError: (String) -> Unit
    )
    
    private var pendingExport: PendingExport? = null
}

