package com.ssj.statuswindow.hud

import java.time.Duration
import java.time.Instant

/**
 * Signals emitted by on-device detectors that feed into the HUD engine.
 */
sealed interface SignalSample {
    val timestamp: Instant
}

data class NotificationBurstSignal(
    override val timestamp: Instant,
    val category: NotificationCategory,
    val count: Int,
    val quietHours: Boolean
) : SignalSample

data class FinancialSignal(
    override val timestamp: Instant,
    val type: FinancialSignalType,
    val amount: Double
) : SignalSample

data class ActivitySignal(
    override val timestamp: Instant,
    val steps: Int,
    val activityDuration: Duration,
    val isRecovery: Boolean
) : SignalSample

data class SleepSignal(
    override val timestamp: Instant,
    val duration: Duration,
    val qualityScore: Double,
    val interruptions: Int
) : SignalSample

data class FocusSessionSignal(
    override val timestamp: Instant,
    val duration: Duration,
    val interruptions: Int,
    val isUserInitiated: Boolean
) : SignalSample

data class ScreenUsageSignal(
    override val timestamp: Instant,
    val duration: Duration,
    val isLateNight: Boolean
) : SignalSample

enum class NotificationCategory {
    FINANCE,
    FITNESS,
    FOCUS,
    SOCIAL,
    ENTERTAINMENT,
    SYSTEM
}

enum class FinancialSignalType { SPEND, SAVE, INVEST }

