package com.ssj.statuswindow.database.dao

import androidx.room.*
import com.ssj.statuswindow.database.entity.NotificationProcessingResultEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * 알림 처리 결과 DAO
 */
@Dao
interface NotificationProcessingResultDao {
    
    @Query("SELECT * FROM notification_processing_results ORDER BY processedAt DESC LIMIT :limit")
    fun getRecentResults(limit: Int = 100): Flow<List<NotificationProcessingResultEntity>>
    
    @Query("SELECT * FROM notification_processing_results WHERE confidenceScore < :threshold AND processingStatus != 'DUPLICATE' ORDER BY processedAt DESC")
    fun getLowConfidenceResults(threshold: Float = 0.7f): Flow<List<NotificationProcessingResultEntity>>
    
    @Query("SELECT * FROM notification_processing_results WHERE processingStatus = :status ORDER BY processedAt DESC")
    fun getResultsByStatus(status: String): Flow<List<NotificationProcessingResultEntity>>
    
    @Query("SELECT * FROM notification_processing_results WHERE packageName = :packageName ORDER BY processedAt DESC")
    fun getResultsByPackage(packageName: String): Flow<List<NotificationProcessingResultEntity>>
    
    @Query("SELECT COUNT(*) FROM notification_processing_results")
    suspend fun getTotalCount(): Int
    
    @Query("SELECT COUNT(*) FROM notification_processing_results WHERE processingStatus = :status")
    suspend fun getCountByStatus(status: String): Int
    
    @Query("SELECT AVG(confidenceScore) FROM notification_processing_results")
    suspend fun getAverageConfidenceScore(): Float?
    
    @Query("SELECT * FROM notification_processing_results WHERE processedAt BETWEEN :startTime AND :endTime ORDER BY processedAt DESC")
    fun getResultsByTimeRange(startTime: LocalDateTime, endTime: LocalDateTime): Flow<List<NotificationProcessingResultEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: NotificationProcessingResultEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<NotificationProcessingResultEntity>)
    
    @Update
    suspend fun update(result: NotificationProcessingResultEntity)
    
    @Delete
    suspend fun delete(result: NotificationProcessingResultEntity)
    
    @Query("DELETE FROM notification_processing_results WHERE processedAt < :cutoffTime")
    suspend fun deleteOldResults(cutoffTime: LocalDateTime)
    
    @Query("DELETE FROM notification_processing_results")
    suspend fun deleteAll()
}
