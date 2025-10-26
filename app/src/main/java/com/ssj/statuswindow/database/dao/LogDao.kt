package com.ssj.statuswindow.database.dao

import androidx.room.*
import com.ssj.statuswindow.database.entity.LogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * 로그 DAO
 */
@Dao
interface LogDao {
    
    @Query("SELECT * FROM app_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntity>>
    
    @Query("SELECT * FROM app_logs WHERE level = :level ORDER BY timestamp DESC")
    fun getLogsByLevel(level: String): Flow<List<LogEntity>>
    
    @Query("SELECT * FROM app_logs WHERE tag = :tag ORDER BY timestamp DESC")
    fun getLogsByTag(tag: String): Flow<List<LogEntity>>
    
    @Query("SELECT * FROM app_logs WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getLogsByTimeRange(startTime: LocalDateTime, endTime: LocalDateTime): Flow<List<LogEntity>>
    
    @Query("SELECT * FROM app_logs WHERE level = 'ERROR' ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentErrors(limit: Int = 100): Flow<List<LogEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<LogEntity>)
    
    @Delete
    suspend fun deleteLog(log: LogEntity)
    
    @Query("DELETE FROM app_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)
    
    @Query("DELETE FROM app_logs WHERE timestamp < :cutoffTime")
    suspend fun deleteOldLogs(cutoffTime: LocalDateTime)
    
    @Query("DELETE FROM app_logs")
    suspend fun deleteAllLogs()
    
    @Query("SELECT COUNT(*) FROM app_logs")
    suspend fun getLogCount(): Int
    
    @Query("SELECT COUNT(*) FROM app_logs WHERE level = :level")
    suspend fun getLogCountByLevel(level: String): Int
    
    @Query("SELECT DISTINCT tag FROM app_logs ORDER BY tag")
    suspend fun getAllTags(): List<String>
    
    @Query("SELECT DISTINCT level FROM app_logs ORDER BY level")
    suspend fun getAllLevels(): List<String>
}

