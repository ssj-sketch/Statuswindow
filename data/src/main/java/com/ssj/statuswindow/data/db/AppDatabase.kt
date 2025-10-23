package com.ssj.statuswindow.data.db

import android.content.Context
// Room temporarily disabled due to Windows SQLite access issues
/*
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ssj.statuswindow.data.model.AppNotificationLog

@Database(
    entities = [AppNotificationLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun notificationLogDao(): NotificationLogDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "statuswindow_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
*/

// Temporary mock implementation
class AppDatabase {
    companion object {
        fun getDatabase(context: Context): AppDatabase {
            return AppDatabase()
        }
    }
    
    fun notificationLogDao(): NotificationLogDao {
        return MockNotificationLogDao()
    }
}