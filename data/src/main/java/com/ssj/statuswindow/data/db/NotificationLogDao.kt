package com.ssj.statuswindow.data.db

// Room temporarily disabled due to Windows SQLite access issues
/*
import androidx.room.*
import com.ssj.statuswindow.data.model.AppNotificationLog
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationLogDao {
    
    @Query("SELECT * FROM notification_logs ORDER BY postedAtEpochMillis DESC")
    fun getAllFlow(): Flow<List<AppNotificationLog>>
    
    @Query("SELECT * FROM notification_logs ORDER BY postedAtEpochMillis DESC")
    suspend fun getAll(): List<AppNotificationLog>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AppNotificationLog)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<AppNotificationLog>)
    
    @Delete
    suspend fun delete(log: AppNotificationLog)
    
    @Query("DELETE FROM notification_logs")
    suspend fun deleteAll()
}
*/

// Temporary mock implementation
interface NotificationLogDao {
    fun getAllFlow(): kotlinx.coroutines.flow.Flow<List<com.ssj.statuswindow.data.model.AppNotificationLog>>
    suspend fun getAll(): List<com.ssj.statuswindow.data.model.AppNotificationLog>
    suspend fun insert(log: com.ssj.statuswindow.data.model.AppNotificationLog)
    suspend fun insertAll(logs: List<com.ssj.statuswindow.data.model.AppNotificationLog>)
    suspend fun delete(log: com.ssj.statuswindow.data.model.AppNotificationLog)
    suspend fun deleteAll()
}

class MockNotificationLogDao : NotificationLogDao {
    private val logs = mutableListOf<com.ssj.statuswindow.data.model.AppNotificationLog>()
    
    override fun getAllFlow(): kotlinx.coroutines.flow.Flow<List<com.ssj.statuswindow.data.model.AppNotificationLog>> {
        return kotlinx.coroutines.flow.flowOf(logs.toList())
    }
    
    override suspend fun getAll(): List<com.ssj.statuswindow.data.model.AppNotificationLog> {
        return logs.toList()
    }
    
    override suspend fun insert(log: com.ssj.statuswindow.data.model.AppNotificationLog) {
        logs.add(log)
    }
    
    override suspend fun insertAll(newLogs: List<com.ssj.statuswindow.data.model.AppNotificationLog>) {
        logs.addAll(newLogs)
    }
    
    override suspend fun delete(log: com.ssj.statuswindow.data.model.AppNotificationLog) {
        logs.remove(log)
    }
    
    override suspend fun deleteAll() {
        logs.clear()
    }
}
