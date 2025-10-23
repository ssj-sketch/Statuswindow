package com.ssj.statuswindow.hud

import java.time.Instant
import kotlin.math.abs
import kotlin.math.max

/**
 * Applies smoothing and heuristics to translate raw signals into HUD stat updates.
 */
class HudEngine(
    private val ewmaAlpha: Double = 0.15
) {
    private val defaultStat: Map<HudStatKind, HudStat> = HudStatKind.entries.associateWith { kind ->
        HudStat(
            kind = kind,
            baseScore = if (kind == HudStatKind.BALANCE) 60.0 else 55.0,
            ewmaAlpha = ewmaAlpha,
            modifiers = emptyList()
        )
    }

    fun computeSnapshot(
        previous: HudSnapshot?,
        signals: List<SignalSample>,
        now: Instant,
        activeQuests: List<Quest> = previous?.activeQuests ?: emptyList(),
        suggestions: List<CoachingSuggestion> = emptyList()
    ): HudSnapshot {
        val stats = (previous?.stats ?: defaultStat)
            .mapValues { (kind, stat) ->
                if (kind == HudStatKind.BALANCE) stat else stat.copy(ewmaAlpha = ewmaAlpha)
            }
            .toMutableMap()

        val timeline = mutableListOf<HudTimelineEvent>()
        val exp = previous?.exp ?: ExpProgress(level = 1, currentExp = 0, expForNext = 100)
        var updatedExp = exp

        signals.sortedBy { it.timestamp }.forEach { signal ->
            val deltas = applySignal(signal, stats)
            if (deltas.isNotEmpty()) {
                timeline += HudTimelineEvent(
                    timestamp = signal.timestamp,
                    message = describeSignal(signal, deltas),
                    deltas = deltas
                )
                val expReward = computeExpReward(signal, deltas)
                if (expReward > 0) {
                    updatedExp = updatedExp.award(expReward)
                }
            }
        }

        val balanceStat = stats[HudStatKind.BALANCE]
        if (balanceStat != null) {
            val balanceScore = HudScoring.computeBalance(stats, now)
            stats[HudStatKind.BALANCE] = balanceStat.withUpdatedBase(balanceScore)
        }

        return HudSnapshot(
            generatedAt = now,
            stats = stats,
            exp = updatedExp,
            activeQuests = activeQuests,
            timeline = (previous?.timeline.orEmpty() + timeline).takeLast(50),
            suggestions = suggestions.ifEmpty { buildDefaultSuggestions(stats, now) }
        )
    }

    private fun applySignal(
        signal: SignalSample,
        stats: MutableMap<HudStatKind, HudStat>
    ): Map<HudStatKind, Double> {
        val deltas = mutableMapOf<HudStatKind, Double>()
        when (signal) {
            is FinancialSignal -> {
                val wealth = stats.getValue(HudStatKind.WEALTH)
                val adjustment = when (signal.type) {
                    FinancialSignalType.SAVE -> minOf(12.0, 4.0 + signal.amount.log2())
                    FinancialSignalType.INVEST -> minOf(15.0, 6.0 + signal.amount.log2())
                    FinancialSignalType.SPEND -> -minOf(10.0, 3.0 + signal.amount.log2())
                }
                stats[HudStatKind.WEALTH] = wealth.withUpdatedBase(wealth.baseScore + adjustment)
                deltas[HudStatKind.WEALTH] = adjustment
            }
            is ActivitySignal -> {
                val vital = stats.getValue(HudStatKind.VITAL)
                val baseBoost = (signal.steps / 1200.0 * 8.0).coerceAtMost(10.0)
                val recoveryBonus = if (signal.isRecovery) 6.0 else 0.0
                val total = baseBoost + recoveryBonus
                stats[HudStatKind.VITAL] = vital.withUpdatedBase(vital.baseScore + total)
                if (total != 0.0) deltas[HudStatKind.VITAL] = total
            }
            is SleepSignal -> {
                val vital = stats.getValue(HudStatKind.VITAL)
                val durationHours = signal.duration.toHours().toDouble()
                val quality = signal.qualityScore.coerceIn(0.0, 1.0)
                val sleepScore = (durationHours / 8.0 * 70 + quality * 30) - signal.interruptions * 3
                val boost = (sleepScore - 70).coerceIn(-15.0, 15.0)
                stats[HudStatKind.VITAL] = vital.withUpdatedBase(vital.baseScore + boost)
                if (boost != 0.0) deltas[HudStatKind.VITAL] = boost
            }
            is FocusSessionSignal -> {
                val cognition = stats.getValue(HudStatKind.COGNITION)
                val focusScore = (signal.duration.toMinutes() / 25.0 * 8.0).coerceAtMost(12.0)
                val interruptionPenalty = signal.interruptions * 2.0
                val total = focusScore - interruptionPenalty + if (signal.isUserInitiated) 2.0 else 0.0
                stats[HudStatKind.COGNITION] = cognition.withUpdatedBase(cognition.baseScore + total)
                if (total != 0.0) deltas[HudStatKind.COGNITION] = total
            }
            is NotificationBurstSignal -> {
                val target = when (signal.category) {
                    NotificationCategory.FINANCE -> HudStatKind.WEALTH
                    NotificationCategory.FITNESS -> HudStatKind.VITAL
                    NotificationCategory.FOCUS -> HudStatKind.COGNITION
                    NotificationCategory.SOCIAL -> HudStatKind.SOCIAL
                    NotificationCategory.ENTERTAINMENT -> HudStatKind.COGNITION
                    NotificationCategory.SYSTEM -> HudStatKind.BALANCE
                }
                val stat = stats[target] ?: return emptyMap()
                val magnitude = when (signal.category) {
                    NotificationCategory.FINANCE -> minOf(10.0, signal.count * 1.5)
                    NotificationCategory.FITNESS -> minOf(8.0, signal.count * 1.2)
                    NotificationCategory.FOCUS -> -minOf(12.0, signal.count * 1.8)
                    NotificationCategory.SOCIAL -> minOf(6.0, signal.count.toDouble())
                    NotificationCategory.ENTERTAINMENT -> -minOf(8.0, signal.count * 1.2)
                    NotificationCategory.SYSTEM -> -minOf(5.0, signal.count * 0.8)
                }
                val quietPenalty = if (signal.quietHours && magnitude > 0) magnitude * 0.5 else 0.0
                val total = magnitude - quietPenalty
                stats[target] = stat.withUpdatedBase(stat.baseScore + total)
                if (total != 0.0) deltas[target] = total
            }
            is ScreenUsageSignal -> {
                val cognition = stats.getValue(HudStatKind.COGNITION)
                val penalty = (signal.duration.toMinutes() / 15.0 * 3.0).coerceAtMost(12.0)
                val total = if (signal.isLateNight) -penalty * 1.5 else -penalty
                stats[HudStatKind.COGNITION] = cognition.withUpdatedBase(cognition.baseScore + total)
                if (total != 0.0) deltas[HudStatKind.COGNITION] = total
                val balance = stats[HudStatKind.BALANCE]
                if (balance != null && total < 0) {
                    val spillover = total * 0.4
                    stats[HudStatKind.BALANCE] = balance.withUpdatedBase(balance.baseScore + spillover)
                    deltas[HudStatKind.BALANCE] = spillover
                }
            }
        }
        return deltas
    }

    private fun describeSignal(signal: SignalSample, deltas: Map<HudStatKind, Double>): String {
        val deltaSummary = deltas.entries.joinToString { (kind, delta) ->
            val sign = if (delta >= 0) "+" else ""
            "${kind.displayName} $sign${"%.1f".format(delta)}"
        }
        return when (signal) {
            is FinancialSignal -> when (signal.type) {
                FinancialSignalType.SAVE -> "저축 신호 처리: $deltaSummary"
                FinancialSignalType.INVEST -> "투자 활동 반영: $deltaSummary"
                FinancialSignalType.SPEND -> "소비 패턴 반영: $deltaSummary"
            }
            is ActivitySignal -> if (signal.isRecovery) {
                "회복 세션 기록: $deltaSummary"
            } else {
                "활동량 갱신: $deltaSummary"
            }
            is SleepSignal -> "수면 패턴 분석: $deltaSummary"
            is FocusSessionSignal -> "집중 세션 추적: $deltaSummary"
            is NotificationBurstSignal -> "알림 버스트 해석: $deltaSummary"
            is ScreenUsageSignal -> "스크린 타임 조정: $deltaSummary"
        }
    }

    private fun computeExpReward(signal: SignalSample, deltas: Map<HudStatKind, Double>): Int {
        if (deltas.isEmpty()) return 0
        val totalDelta = deltas.values.sumOf { abs(it) }
        val cadenceMultiplier = when (signal) {
            is ActivitySignal -> 1.2
            is FocusSessionSignal -> 1.4
            is FinancialSignal -> 1.1
            is SleepSignal -> 1.0
            is NotificationBurstSignal -> 0.8
            is ScreenUsageSignal -> 0.6
        }
        return max(0, (totalDelta * cadenceMultiplier).toInt())
    }

    private fun buildDefaultSuggestions(
        stats: Map<HudStatKind, HudStat>,
        now: Instant
    ): List<CoachingSuggestion> {
        val cognitionScore = stats[HudStatKind.COGNITION]?.effectiveScore(now) ?: 0.0
        val vitalScore = stats[HudStatKind.VITAL]?.effectiveScore(now) ?: 0.0
        val wealthScore = stats[HudStatKind.WEALTH]?.effectiveScore(now) ?: 0.0

        val suggestions = mutableListOf<CoachingSuggestion>()
        if (cognitionScore < 55.0) {
            suggestions += CoachingSuggestion(
                title = "집중력 회복을 위한 빠른 선택",
                options = listOf("25분 포모도로 시작", "방해 요소 알림 30분 차단")
            )
        }
        if (vitalScore < 60.0) {
            suggestions += CoachingSuggestion(
                title = "오늘의 체력 버프",
                options = listOf("10분 스트레칭", "20분 산책")
            )
        }
        if (wealthScore < 58.0) {
            suggestions += CoachingSuggestion(
                title = "재력 균형 맞추기",
                options = listOf("이번 주 저축 자동이체 확인", "소비 요약 메모 작성")
            )
        }
        if (suggestions.isEmpty()) {
            suggestions += CoachingSuggestion(
                title = "현재 버프 유지하기",
                options = listOf("집중 세션 연속 유지", "야간 알림 최소화 루틴 실행")
            )
        }
        return suggestions
    }
}

private fun Double.log2(): Double = kotlin.math.ln(this.coerceAtLeast(1.0)) / kotlin.math.ln(2.0)

