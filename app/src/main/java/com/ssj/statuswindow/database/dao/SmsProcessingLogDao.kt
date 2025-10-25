package com.ssj.statuswindow.database.dao

import androidx.room.*
import com.ssj.statuswindow.database.entity.SmsProcessingLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * SMS 처리 로그 DAO
 */
@Dao
interface SmsProcessingLogDao {
    
    @Query("SELECT * FROM sms_processing_logs ORDER BY createdAt DESC")
    fun getAllSmsProcessingLogs(): Flow<List<SmsProcessingLogEntity>>
    
    @Query("SELECT * FROM sms_processing_logs WHERE id = :id")
    suspend fun getSmsProcessingLogById(id: Long): SmsProcessingLogEntity?
    
    @Query("SELECT * FROM sms_processing_logs WHERE processingStatus = :status ORDER BY createdAt DESC")
    fun getSmsProcessingLogsByStatus(status: String): Flow<List<SmsProcessingLogEntity>>
    
    @Query("SELECT * FROM sms_processing_logs WHERE createdAt >= :fromDate ORDER BY createdAt DESC")
    fun getSmsProcessingLogsFromDate(fromDate: LocalDateTime): Flow<List<SmsProcessingLogEntity>>
    
    @Insert
    suspend fun insertSmsProcessingLog(smsProcessingLog: SmsProcessingLogEntity): Long
    
    @Insert
    suspend fun insertSmsProcessingLogs(smsProcessingLogs: List<SmsProcessingLogEntity>)
    
    @Update
    suspend fun updateSmsProcessingLog(smsProcessingLog: SmsProcessingLogEntity)
    
    @Delete
    suspend fun deleteSmsProcessingLog(smsProcessingLog: SmsProcessingLogEntity)
    
    @Query("DELETE FROM sms_processing_logs WHERE id = :id")
    suspend fun deleteSmsProcessingLogById(id: Long)
    
    @Query("DELETE FROM sms_processing_logs")
    suspend fun deleteAllSmsProcessingLogs()
    
    @Query("SELECT COUNT(*) FROM sms_processing_logs")
    suspend fun getSmsProcessingLogCount(): Int
    
    @Query("SELECT COUNT(*) FROM sms_processing_logs WHERE processingStatus = :status")
    suspend fun getSmsProcessingLogCountByStatus(status: String): Int
    
    // 중복 체크 메서드 추가
    @Query("SELECT * FROM sms_processing_logs WHERE inputSms = :inputSms LIMIT 1")
    suspend fun getSmsProcessingLogByInputSms(inputSms: String): SmsProcessingLogEntity?
}
