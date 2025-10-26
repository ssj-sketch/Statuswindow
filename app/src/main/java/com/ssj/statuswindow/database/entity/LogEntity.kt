package com.ssj.statuswindow.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * 로그 엔티티
 */
@Entity(tableName = "app_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val level: String,           // 로그 레벨 (DEBUG, INFO, WARN, ERROR)
    val tag: String,             // 로그 태그
    val message: String,         // 로그 메시지
    val timestamp: LocalDateTime = LocalDateTime.now(), // 로그 생성 시간
    val stackTrace: String? = null, // 스택 트레이스 (에러 로그의 경우)
    val extra: String? = null    // 추가 정보 (JSON 형태)
)

