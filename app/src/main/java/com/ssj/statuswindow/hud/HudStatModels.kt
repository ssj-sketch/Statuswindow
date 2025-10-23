package com.ssj.statuswindow.hud

import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Core and optional HUD stats that can be surfaced inside the life RPG status window.
 */
enum class HudStatKind(val displayName: String, val isCore: Boolean) {
    WEALTH("재력", true),
    VITAL("체력", true),
    COGNITION("사고력", true),
    BALANCE("균형", true),
    SOCIAL("사회성", false),
    RESILIENCE("복원력", false),
    MOOD("감정 에너지", false);

    companion object {
        val coreStats: List<HudStatKind> = entries.filter { it.isCore }
    }
}

/**
 * A modifier applied to a stat, representing a buff or debuff window.
 */
data class StatModifier(
    val id: String,
    val source: ModifierSource,
    val stat: HudStatKind,
    val kind: ModifierKind,
    val magnitude: Double,
    val multiplier: Double,
    val expiresAt: Instant?,
    val description: String
) {
    fun isActive(at: Instant): Boolean = expiresAt?.let { at.isBefore(it) } ?: true
}

enum class ModifierKind { BUFF, DEBUFF }

data class ModifierSource(
    val label: String,
    val icon: String? = null
)

/**
 * Represents the smoothed score for a single stat along with active modifiers.
 */
data class HudStat(
    val kind: HudStatKind,
    val baseScore: Double,
    val ewmaAlpha: Double,
    val modifiers: List<StatModifier>
) {
    init {
        require(baseScore in 0.0..100.0) { "base score must be between 0 and 100" }
        require(ewmaAlpha in 0.0..1.0) { "alpha must be 0..1" }
    }

    fun effectiveScore(at: Instant): Double {
        val (multiplierProduct, additive) = modifiers
            .filter { it.isActive(at) }
            .fold(1.0 to 0.0) { acc, modifier ->
                val nextMultiplier = acc.first * modifier.multiplier
                val nextAdditive = acc.second + modifier.magnitude
                nextMultiplier to nextAdditive
            }
        val raw = (baseScore + additive).coerceIn(0.0, 100.0)
        return (raw * multiplierProduct).coerceIn(0.0, 100.0)
    }

    fun withUpdatedBase(observation: Double): HudStat {
        val normalized = observation.coerceIn(0.0, 100.0)
        val updated = (1.0 - ewmaAlpha) * baseScore + ewmaAlpha * normalized
        return copy(baseScore = updated)
    }
}

/**
 * Tracks a quest and the stat impact it will deliver upon completion.
 */
data class Quest(
    val id: String,
    val title: String,
    val cadence: QuestCadence,
    val target: QuestTarget,
    val rewardExp: Int,
    val rewardModifiers: List<StatModifier>,
    val progress: QuestProgress = QuestProgress.NOT_STARTED
)

enum class QuestCadence { DAILY, WEEKLY, EVENT }

data class QuestTarget(
    val description: String,
    val suggestedDuration: Duration? = null,
    val threshold: Double? = null
)

enum class QuestProgress { NOT_STARTED, IN_PROGRESS, COMPLETED, EXPIRED }

/**
 * Encapsulates XP, level, and next level threshold for the player profile.
 */
data class ExpProgress(
    val level: Int,
    val currentExp: Int,
    val expForNext: Int
) {
    init {
        require(level >= 1) { "level must start at 1" }
        require(currentExp >= 0) { "exp cannot be negative" }
        require(expForNext > 0) { "threshold must be positive" }
    }

    fun award(amount: Int): ExpProgress {
        require(amount >= 0) { "amount must be >= 0" }
        if (amount == 0) return this
        var nextLevel = level
        var expPool = currentExp + amount
        var nextThreshold = expForNext
        while (expPool >= nextThreshold) {
            expPool -= nextThreshold
            nextLevel += 1
            nextThreshold = computeNextThreshold(nextLevel)
        }
        return copy(level = nextLevel, currentExp = expPool, expForNext = nextThreshold)
    }

    private fun computeNextThreshold(level: Int): Int {
        val base = 100
        val growthFactor = 1.35
        return (base * growthFactor.pow(level - 1)).toInt()
    }
}

/**
 * A coaching suggestion surfaced alongside the HUD snapshot.
 */
data class CoachingSuggestion(
    val title: String,
    val options: List<String>
)

/**
 * Timeline event describing an update that affected the HUD.
 */
data class HudTimelineEvent(
    val timestamp: Instant,
    val message: String,
    val deltas: Map<HudStatKind, Double>
)

/**
 * Complete snapshot of the HUD state at a given instant.
 */
data class HudSnapshot(
    val generatedAt: Instant,
    val stats: Map<HudStatKind, HudStat>,
    val exp: ExpProgress,
    val activeQuests: List<Quest>,
    val timeline: List<HudTimelineEvent>,
    val suggestions: List<CoachingSuggestion>
) {
    fun statScore(kind: HudStatKind, at: Instant = generatedAt): Double =
        stats[kind]?.effectiveScore(at) ?: 0.0
}

/**
 * Utility helpers to derive new scores for core stats and balance.
 */
object HudScoring {
    fun computeBalance(coreStats: Map<HudStatKind, HudStat>, at: Instant): Double {
        val scores = HudStatKind.coreStats
            .filter { it != HudStatKind.BALANCE }
            .mapNotNull { coreStats[it]?.effectiveScore(at) }
        if (scores.isEmpty()) return 50.0
        val mean = scores.average()
        val variance = scores.fold(0.0) { acc, score -> acc + (score - mean).pow(2) } / scores.size
        val stdev = sqrt(variance)
        val balance = max(0.0, 100.0 - stdev * 1.5)
        return min(100.0, balance)
    }
}

