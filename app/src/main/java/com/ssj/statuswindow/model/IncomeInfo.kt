package com.ssj.statuswindow.model

import java.time.LocalDateTime

/**
 * 수입 정보 데이터 모델
 */
data class IncomeInfo(
    val id: Long = 0,
    val amount: Long,
    val source: String, // 급여, 부업, 투자수익 등
    val bankName: String,
    val transactionDate: LocalDateTime,
    val description: String = "",
    val isRecurring: Boolean = false // 정기 수입 여부
)
