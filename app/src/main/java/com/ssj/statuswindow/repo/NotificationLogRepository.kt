package com.ssj.statuswindow.repo

import android.content.Context
import com.ssj.statuswindow.data.db.AppDatabase
import com.ssj.statuswindow.data.model.AppNotificationLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking

/**
 * 알림 로그 데이터베이스에 접근하기 위한 리포지토리 클래스입니다.
 * 싱글턴 패턴을 사용하여 앱 전체에서 단일 인스턴스를 유지합니다.
 */
class NotificationLogRepository private constructor(context: Context) {

    // Room 데이터베이스의 DAO(Data Access Object) 인스턴스를 가져옵니다.
    // AppDatabase.getDatabase()는 데이터베이스 인스턴스를 생성하거나 가져오는 싱글턴 메서드여야 합니다.
    private val logDao = AppDatabase.getDatabase(context).notificationLogDao()

    /**
     * 모든 알림 로그를 Flow로 반환합니다.
     */
    val logs: Flow<List<AppNotificationLog>> = logDao.getAllFlow()

    /**
     * 새로운 알림 로그 하나를 데이터베이스에 추가합니다.
     * 이 함수는 코루틴 내에서 호출되어야 합니다 (suspend 함수).
     */
    suspend fun add(log: AppNotificationLog) {
        logDao.insert(log)
    }

    /**
     * 데이터베이스에 저장된 모든 알림 로그의 현재 스냅샷을 동기적으로 반환합니다.
     * 이 함수가 바로 NotificationSheetsExporter.kt에서 호출하는 함수입니다.
     *
     * runBlocking을 사용하여 suspend 함수인 logDao.getAll()을 동기적으로 실행합니다.
     * NotificationSheetsExporter에서는 이미 IO Dispatcher를 사용하고 있으므로,
     * 이 코드가 메인 스레드를 차단하지 않습니다.
     */
    fun snapshot(): List<AppNotificationLog> {
        return runBlocking(Dispatchers.IO) {
            logDao.getAll()
        }
    }

    // 여기에 다른 리포지토리 관련 함수가 있다면 추가할 수 있습니다.
    // 예: 특정 로그를 삭제하거나 조회하는 함수 등

    companion object {
        // @Volatile 어노테이션은 INSTANCE 변수가 메인 메모리에만 저장되도록 보장하여
        // 여러 스레드에서 접근할 때 발생할 수 있는 문제를 방지합니다.
        @Volatile
        private var INSTANCE: NotificationLogRepository? = null

        /**
         * NotificationLogRepository의 싱글턴 인스턴스를 가져옵니다.
         * 인스턴스가 존재하지 않으면 스레드에 안전한 방식으로 새로 생성합니다.
         */
        fun instance(context: Context): NotificationLogRepository {
            // 엘비스 연산자(?:)를 사용하여 인스턴스가 null일 경우에만 synchronized 블록을 실행합니다.
            return INSTANCE ?: synchronized(this) {
                // synchronized 블록 내에서 다시 한번 null 체크를 하여
                // 여러 스레드가 동시에 접근했을 때 인스턴스가 중복 생성되는 것을 방지합니다.
                val instance = INSTANCE ?: NotificationLogRepository(context.applicationContext).also { INSTANCE = it }
                instance
            }
        }
    }
}
