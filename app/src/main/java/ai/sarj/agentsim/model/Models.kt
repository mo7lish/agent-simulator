package ai.sarj.agentsim.model

import androidx.compose.runtime.Immutable

// ---- App-level navigation ----

enum class Screen { SPLASH, GATE, DOWNLOAD, LOADING, HOME, PLAYING, SUMMARY, SHOP }

enum class Tab(val label: String) {
    CHATS("Chats"),
    ACCOUNTS("Accounts"),
    CARDS("Cards"),
    BOOKINGS("Bookings"),
    PAYMENTS("Payments")
}

// ---- Conversations ----

enum class Sender { CUSTOMER, AGENT, SYSTEM }

/** What the customer wants. Each maps to a tool tab + a resolution check. */
enum class Intent(val tab: Tab) {
    BALANCE(Tab.ACCOUNTS),
    STATEMENT(Tab.ACCOUNTS),
    FREEZE_CARD(Tab.CARDS),
    RESCHEDULE_BOOKING(Tab.BOOKINGS),
    MAKE_PAYMENT(Tab.PAYMENTS),
    COMPLAINT(Tab.CHATS), // resolved by empathy, no tool action
    DISPUTE_CHARGE(Tab.ACCOUNTS),  // flag a named transaction as disputed
    REPORT_FRAUD(Tab.ACCOUNTS),    // find + flag the planted suspicious transaction
    REPLACE_CARD(Tab.CARDS)        // reissue a damaged/lost card
}

enum class ConversationStatus { ONGOING, RESOLVED }

/** An extra action a sensitive request requires before it can be resolved. */
enum class GateKind { NONE, VERIFY_ID, FRAUD_FLAG, TRIAGE }

@Immutable
data class ChatMessage(
    val id: Long,
    val sender: Sender,
    val text: String,
    val streaming: Boolean = false
)

@Immutable
data class Conversation(
    val id: Long,
    val customer: Customer,
    val intent: Intent,
    val messages: List<ChatMessage>,
    val status: ConversationStatus = ConversationStatus.ONGOING,
    val satisfaction: Int = 50,
    val expectedValue: String? = null,   // e.g. the correct balance string for BALANCE
    val targetDate: String? = null,      // requested new date for RESCHEDULE_BOOKING
    val customerTyping: Boolean = false,
    val unread: Boolean = false,
    // Queue / impatience
    val patience: Float = 1f,            // 1 fresh -> 0 about to leave (waiting customers only)
    val patienceMaxMs: Long = 45_000L,   // persona-scaled at spawn
    val nudgeLevel: Int = 0,             // proactive nudges already fired (0..3)
    val agentMsgs: Int = 0,              // agent messages sent (correctness scoring)
    val usedEmpathy: Boolean = false,    // agent ever used empathy/greeting words
    val abandoned: Boolean = false,
    val stars: Int? = null,
    // Action gate (variety: an extra verb before a sensitive request resolves)
    val gate: GateKind = GateKind.NONE,
    val gateCleared: Boolean = false,
    val verifyAsked: Boolean = false,       // agent has requested the customer confirm their card
    val verifyAnswer: String? = null,       // the real card last-4 (what the customer states when asked)
    // Transaction-targeting intents (dispute / fraud): the row the request is about
    val targetTxnIndex: Int? = null,
    // True while the LLM is still composing this chat's opening line (template shows first)
    val openerPending: Boolean = false
) {
    val lastMessage: ChatMessage? get() = messages.lastOrNull()
}

// ---- Customers + fake banking database ----

enum class CardStatus { ACTIVE, FROZEN, REPLACED }

/** A flag an agent can put on a transaction (dispute / fraud report). */
enum class TxnFlag { NONE, DISPUTED, FRAUD }

enum class PersonaArchetype { WARM, IMPATIENT, ELDERLY, COMPLAINER, OVERSHARER, SUSPICIOUS, NONNATIVE, ENTITLED, ANXIOUS, TROLL }

@Immutable
data class Persona(
    val archetype: PersonaArchetype,
    val label: String,             // short display name, e.g. "Impatient executive"
    val promptBlurb: String,       // personality + style, injected into the LLM system prompt
    val patienceMultiplier: Float, // scales wait time (Phase 3)
    val harsh: Boolean,            // harsh raters give lower stars (Phase 4)
    val wSpeed: Float,             // star weighting (~sums to 1)
    val wCorrect: Float,
    val wPolite: Float,
    val accentColor: Long,         // avatar color
    val mood: String               // emoji face
)

@Immutable
data class Customer(
    val id: String,
    val name: String,
    val accountNumber: String, // canonical (no spaces); displayed grouped
    val cardNumber: String,    // canonical digits
    val phone: String,
    val persona: Persona
) {
    val cardLast4: String get() = cardNumber.takeLast(4)
    val accountDisplay: String get() = accountNumber.chunked(4).joinToString(" ")
    val cardDisplay: String get() = "•••• •••• •••• $cardLast4"
}

@Immutable data class Txn(val desc: String, val amount: Int, val flag: TxnFlag = TxnFlag.NONE)

@Immutable
data class Account(
    val number: String,
    val ownerName: String,
    val balance: Int,
    val currency: String = "SAR",
    val statementSent: Boolean = false,
    val transactions: List<Txn> = emptyList()
) {
    val balanceDisplay: String get() = "%,d %s".format(balance, currency)
}

@Immutable
data class Card(
    val number: String,
    val ownerName: String,
    val status: CardStatus
) {
    val last4: String get() = number.takeLast(4)
}

@Immutable
data class Booking(
    val customerId: String,
    val type: String,
    val date: String
)

@Immutable
data class Payment(
    val id: String,
    val customerId: String,
    val payee: String,
    val amount: Int,
    val processed: Boolean = false
)

/** Immutable snapshot of the bank. Tool actions return a new copy. */
@Immutable
data class Bank(
    val accounts: Map<String, Account>,
    val cards: Map<String, Card>,
    val bookings: Map<String, Booking>,  // keyed by customerId
    val payments: Map<String, Payment>   // keyed by paymentId
) {
    fun setCard(cardNumber: String, status: CardStatus): Bank {
        val c = cards[cardNumber] ?: return this
        return copy(cards = cards + (cardNumber to c.copy(status = status)))
    }

    fun sendStatement(accountNumber: String): Bank {
        val a = accounts[accountNumber] ?: return this
        return copy(accounts = accounts + (accountNumber to a.copy(statementSent = true)))
    }

    fun reschedule(customerId: String, newDate: String): Bank {
        val b = bookings[customerId] ?: return this
        return copy(bookings = bookings + (customerId to b.copy(date = newDate)))
    }

    fun processPayment(paymentId: String): Bank {
        val p = payments[paymentId] ?: return this
        return copy(payments = payments + (paymentId to p.copy(processed = true)))
    }

    fun setTxnFlag(accountNumber: String, txnIndex: Int, flag: TxnFlag): Bank {
        val a = accounts[accountNumber] ?: return this
        if (txnIndex !in a.transactions.indices) return this
        val newTxns = a.transactions.toMutableList().also { it[txnIndex] = it[txnIndex].copy(flag = flag) }
        return copy(accounts = accounts + (accountNumber to a.copy(transactions = newTxns)))
    }

    fun paymentFor(customerId: String): Payment? = payments.values.firstOrNull { it.customerId == customerId }
}

// ---- Whole-app state ----

@Immutable
data class GameState(
    val screen: Screen = Screen.SPLASH,
    val tab: Tab = Tab.CHATS,
    val tokens: Int = 0,
    val bank: Bank,
    val roster: List<Customer> = emptyList(),   // the per-shift procedurally-generated customers
    val conversations: List<Conversation> = emptyList(),
    val openConversationId: Long? = null,
    val clipboard: String? = null,          // in-app "copied" value
    val resolvedCount: Int = 0,
    val sarjActive: Boolean = false,
    val sarjUsed: Int = 0,
    val brainLabel: String = "Basic",       // "On-device AI" once the LLM is loaded
    // Boot / model-loading state
    val gateReason: String? = null,
    val loadError: String? = null,
    val downloadProgress: Float = 0f,
    val downloading: Boolean = false,
    // Ratings / reputation (The Last Star)
    val shiftStars: List<Int> = emptyList(),
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val servedByAgent: Int = 0,
    val strikes: Int = 0,
    val pendingReview: Review? = null,
    val reviews: List<Review> = emptyList(),
    val abandonedCount: Int = 0,
    val shiftReport: ShiftReport? = null,
    // Effective per-shift values (seeded from rank + owned upgrades at startShift; defaults = GameConfig)
    val maxStrikes: Int = 3,
    val sarjCost: Int = 60,
    val targetServed: Int = 12,  // serve this many to finish the shift (shown as shift progress)
    val shiftStartMs: Long = 0L  // wall-clock at shift start (for the running shift timer)
) {
    val openConversation: Conversation?
        get() = conversations.firstOrNull { it.id == openConversationId }
    val canSummonSarj: Boolean
        get() = !sarjActive && conversations.any { it.status == ConversationStatus.ONGOING }
    val shiftRating: Float
        get() = if (shiftStars.isEmpty()) 0f else shiftStars.average().toFloat()
}

/** Persistent player state (saved via DataStore) — survives shifts and app restarts. */
@Immutable
data class PlayerProfile(
    val tokens: Int = 0,                       // persistent wallet
    val xp: Long = 0L,
    val streakDays: Int = 0,
    val lastPlayedEpochDay: Long = 0L,
    val upgradeLevels: Map<String, Int> = emptyMap(),   // upgradeId -> current level (0 = not bought)
    val agentName: String = "",
    val themeId: Int = 0,
    val muted: Boolean = false,
    val tutorialSeen: Boolean = false,
    val seenIntents: Set<String> = emptySet(),   // request types the player has been coached on
    val firstWinDay: Long = -1L,               // epoch-day the first-win bonus was last claimed
    val bestEndless: Int = 0,
    val lifetimeServed: Int = 0,
    val dailyDay: Long = -1L,                  // epoch-day the current daily objectives belong to
    val daily: List<DailyObjective> = emptyList()
)

// ---- Daily objectives (v4 retention hooks) ----

enum class ObjectiveKind(val text: String, val target: Int, val reward: Int) {
    SERVE_8("Serve 8 customers", 8, 40),
    SERVE_15("Serve 15 customers in a day", 15, 70),
    FIVE_STARS_3("Earn three 5★ ratings", 3, 50),
    COMBO_4("Reach a x4 combo", 4, 50),
    NO_ABANDON("Finish a shift with no walkouts", 1, 60),
    EMPATHY_6("Use a kind word in 6 replies", 6, 30),
    SARJ_1("Use the AI Assistant once", 1, 25)
}

@Immutable
data class DailyObjective(val kind: ObjectiveKind, val progress: Int, val claimed: Boolean) {
    val done: Boolean get() = progress >= kind.target
}

// ---- Upgrades shop (v4) ----

enum class UpgradeCategory(val label: String) { POWER("Power-ups"), SARJ("AI Assistant"), COMFORT("Comfort") }

/** A leveled, repeatable upgrade. Each level costs more and pushes its effect further. */
@Immutable
data class Upgrade(
    val id: String,
    val title: String,
    val desc: String,        // describes the per-level effect
    val baseCost: Int,       // cost of the first level (escalates per level)
    val maxLevel: Int,
    val category: UpgradeCategory,
    val emoji: String
)

/** Effective per-shift constants after applying the player's owned upgrades. */
@Immutable
data class ShiftMods(
    val maxStrikes: Int,
    val patienceMult: Float,
    val spawnMult: Float,          // >1 = chats arrive slower
    val targetServed: Int,
    val comboKeepThreshold: Int,   // stars >= this keeps your combo (default 4)
    val sarjCost: Int,
    val tokenMult: Float,          // multiplier on resolve earnings
    val sarjPerChat: Int,
    val fiveStarBonus: Int         // extra tokens for each 5★ resolve
)

/** A career rank. Higher ranks unlock more personas/intents and ramp difficulty. */
@Immutable
data class Rank(
    val index: Int,
    val id: String,
    val title: String,
    val xpThreshold: Long,
    val color: Long,
    val personasUnlocked: Int,   // how many of GameConfig.PERSONA_UNLOCK_ORDER are live
    val intentsUnlocked: Int,    // how many of GameConfig.INTENT_UNLOCK_ORDER are live
    val spawnMult: Float,        // scales spawn interval (higher = calmer/slower arrivals)
    val patienceMult: Float,     // scales customer patience (higher = more forgiving)
    val maxOpen: Int,            // max concurrent waiting chats at this tier (fewer = gentler)
    val unlockLabel: String      // what this rank newly unlocks (shown on rank-up)
)

/** End-of-shift payout breakdown — the conversion surface on the Summary screen. */
@Immutable
data class ShiftReport(
    val served: Int,
    val abandoned: Int,
    val avgStars: Float,
    val grade: String,
    val tokensEarnedShift: Int,  // tokens earned during the shift (resolves + combos)
    val shiftBonus: Int,         // end-of-shift bonus (grade + per-served)
    val xpGained: Long,
    val xpBefore: Long,
    val xpAfter: Long,
    val rankBefore: Rank,
    val rankAfter: Rank,
    val rankedUp: Boolean,
    val unlocked: String?
)

@Immutable
data class Review(
    val id: Long,
    val customerName: String,
    val accentColor: Long,
    val mood: String,
    val stars: Int,
    val text: String
)
