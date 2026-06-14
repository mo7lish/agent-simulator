package ai.sarj.agentsim.config

import ai.sarj.agentsim.model.Intent
import ai.sarj.agentsim.model.PersonaArchetype
import ai.sarj.agentsim.model.Rank
import ai.sarj.agentsim.model.ShiftMods
import ai.sarj.agentsim.model.Upgrade
import ai.sarj.agentsim.model.UpgradeCategory
import kotlin.math.roundToInt

/**
 * SINGLE SOURCE OF TRUTH for tunable numbers + the editable splash/summary credit + the LLM model.
 */
object GameConfig {

    // ---- Editable credit shown on splash + summary screens (fill these in) ----
    const val STUDENT_NAME = "[STUDENT NAME]"
    const val MATRIC = "[MATRIC NUMBER]"

    // ---- Token economy (no lose condition — it's an endless shift) ----
    const val TOKENS_PER_RESOLVE = 20
    const val SATISFACTION_BONUS_AT = 80          // satisfaction >= this earns a bonus
    const val TOKENS_SATISFACTION_BONUS = 10

    // ---- Conversation spawn cadence (snappier = more alive) ----
    const val FIRST_SPAWN_MS = 700L
    const val SPAWN_INTERVAL_MS = 10000L          // a new chat roughly every 10s...
    const val SPAWN_JITTER = 0.30f                // ...±30%
    const val MAX_OPEN_CONVERSATIONS = 6          // stop spawning past this backlog

    // ---- Summon AI Assistant (recurring co-pilot power-up) ----
    const val SARJ_COST = 150                     // tokens to summon (a real, aspirational spend)
    const val SARJ_RESOLVE_EACH_MS = 460L         // pace of the auto-resolve sweep
    const val SARJ_TOKENS_PER_CHAT = 15           // tokens it banks per chat it clears
    const val SARJ_MIN_HANDLE = 4                 // it also pulls in this many fresh chats if the inbox is thin

    // ---- Queue / impatience (Phase 3) ----
    const val PATIENCE_TICK_MS = 250L
    const val BASE_PATIENCE_MS = 45_000L          // full bar for a 1.0x persona
    const val NUDGE1_AT = 0.66f
    const val NUDGE2_AT = 0.40f
    const val NUDGE3_AT = 0.18f

    // ---- The Last Star (Phase 4) ----
    const val STAR_TOKEN_STEP = 10                 // (stars-3)*step token bonus/penalty
    val COMBO_MULTIPLIERS = floatArrayOf(1f, 1.2f, 1.5f, 2f)
    const val ABANDON_TOKEN_PENALTY = 15
    const val MAX_STRIKES = 3
    const val SHIFT_TARGET_SERVED = 12             // shift ends after this many served
    const val RATING_GOOD = 4.2f                   // above -> warmer, more patient arrivals
    const val RATING_BAD = 3.0f                    // below -> colder, impatient arrivals
    const val REP_PATIENCE_BONUS = 0.20f           // +/- spawn patience from reputation
    const val REVIEW_REVEAL_MS = 1850L
    const val MAX_TICKER_REVIEWS = 8

    // ---- Customer reply pacing (scripted brain; LLM streams for real) ----
    const val TYPING_MIN_MS = 700L
    const val TYPING_MAX_MS = 1600L
    const val OPENING_TIMEOUT_MS = 2500L   // max wait for an LLM-voiced opener before keeping the template
    const val REVIEW_TIMEOUT_MS = 4500L    // max wait for an LLM-voiced star review (updates in the ticker)

    // ---- On-device LLM model (Qwen2.5-1.5B-Instruct, MediaPipe .task) ----
    // Ungated + Apache-2.0 → the app downloads it directly with NO login / NO Hugging Face account.
    const val MODEL_NAME = "Qwen2.5-1.5B-Instruct"
    const val MODEL_FILE = "qwen2.5-1.5b-it-q8.task"
    const val MODEL_URL = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task?download=true"
    const val MODEL_SIZE_BYTES = 1_650_000_000L    // ~1.6 GB (for progress bar + sanity check)
    const val MODEL_MAX_TOKENS = 1280

    // ---- Ranks / XP (v4: gradual leveling + unlock-gated difficulty) ----
    // Difficulty ramps by INTRODUCING complexity one element at a time: each rank turns on
    // the next intent + persona, and nudges spawn/patience. spawnConversation() just filters
    // its random pick by the unlocked prefix of these ordered lists.
    val INTENT_UNLOCK_ORDER = listOf(
        Intent.BALANCE, Intent.FREEZE_CARD, Intent.COMPLAINT,
        Intent.STATEMENT, Intent.RESCHEDULE_BOOKING, Intent.MAKE_PAYMENT,
        Intent.REPLACE_CARD, Intent.DISPUTE_CHARGE, Intent.REPORT_FRAUD
    )
    val PERSONA_UNLOCK_ORDER = listOf(
        PersonaArchetype.WARM, PersonaArchetype.ELDERLY, PersonaArchetype.ANXIOUS, PersonaArchetype.NONNATIVE,
        PersonaArchetype.IMPATIENT, PersonaArchetype.OVERSHARER, PersonaArchetype.COMPLAINER,
        PersonaArchetype.ENTITLED, PersonaArchetype.SUSPICIOUS, PersonaArchetype.TROLL
    )

    val RANKS = listOf(
        //   idx id            title           xp      color        pers int spawn  pat   maxOpen unlockLabel
        // Difficulty ramp: maxOpen rises 1 -> 6, so a Trainee handles ONE chat at a time (nothing else can
        // expire while they work), and patience starts very generous and tightens with each tier.
        Rank(0, "trainee",    "Trainee",         0L, 0xFF94A3B8, 4, 3, 1.10f, 2.40f, 1, ""),
        Rank(1, "junior",     "Junior Agent",  300L, 0xFF12B886, 5, 4, 1.10f, 1.90f, 2, "Statements + impatient customers"),
        Rank(2, "agent",      "Agent",         800L, 0xFF4C6EF5, 6, 5, 1.05f, 1.55f, 3, "Rescheduling + over-sharers"),
        Rank(3, "senior",     "Senior Agent", 1600L, 0xFF7048E8, 8, 6, 1.00f, 1.25f, 4, "Payments, complainers & entitled VIPs"),
        Rank(4, "specialist", "Specialist",   2800L, 0xFFE8590C, 9, 7, 0.95f, 1.05f, 5, "Card replacements + suspicious customers"),
        Rank(5, "lead",       "Team Lead",    4500L, 0xFFE03131, 10, 8, 0.85f, 0.92f, 6, "Charge disputes + trolls"),
        Rank(6, "lead_star",  "Lead ★",       7000L, 0xFFF59F00, 10, 9, 0.78f, 0.85f, 6, "Fraud investigations — the full desk")
    )

    fun rankForXp(xp: Long): Rank = RANKS.last { xp >= it.xpThreshold }
    fun nextRank(r: Rank): Rank? = RANKS.getOrNull(r.index + 1)
    fun rankProgress(xp: Long): Float {
        val r = rankForXp(xp)
        val next = nextRank(r) ?: return 1f
        val span = (next.xpThreshold - r.xpThreshold).toFloat().coerceAtLeast(1f)
        return ((xp - r.xpThreshold) / span).coerceIn(0f, 1f)
    }

    // ---- Shift-end payout (the Summary "conversion surface") ----
    const val SHIFT_BONUS_PER_SERVED = 5
    const val XP_PER_SERVED = 10
    const val XP_PER_BESTCOMBO = 5
    fun gradeFor(rating: Float, served: Int): String = when {
        served <= 0 -> "—"
        rating >= 4.6f -> "S"
        rating >= 4.0f -> "A"
        rating >= 3.0f -> "B"
        else -> "C"
    }
    fun gradeTokenBonus(grade: String): Int = when (grade) { "S" -> 60; "A" -> 40; "B" -> 25; "C" -> 10; else -> 0 }
    fun gradeXpBonus(grade: String): Long = when (grade) { "S" -> 100L; "A" -> 60L; "B" -> 30L; "C" -> 10L; else -> 0L }

    // ---- Upgrades shop (LEVELED token sink: every upgrade keeps leveling up, costing more each time) ----
    val UPGRADES = listOf(
        //      id              title                desc (per level)                                                        base max category               emoji
        Upgrade("raise",         "Pay Raise",        "Earn +10% tokens from every chat you resolve, per level.",               100, 8, UpgradeCategory.POWER, "💵"),
        Upgrade("big_tipper",    "Big Tipper",       "Every 5★ resolve tips you +10 bonus tokens, per level.",                 130, 5, UpgradeCategory.POWER, "🤑"),
        Upgrade("patience_coach","Patience Coach",   "Customers wait +8% longer before getting impatient, per level.",          80, 5, UpgradeCategory.POWER, "⏳"),
        Upgrade("calm_queue",    "Calm Queue",       "New chats arrive 8% less often, per level.",                              70, 5, UpgradeCategory.POWER, "🧘"),
        Upgrade("overtime",      "Overtime",         "Serve +2 more before the shift ends — longer shifts, bigger payout, per level.", 110, 4, UpgradeCategory.POWER, "📈"),
        Upgrade("extra_strike",  "Thicker Skin",     "Survive +1 angry customer (strike) before the shift ends, per level.",    90, 2, UpgradeCategory.POWER, "🛡️"),
        Upgrade("combo_keeper",  "Combo Keeper",     "A 3★ review keeps your hot streak alive (not just 4★ and up).",          220, 1, UpgradeCategory.POWER, "🔥"),
        Upgrade("sarj_discount", "Assistant Discount","Summoning the AI Assistant costs 20 less, per level.",                   120, 3, UpgradeCategory.SARJ,  "🏷️"),
        Upgrade("sarj_smart",    "Assistant Pro",    "The AI Assistant banks +6 tokens per chat it clears, per level.",        160, 4, UpgradeCategory.SARJ,  "⚡")
    )

    /** Cost of the NEXT level (currentLevel is 0-based): escalates ~1.55x each level. */
    fun upgradeCost(up: Upgrade, currentLevel: Int): Int {
        var c = up.baseCost.toDouble()
        repeat(currentLevel) { c *= 1.55 }
        return c.roundToInt()
    }

    // ---- Daily hooks (v4 retention) ----
    const val DAILY_OBJECTIVE_COUNT = 3
    const val DAILY_REROLL_COST = 40
    const val FIRST_WIN_MULT = 2            // first resolved chat of the day pays double
    const val COMEBACK_BONUS = 50           // tokens gifted when you return after missing a day
    const val STREAK_TOKEN_PER_DAY = 10     // streak payout = min(streak,7) * this (granted on new day)
    const val STREAK_TOKEN_CAP_DAYS = 7

    fun modsFor(levels: Map<String, Int>): ShiftMods {
        fun lvl(id: String) = levels[id] ?: 0
        return ShiftMods(
            maxStrikes = MAX_STRIKES + lvl("extra_strike"),
            patienceMult = 1f + 0.08f * lvl("patience_coach"),
            spawnMult = 1f + 0.08f * lvl("calm_queue"),
            targetServed = SHIFT_TARGET_SERVED + 2 * lvl("overtime"),
            comboKeepThreshold = if (lvl("combo_keeper") >= 1) 3 else 4,
            sarjCost = (SARJ_COST - 20 * lvl("sarj_discount")).coerceAtLeast(50),
            tokenMult = 1f + 0.10f * lvl("raise"),
            sarjPerChat = SARJ_TOKENS_PER_CHAT + 6 * lvl("sarj_smart"),
            fiveStarBonus = 10 * lvl("big_tipper")
        )
    }
}
