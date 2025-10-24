package com.ssj.statuswindow.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.ssj.statuswindow.database.converter.DateTimeConverter
import com.ssj.statuswindow.database.dao.*
import com.ssj.statuswindow.database.entity.*

/**
 * StatusWindow 앱의 Room 데이터베이스
 * - 카드 거래 내역
 * - 수입 내역  
 * - 은행 잔고 (자산내역)
 */
@Database(
        entities = [
            CardTransactionEntity::class,
            CreditCardUsageEntity::class,
            BankTransactionEntity::class,
            IncomeTransactionEntity::class,
            BankBalanceEntity::class,
            SmsProcessingLogEntity::class
        ],
    version = 6,
    exportSchema = false,
    autoMigrations = []
)
@TypeConverters(DateTimeConverter::class)
abstract class StatusWindowDatabase : RoomDatabase() {
    
    abstract fun cardTransactionDao(): CardTransactionDao
    abstract fun creditCardUsageDao(): CreditCardUsageDao
    abstract fun bankTransactionDao(): BankTransactionDao
    abstract fun incomeTransactionDao(): IncomeTransactionDao
    abstract fun bankBalanceDao(): BankBalanceDao
    abstract fun smsProcessingLogDao(): SmsProcessingLogDao
    
    companion object {
        @Volatile
        private var INSTANCE: StatusWindowDatabase? = null
        
        fun getDatabase(context: Context): StatusWindowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StatusWindowDatabase::class.java,
                    "status_window_database"
                )
                        .allowMainThreadQueries() // 테스트용으로 메인 스레드에서 쿼리 허용
                        .fallbackToDestructiveMigration() // 스키마 변경 시 데이터 삭제 후 재생성
                        .fallbackToDestructiveMigrationOnDowngrade() // 다운그레이드 시에도 데이터 삭제
                        .fallbackToDestructiveMigrationFrom(4) // 버전 4에서 마이그레이션 시 데이터 삭제
                       .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
