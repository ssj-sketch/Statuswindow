package com.ssj.statuswindow.repo.database

import com.ssj.statuswindow.database.StatusWindowDatabase
import com.ssj.statuswindow.database.entity.SmsProcessingLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * SMS 처리 로그 Room 데이터베이스 레포지토리
 */
class SmsProcessingLogRepository(private val database: StatusWindowDatabase) {
    
    private val smsProcessingLogDao = database.smsProcessingLogDao()
    
    /**
     * 모든 SMS 처리 로그 조회
     */
    fun getAllSmsProcessingLogs(): Flow<List<SmsProcessingLogEntity>> {
        return smsProcessingLogDao.getAllSmsProcessingLogs()
    }
    
    /**
     * 특정 SMS 처리 로그 조회
     */
    suspend fun getSmsProcessingLogById(id: Long): SmsProcessingLogEntity? {
        return smsProcessingLogDao.getSmsProcessingLogById(id)
    }
    
    /**
     * 처리 상태별 SMS 처리 로그 조회
     */
    fun getSmsProcessingLogsByStatus(status: String): Flow<List<SmsProcessingLogEntity>> {
        return smsProcessingLogDao.getSmsProcessingLogsByStatus(status)
    }
    
    /**
     * 특정 날짜 이후 SMS 처리 로그 조회
     */
    fun getSmsProcessingLogsFromDate(fromDate: LocalDateTime): Flow<List<SmsProcessingLogEntity>> {
        return smsProcessingLogDao.getSmsProcessingLogsFromDate(fromDate)
    }
    
    /**
     * SMS 처리 로그 저장
     */
    suspend fun insertSmsProcessingLog(smsProcessingLog: SmsProcessingLogEntity): Long {
        return smsProcessingLogDao.insertSmsProcessingLog(smsProcessingLog)
    }
    
    /**
     * SMS 처리 로그들 저장
     */
    suspend fun insertSmsProcessingLogs(smsProcessingLogs: List<SmsProcessingLogEntity>) {
        smsProcessingLogDao.insertSmsProcessingLogs(smsProcessingLogs)
    }
    
    /**
     * SMS 처리 로그 업데이트
     */
    suspend fun updateSmsProcessingLog(smsProcessingLog: SmsProcessingLogEntity) {
        smsProcessingLogDao.updateSmsProcessingLog(smsProcessingLog)
    }
    
    /**
     * SMS 처리 로그 삭제
     */
    suspend fun deleteSmsProcessingLog(smsProcessingLog: SmsProcessingLogEntity) {
        smsProcessingLogDao.deleteSmsProcessingLog(smsProcessingLog)
    }
    
    /**
     * ID로 SMS 처리 로그 삭제
     */
    suspend fun deleteSmsProcessingLogById(id: Long) {
        smsProcessingLogDao.deleteSmsProcessingLogById(id)
    }
    
    /**
     * 모든 SMS 처리 로그 삭제
     */
    suspend fun deleteAllSmsProcessingLogs() {
        smsProcessingLogDao.deleteAllSmsProcessingLogs()
    }
    
    /**
     * SMS 처리 로그 개수 조회
     */
    suspend fun getSmsProcessingLogCount(): Int {
        return smsProcessingLogDao.getSmsProcessingLogCount()
    }
    
    /**
     * 처리 상태별 SMS 처리 로그 개수 조회
     */
    suspend fun getSmsProcessingLogCountByStatus(status: String): Int {
        return smsProcessingLogDao.getSmsProcessingLogCountByStatus(status)
    }
}
