package ai.sarj.agentsim.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import ai.sarj.agentsim.config.GameConfig
import ai.sarj.agentsim.audio.Sfx
import ai.sarj.agentsim.audio.SoundManager
import ai.sarj.agentsim.data.CustomerFactory
import ai.sarj.agentsim.data.ConversationSeeds
import ai.sarj.agentsim.data.PersonaLines
import ai.sarj.agentsim.data.ProfileRepository
import ai.sarj.agentsim.model.PlayerProfile
import ai.sarj.agentsim.llm.CustomerBrain
import ai.sarj.agentsim.llm.LlmCustomerBrain
import ai.sarj.agentsim.llm.ModelManager
import ai.sarj.agentsim.llm.ScriptedCustomerBrain
import ai.sarj.agentsim.model.CardStatus
import ai.sarj.agentsim.model.ChatMessage
import ai.sarj.agentsim.model.Conversation
import ai.sarj.agentsim.model.ConversationStatus
import ai.sarj.agentsim.model.DailyObjective
import ai.sarj.agentsim.model.GameState
import ai.sarj.agentsim.model.GateKind
import ai.sarj.agentsim.model.Intent
import ai.sarj.agentsim.model.ObjectiveKind
import ai.sarj.agentsim.model.Review
import ai.sarj.agentsim.model.Screen
import ai.sarj.agentsim.model.Sender
import ai.sarj.agentsim.model.ShiftMods
import ai.sarj.agentsim.model.TxnFlag
import ai.sarj.agentsim.model.ShiftReport
import ai.sarj.agentsim.model.Tab
import ai.sarj.agentsim.util.DeviceGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt
import kotlin.random.Random

class AppViewModel : ViewModel() {

    private val _state = MutableStateFlow(
        CustomerFactory.generate(seed = 1L).let { (roster, bank) -> GameState(bank = bank, roster = roster) }
    )
    val state: StateFlow<GameState> = _state.asStateFlow()

    @Volatile private var brain: CustomerBrain = ScriptedCustomerBrain()

    private var shiftScope: CoroutineScope = newShiftScope()
    private var shiftEpoch = 0
    private var nextConvoId = 1L
    private var nextMsgId = 1L
    private val processing = HashSet<Long>()
    private var booted = false
    private var reviewSeq = 0L
    private var shiftEnding = false
    private var shiftStartTokens = 0
    private var shiftMods: ShiftMods = GameConfig.modsFor(emptyMap())
    @Volatile private var openerGenInFlight = false
    @Volatile private var reviewGenInFlight = false

    private var profileRepo: ProfileRepository? = null
    private val soundManager = SoundManager()
    private val _profile = MutableStateFlow(PlayerProfile())
    val profile: StateFlow<PlayerProfile> = _profile.asStateFlow()

    private fun saveProfile(transform: (PlayerProfile) -> PlayerProfile) {
        val repo = profileRepo ?: return
        val updated = transform(_profile.value)
        _profile.value = updated
        viewModelScope.launch { repo.save(updated) }
    }

    fun toggleMute() {
        val muted = !_profile.value.muted
        soundManager.muted = muted
        saveProfile { it.copy(muted = muted) }
    }

    fun markTutorialSeen() {
        if (!_profile.value.tutorialSeen) saveProfile { it.copy(tutorialSeen = true) }
    }

    /** Mark a request type as coached so its first-time guide stops appearing. */
    fun markIntentSeen(intent: Intent) {
        if (intent.name !in _profile.value.seenIntents) saveProfile { it.copy(seenIntents = it.seenIntents + intent.name) }
    }

    fun sfx(s: Sfx) = soundManager.play(s)

    private fun newShiftScope(): CoroutineScope =
        CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext.job))

    // ---- Shift lifecycle ----

    fun startShift() {
        ensureDaily()
        shiftScope.cancel()
        shiftScope = newShiftScope()
        shiftEpoch++
        nextConvoId = 1L; nextMsgId = 1L; processing.clear(); shiftEnding = false
        shiftStartTokens = _profile.value.tokens
        shiftMods = GameConfig.modsFor(_profile.value.upgradeLevels)
        val (roster, bank) = CustomerFactory.generate(seed = System.currentTimeMillis() + shiftEpoch)
        _state.value = GameState(
            screen = Screen.PLAYING, bank = bank, roster = roster, brainLabel = brain.label,
            tokens = _profile.value.tokens, maxStrikes = shiftMods.maxStrikes, sarjCost = shiftMods.sarjCost,
            targetServed = shiftMods.targetServed, shiftStartMs = System.currentTimeMillis()
        )
        startSpawnLoop()
        startPatienceLoop()
    }

    fun endShift() {
        if (_state.value.screen == Screen.SUMMARY) return   // already ended (don't double-pay)
        shiftScope.cancel()
        shiftScope = newShiftScope()
        shiftEpoch++
        processing.clear()
        // End-of-shift objectives (rewards land in the live wallet before we tally the report).
        _state.value.let { s ->
            if (s.servedByAgent > 0) {
                progressObjective(ObjectiveKind.COMBO_4, setTo = s.bestCombo)
                if (s.abandonedCount == 0) progressObjective(ObjectiveKind.NO_ABANDON, setTo = 1)
            }
        }
        val report = buildShiftReport(_state.value)
        saveProfile { p ->
            p.copy(
                tokens = p.tokens + report.shiftBonus,
                xp = report.xpAfter,
                bestEndless = maxOf(p.bestEndless, report.served),
                lifetimeServed = p.lifetimeServed + report.served
            )
        }
        _state.update { it.copy(screen = Screen.SUMMARY, sarjActive = false, tokens = it.tokens + report.shiftBonus, shiftReport = report) }
        if (report.rankedUp) soundManager.play(Sfx.LEVEL_UP)
    }

    private fun buildShiftReport(s: GameState): ShiftReport {
        val served = s.servedByAgent
        val avg = s.shiftRating
        val xpBefore = _profile.value.xp
        val rankBefore = GameConfig.rankForXp(xpBefore)
        if (served <= 0) {
            // Empty shift — no payout, no XP (can't farm tokens by ending instantly).
            return ShiftReport(0, s.abandonedCount, avg, "—", 0, 0, 0L, xpBefore, xpBefore, rankBefore, rankBefore, false, null)
        }
        val grade = GameConfig.gradeFor(avg, served)
        val shiftBonus = served * GameConfig.SHIFT_BONUS_PER_SERVED + GameConfig.gradeTokenBonus(grade)
        val xpGained = served.toLong() * GameConfig.XP_PER_SERVED +
            s.bestCombo.toLong() * GameConfig.XP_PER_BESTCOMBO + GameConfig.gradeXpBonus(grade)
        val xpAfter = xpBefore + xpGained
        val rankAfter = GameConfig.rankForXp(xpAfter)
        val rankedUp = rankAfter.index > rankBefore.index
        return ShiftReport(
            served = served, abandoned = s.abandonedCount, avgStars = avg, grade = grade,
            tokensEarnedShift = (s.tokens - shiftStartTokens).coerceAtLeast(0),
            shiftBonus = shiftBonus, xpGained = xpGained, xpBefore = xpBefore, xpAfter = xpAfter,
            rankBefore = rankBefore, rankAfter = rankAfter, rankedUp = rankedUp,
            unlocked = if (rankedUp) rankAfter.unlockLabel.ifEmpty { null } else null
        )
    }

    fun setBrain(newBrain: CustomerBrain) {
        val old = brain
        brain = newBrain
        if (old !== newBrain) old.close()
        _state.update { it.copy(brainLabel = newBrain.label) }
    }

    fun goHome() = _state.update { it.copy(screen = Screen.HOME) }

    fun goShop() = _state.update { it.copy(screen = Screen.SHOP) }

    fun buyUpgrade(id: String) {
        val up = GameConfig.UPGRADES.firstOrNull { it.id == id } ?: return
        val p = _profile.value
        val level = p.upgradeLevels[id] ?: 0
        if (level >= up.maxLevel) { soundManager.play(Sfx.ERROR); return }
        val cost = GameConfig.upgradeCost(up, level)
        if (p.tokens < cost) { soundManager.play(Sfx.ERROR); return }
        saveProfile { it.copy(tokens = it.tokens - cost, upgradeLevels = it.upgradeLevels + (id to level + 1)) }
        soundManager.play(Sfx.TOKEN_EARN)
        soundManager.play(Sfx.COMBO_UP)
    }

    // ---- Daily hooks: streak, first-win 2x, objectives, comeback bonus ----

    private fun todayDay(): Long = System.currentTimeMillis() / 86_400_000L

    private fun pickDailyObjectives(day: Long): List<DailyObjective> {
        val pool = ObjectiveKind.entries.toMutableList()
        pool.shuffle(Random(day))           // deterministic per day → stable across restarts
        return pool.take(GameConfig.DAILY_OBJECTIVE_COUNT).map { DailyObjective(it, 0, false) }
    }

    /** Roll over streak + objectives + comeback bonus when a new day begins. Idempotent per day. */
    private fun ensureDaily() {
        val today = todayDay()
        val p = _profile.value
        // Already set up for today (and not corrupted) → nothing to do.
        if (p.dailyDay == today && p.daily.size == GameConfig.DAILY_OBJECTIVE_COUNT) return
        // Same day but objectives are missing/corrupt → just re-seed objectives, keep streak.
        if (p.dailyDay == today) {
            saveProfile { it.copy(daily = pickDailyObjectives(today)) }
            return
        }
        val last = p.lastPlayedEpochDay
        var streak = p.streakDays
        var comeback = 0
        when {
            last <= 0L -> streak = 1
            today == last + 1 -> streak += 1
            today > last + 1 -> { streak = 1; comeback = GameConfig.COMEBACK_BONUS }
            else -> { /* same/earlier day already handled by the guard above */ }
        }
        val streakPay = minOf(streak, GameConfig.STREAK_TOKEN_CAP_DAYS) * GameConfig.STREAK_TOKEN_PER_DAY
        saveProfile {
            it.copy(
                dailyDay = today, daily = pickDailyObjectives(today), firstWinDay = -1L,
                streakDays = streak, lastPlayedEpochDay = today,
                tokens = it.tokens + comeback + streakPay
            )
        }
    }

    /** Add tokens to the live shift wallet if a shift is running, otherwise to the persistent profile. */
    private fun grantTokens(n: Int) {
        if (n <= 0) return
        if (_state.value.screen == Screen.PLAYING) {
            _state.update { it.copy(tokens = it.tokens + n) }
            saveProfile { it.copy(tokens = _state.value.tokens) }
        } else {
            saveProfile { it.copy(tokens = it.tokens + n) }
        }
    }

    /** Advance a daily objective; auto-claims its reward on completion. No-op if not active today. */
    private fun progressObjective(kind: ObjectiveKind, delta: Int = 1, setTo: Int? = null) {
        val obj = _profile.value.daily.firstOrNull { it.kind == kind } ?: return
        if (obj.claimed) return
        val np = (setTo ?: (obj.progress + delta)).coerceIn(0, kind.target)
        if (np <= obj.progress) return
        val becameDone = np >= kind.target
        saveProfile { p ->
            p.copy(daily = p.daily.map { o -> if (o.kind == kind) o.copy(progress = np, claimed = becameDone) else o })
        }
        if (becameDone) {
            grantTokens(kind.reward)
            soundManager.play(Sfx.LEVEL_UP)
        }
    }

    fun rerollObjective(kind: ObjectiveKind) {
        val p = _profile.value
        val obj = p.daily.firstOrNull { it.kind == kind } ?: return
        if (obj.claimed) return
        if (p.tokens < GameConfig.DAILY_REROLL_COST) { soundManager.play(Sfx.ERROR); return }
        val currentKinds = p.daily.map { it.kind }.toSet()
        val replacement = ObjectiveKind.entries.filter { it !in currentKinds }.randomOrNull() ?: return
        saveProfile {
            it.copy(
                tokens = it.tokens - GameConfig.DAILY_REROLL_COST,
                daily = it.daily.map { o -> if (o.kind == kind) DailyObjective(replacement, 0, false) else o }
            )
        }
        soundManager.play(Sfx.TAP)
    }

    // ---- Boot flow: SPLASH -> GATE -> DOWNLOAD -> LOADING -> HOME ----

    fun boot(context: Context) {
        if (booted) return
        booted = true
        val app = context.applicationContext
        viewModelScope.launch {
            val repo = ProfileRepository(app)
            profileRepo = repo
            _profile.value = repo.profile.first()
            soundManager.muted = _profile.value.muted
            ensureDaily()
            delay(900)
            val gate = DeviceGate.check(app)
            if (!gate.ok) {
                _state.update { it.copy(screen = Screen.GATE, gateReason = gate.reason) }
                return@launch
            }
            val existing = ModelManager(app).existingModel()
            if (existing == null) _state.update { it.copy(screen = Screen.DOWNLOAD) }
            else loadModelInternal(app, existing.absolutePath)
        }
    }

    private suspend fun loadModelInternal(context: Context, path: String) {
        _state.update { it.copy(screen = Screen.LOADING, loadError = null) }
        val newBrain = LlmCustomerBrain.create(context, path)
        if (newBrain != null) {
            setBrain(newBrain)
            _state.update { it.copy(screen = Screen.HOME) }
        } else {
            _state.update { it.copy(loadError = "Couldn't load the AI on this device (it may be low on memory).") }
        }
    }

    fun downloadModel(context: Context) {
        val app = context.applicationContext
        _state.update { it.copy(downloading = true, downloadProgress = 0f, loadError = null) }
        viewModelScope.launch {
            runCatching { ModelManager(app).download { p -> _state.update { it.copy(downloadProgress = p) } } }
                .onSuccess { f -> _state.update { it.copy(downloading = false) }; loadModelInternal(app, f.absolutePath) }
                .onFailure { e -> _state.update { it.copy(downloading = false, loadError = e.message ?: "Download failed.") } }
        }
    }

    fun importModel(context: Context, uri: Uri) {
        val app = context.applicationContext
        _state.update { it.copy(downloading = true, downloadProgress = 0f, loadError = null) }
        viewModelScope.launch {
            runCatching { ModelManager(app).importFromUri(uri) { p -> _state.update { it.copy(downloadProgress = p) } } }
                .onSuccess { f -> _state.update { it.copy(downloading = false) }; loadModelInternal(app, f.absolutePath) }
                .onFailure { e -> _state.update { it.copy(downloading = false, loadError = e.message ?: "Import failed.") } }
        }
    }

    fun retryLoad(context: Context) {
        val app = context.applicationContext
        viewModelScope.launch {
            val existing = ModelManager(app).existingModel()
            if (existing != null) loadModelInternal(app, existing.absolutePath)
            else _state.update { it.copy(screen = Screen.DOWNLOAD, loadError = null) }
        }
    }

    // ---- Navigation / clipboard ----

    fun selectTab(tab: Tab) = _state.update { it.copy(tab = tab) }

    fun openConversation(id: Long) = _state.update { s ->
        s.copy(tab = Tab.CHATS, openConversationId = id, conversations = s.conversations.map { if (it.id == id) it.copy(unread = false) else it })
    }

    fun closeConversation() = _state.update { it.copy(openConversationId = null) }

    fun copyToClipboard(value: String) = _state.update { it.copy(clipboard = value) }

    // ---- Tool actions ----

    fun setCardStatus(cardNumber: String, status: CardStatus) =
        _state.update { it.copy(bank = it.bank.setCard(cardNumber.normalizedNumber(), status)) }

    fun sendStatement(accountNumber: String) =
        _state.update { it.copy(bank = it.bank.sendStatement(accountNumber.normalizedNumber())) }

    fun reschedule(customerId: String, newDate: String) =
        _state.update { it.copy(bank = it.bank.reschedule(customerId, newDate)) }

    fun processPayment(paymentId: String) =
        _state.update { it.copy(bank = it.bank.processPayment(paymentId)) }

    fun disputeTransaction(accountNumber: String, txnIndex: Int) =
        _state.update { it.copy(bank = it.bank.setTxnFlag(accountNumber.normalizedNumber(), txnIndex, TxnFlag.DISPUTED)) }

    fun reportFraud(accountNumber: String, txnIndex: Int) =
        _state.update { it.copy(bank = it.bank.setTxnFlag(accountNumber.normalizedNumber(), txnIndex, TxnFlag.FRAUD)) }

    fun replaceCard(cardNumber: String) =
        _state.update { it.copy(bank = it.bank.setCard(cardNumber.normalizedNumber(), CardStatus.REPLACED)) }

    // ---- Conversation flow ----

    fun sendAgentMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val convo = _state.value.openConversation ?: return
        if (convo.status != ConversationStatus.ONGOING) return
        if (convo.id in processing) return
        processing.add(convo.id)

        val agentMsg = ChatMessage(nextMsgId++, Sender.AGENT, trimmed)
        val streamId = nextMsgId++
        val typingMsg = ChatMessage(streamId, Sender.CUSTOMER, "", streaming = true)
        val empathy = hasEmpathy(trimmed)
        updateConvo(convo.id) {
            it.copy(messages = it.messages + agentMsg + typingMsg, customerTyping = true, agentMsgs = it.agentMsgs + 1, usedEmpathy = it.usedEmpathy || empathy)
        }
        soundManager.play(Sfx.MESSAGE_SEND)
        if (empathy) progressObjective(ObjectiveKind.EMPATHY_6)

        val appResolved = checkAppResolved(convo, trimmed)
        val convoForBrain = convo.copy(messages = convo.messages + agentMsg)
        val myEpoch = shiftEpoch

        shiftScope.launch {
            try {
                val turn = brain.turn(convoForBrain, trimmed, appResolved) { delta ->
                    if (shiftEpoch == myEpoch) updateConvo(convo.id) { c ->
                        c.copy(messages = c.messages.map { m -> if (m.id == streamId) m.copy(text = m.text + delta) else m })
                    }
                }
                if (shiftEpoch == myEpoch) {
                    updateConvo(convo.id) { c ->
                        c.copy(customerTyping = false, satisfaction = turn.satisfaction,
                            messages = c.messages.map { m -> if (m.id == streamId) m.copy(text = turn.replyText, streaming = false) else m })
                    }
                    soundManager.play(Sfx.MESSAGE_RECEIVE)
                    if (turn.resolved) {
                        val c = _state.value.conversations.firstOrNull { it.id == convo.id }
                        applyOutcome(convo.id, stars = if (c != null) computeStars(c) else 3, abandoned = false, byAgent = true)
                    }
                }
            } finally {
                if (shiftEpoch == myEpoch) {
                    updateConvo(convo.id) { it.copy(customerTyping = false) }
                    processing.remove(convo.id)
                }
            }
        }
    }

    /** Step 1 of identity check: ask the customer to confirm their card; they reply with their last-4. */
    fun askVerify(id: Long) {
        val convo = _state.value.conversations.firstOrNull { it.id == id } ?: return
        if (convo.gate == GateKind.NONE || convo.gateCleared || convo.verifyAsked) return
        val last4 = convo.verifyAnswer ?: return
        updateConvo(id) {
            it.copy(
                verifyAsked = true,
                messages = it.messages +
                    ChatMessage(nextMsgId++, Sender.AGENT, "For security, can you confirm the last 4 digits of your card?") +
                    ChatMessage(nextMsgId++, Sender.CUSTOMER, "Sure — my card ends in $last4.")
            )
        }
        soundManager.play(Sfx.MESSAGE_SEND)
    }

    /** Step 2: confirm the customer's stated card matches the record — clears the gate. */
    fun confirmVerify(id: Long) {
        val convo = _state.value.conversations.firstOrNull { it.id == id } ?: return
        if (convo.gate == GateKind.NONE || convo.gateCleared || !convo.verifyAsked) return
        updateConvo(id) { it.copy(gateCleared = true) }
        soundManager.play(Sfx.STAR_POP, 3)
    }

    private fun checkAppResolved(convo: Conversation, agentText: String): Boolean {
        if (convo.gate != GateKind.NONE && !convo.gateCleared) return false  // verify identity first
        val bank = _state.value.bank
        val c = convo.customer
        return when (convo.intent) {
            Intent.BALANCE -> {
                val live = bank.accounts[c.accountNumber]?.balance?.toString()
                live != null && Regex("\\d+").findAll(agentText).any { it.value == live }
            }
            Intent.STATEMENT -> bank.accounts[c.accountNumber]?.statementSent == true
            Intent.FREEZE_CARD -> bank.cards[c.cardNumber]?.status == CardStatus.FROZEN
            Intent.RESCHEDULE_BOOKING -> convo.targetDate != null && bank.bookings[c.id]?.date == convo.targetDate
            Intent.MAKE_PAYMENT -> bank.paymentFor(c.id)?.processed == true
            // A complaint needs a real two-step: acknowledge first, then properly address it (not one bare "sorry").
            Intent.COMPLAINT -> hasEmpathy(agentText) && convo.agentMsgs >= 1
            Intent.DISPUTE_CHARGE ->
                bank.accounts[c.accountNumber]?.transactions?.getOrNull(convo.targetTxnIndex ?: -1)?.flag == TxnFlag.DISPUTED
            Intent.REPORT_FRAUD ->
                bank.accounts[c.accountNumber]?.transactions?.getOrNull(convo.targetTxnIndex ?: -1)?.flag == TxnFlag.FRAUD
            Intent.REPLACE_CARD -> bank.cards[c.cardNumber]?.status == CardStatus.REPLACED
        }
    }

    // ---- The Last Star: outcomes (agent resolve / abandon / Sarj) ----

    private fun computeStars(c: Conversation): Int {
        val p = c.customer.persona
        val speed = c.patience.coerceIn(0f, 1f)
        val misses = (c.agentMsgs - 1).coerceAtLeast(0)
        val correct = when (misses) { 0 -> 1f; 1 -> 0.7f; 2 -> 0.45f; else -> 0.25f }
        val polite = if (c.usedEmpathy) 1f else 0.55f
        val raw = p.wSpeed * speed + p.wCorrect * correct + p.wPolite * polite
        val harsh = if (p.harsh) 0.85f else 1.0f
        return (raw * 5f * harsh).roundToInt().coerceIn(1, 5)
    }

    private fun applyOutcome(id: Long, stars: Int, abandoned: Boolean, byAgent: Boolean, sarj: Boolean = false) {
        val convo0 = _state.value.conversations.firstOrNull { it.id == id } ?: return
        if (convo0.status == ConversationStatus.RESOLVED) return
        val persona = convo0.customer.persona
        val bucket = PersonaLines.bucketFor(stars, abandoned)
        val review = Review(reviewSeq++, convo0.customer.name, persona.accentColor, persona.mood, stars, PersonaLines.review(persona.archetype, bucket))
        val myEpoch = shiftEpoch

        val today = todayDay()
        val firstWinToday = byAgent && !abandoned && !sarj && _profile.value.firstWinDay != today

        var ended = false
        _state.update { s ->
            val c = s.conversations.firstOrNull { it.id == id } ?: return@update s
            if (c.status == ConversationStatus.RESOLVED) return@update s
            var tokens = s.tokens
            var combo = s.combo
            var strikes = s.strikes
            var served = s.servedByAgent
            when {
                sarj -> { tokens += shiftMods.sarjPerChat; served += 1 }  // the AI you paid for counts toward your shift
                abandoned -> { tokens = (tokens - GameConfig.ABANDON_TOKEN_PENALTY).coerceAtLeast(0); combo = 0; strikes += 1 }
                else -> {
                    served += 1
                    combo = if (stars >= shiftMods.comboKeepThreshold) combo + 1 else 0
                    val idx = if (combo >= 1) (combo - 1).coerceAtMost(GameConfig.COMBO_MULTIPLIERS.size - 1) else 0
                    val base = (GameConfig.TOKENS_PER_RESOLVE + (stars - 3) * GameConfig.STAR_TOKEN_STEP).coerceAtLeast(0)
                    val firstWinMult = if (firstWinToday) GameConfig.FIRST_WIN_MULT else 1
                    tokens += (base * GameConfig.COMBO_MULTIPLIERS[idx] * shiftMods.tokenMult * firstWinMult).toInt()
                    if (stars >= 5) tokens += shiftMods.fiveStarBonus   // Big Tipper bonus
                    if (stars <= 2) strikes += 1
                }
            }
            if (strikes >= shiftMods.maxStrikes || served >= shiftMods.targetServed) ended = true
            s.copy(
                tokens = tokens, combo = combo, bestCombo = maxOf(s.bestCombo, combo), strikes = strikes,
                resolvedCount = s.resolvedCount + 1,
                servedByAgent = served,
                abandonedCount = s.abandonedCount + if (abandoned) 1 else 0,
                // The AI Assistant's resolves now count toward your rating too (you summoned it).
                shiftStars = s.shiftStars + stars,
                pendingReview = review,
                reviews = (listOf(review) + s.reviews).take(GameConfig.MAX_TICKER_REVIEWS),
                conversations = s.conversations.map { if (it.id == id) it.copy(status = ConversationStatus.RESOLVED, stars = stars, abandoned = abandoned) else it }
                // Don't auto-close the chat you're reading — you tap Back when you're done.
            )
        }
        if (ended) shiftEnding = true // stop spawn/patience loops so the summary freezes

        saveProfile { it.copy(tokens = _state.value.tokens) }

        // Daily objectives + first-win-of-the-day bonus
        if (byAgent && !abandoned) {
            progressObjective(ObjectiveKind.SERVE_8)
            progressObjective(ObjectiveKind.SERVE_15)
            if (stars >= 5) progressObjective(ObjectiveKind.FIVE_STARS_3)
        }
        if (firstWinToday) {
            saveProfile { it.copy(firstWinDay = today) }
            soundManager.play(Sfx.COMBO_UP)
        }

        when {
            abandoned -> soundManager.play(Sfx.STRIKE_BUZZ)
            sarj -> soundManager.play(Sfx.TOKEN_EARN)
            else -> {
                soundManager.play(Sfx.TOKEN_EARN)
                if (stars >= 4) soundManager.play(Sfx.COMBO_UP)
                if (stars <= 2) soundManager.play(Sfx.STRIKE_BUZZ)
                shiftScope.launch { for (i in 0 until stars) { delay(120L + i * 130L); soundManager.play(Sfx.STAR_POP, i) } }
            }
        }

        // Let the LLM voice this customer's review (updates the template in place when it lands).
        if (byAgent && !abandoned) launchReview(review.id, convo0, stars)

        shiftScope.launch {
            delay(GameConfig.REVIEW_REVEAL_MS)
            // Keep the resolved chat in the list so the player can still open and read it — just clear the banner.
            _state.update { s ->
                s.copy(pendingReview = if (s.pendingReview?.id == review.id) null else s.pendingReview)
            }
        }
        if (ended) viewModelScope.launch {
            delay(GameConfig.REVIEW_REVEAL_MS + 500)
            if (shiftEpoch == myEpoch && _state.value.screen == Screen.PLAYING) endShift()
        }
    }

    // ---- Spawning ----

    private fun startSpawnLoop() {
        shiftScope.launch {
            delay(GameConfig.FIRST_SPAWN_MS)
            spawnConversation()
            while (isActive) {
                val rank = GameConfig.rankForXp(_profile.value.xp)
                val jitter = 1f + (Random.nextFloat() * 2f - 1f) * GameConfig.SPAWN_JITTER
                delay((GameConfig.SPAWN_INTERVAL_MS * rank.spawnMult * shiftMods.spawnMult * jitter).toLong())
                val s = _state.value
                if (s.screen != Screen.PLAYING || s.sarjActive) continue
                // Per-tier concurrency cap: beginners juggle few chats; experts handle a full queue.
                if (s.conversations.count { it.status == ConversationStatus.ONGOING } < rank.maxOpen) spawnConversation()
            }
        }
    }

    private fun spawnConversation() {
        val rank = GameConfig.rankForXp(_profile.value.xp)
        val unlockedPersonas = GameConfig.PERSONA_UNLOCK_ORDER.take(rank.personasUnlocked).toSet()
        val unlockedIntents = GameConfig.INTENT_UNLOCK_ORDER.take(rank.intentsUnlocked)
        var spawnedConvo: Conversation? = null
        _state.update { s ->
            // Only customers with an OPEN chat are busy — a resolved/closed chat no longer blocks them.
            val busy = s.conversations.filter { it.status == ConversationStatus.ONGOING }.map { it.customer.id }.toSet()
            val customer = s.roster
                .filter { it.id !in busy && it.persona.archetype in unlockedPersonas }
                .randomOrNull() ?: return@update s
            val intent = unlockedIntents.random()
            val account = s.bank.accounts[customer.accountNumber]
            val targetDate = if (intent == Intent.RESCHEDULE_BOOKING) {
                val current = s.bank.bookings[customer.id]?.date
                ConversationSeeds.rescheduleDates.filter { it != current }.random()
            } else null
            val expected = if (intent == Intent.BALANCE) account?.balance?.toString() else null
            // Transaction-targeting intents: pick exactly one row the customer will name in their opener.
            val targetTxnIndex: Int? = when (intent) {
                Intent.REPORT_FRAUD -> account?.let { CustomerFactory.fraudTxnIndex(it).takeIf { i -> i >= 0 } }
                Intent.DISPUTE_CHARGE -> account?.transactions?.indices
                    ?.filter { !CustomerFactory.isSuspicious(account.transactions[it]) }?.randomOrNull()
                else -> null
            }
            // Safety: a dispute/fraud request must name a real transaction — skip the spawn if we can't.
            if ((intent == Intent.DISPUTE_CHARGE || intent == Intent.REPORT_FRAUD) && targetTxnIndex == null) return@update s
            val targetTxn = targetTxnIndex?.let { account?.transactions?.getOrNull(it) }
            val openingText = ConversationSeeds.opening(
                intent, date = targetDate, merchant = targetTxn?.desc, amount = targetTxn?.let { -it.amount }
            )
            val rep = s.shiftRating
            val repMod = when {
                s.shiftStars.size < 3 -> 1f               // don't let 1-2 early reviews swing arrivals
                rep >= GameConfig.RATING_GOOD -> 1f + GameConfig.REP_PATIENCE_BONUS
                rep <= GameConfig.RATING_BAD -> 1f - GameConfig.REP_PATIENCE_BONUS
                else -> 1f
            }
            val patienceMax = (GameConfig.BASE_PATIENCE_MS * customer.persona.patienceMultiplier * repMod * rank.patienceMult * shiftMods.patienceMult).toLong().coerceAtLeast(8000L)
            // Sensitive card/money actions need an identity check first (a second, tap-based verb).
            val needsVerify = intent == Intent.FREEZE_CARD || intent == Intent.MAKE_PAYMENT || intent == Intent.REPLACE_CARD
            val verifyAnswer = if (needsVerify) customer.cardLast4 else null
            val convo = Conversation(
                id = nextConvoId++,
                customer = customer,
                intent = intent,
                messages = listOf(ChatMessage(nextMsgId++, Sender.CUSTOMER, openingText)),
                expectedValue = expected,
                targetDate = targetDate,
                unread = true,
                patienceMaxMs = patienceMax,
                gate = if (needsVerify) GateKind.VERIFY_ID else GateKind.NONE,
                verifyAnswer = verifyAnswer,
                targetTxnIndex = targetTxnIndex
            )
            spawnedConvo = convo
            s.copy(conversations = s.conversations + convo)
        }
        val c = spawnedConvo ?: return
        // For flavour-only intents, let the on-device LLM voice the opener (bounded, with template fallback).
        if (brain.writesOpeners && !openerGenInFlight && c.intent in OPENER_LLM_INTENTS) launchOpener(c)
    }

    private val OPENER_LLM_INTENTS = setOf(
        Intent.BALANCE, Intent.STATEMENT, Intent.FREEZE_CARD, Intent.COMPLAINT, Intent.REPLACE_CARD
    )

    /** Have the on-device LLM write the customer's star-review text, replacing the template in the ticker. */
    private fun launchReview(reviewId: Long, convo: Conversation, stars: Int) {
        if (!brain.writesOpeners || reviewGenInFlight) return
        reviewGenInFlight = true
        val myEpoch = shiftEpoch
        shiftScope.launch {
            try {
                val gen = runCatching { withTimeoutOrNull(GameConfig.REVIEW_TIMEOUT_MS) { brain.review(convo, stars) } }.getOrNull()
                val clean = gen?.trim()?.takeIf { it.isNotBlank() }
                if (clean != null && shiftEpoch == myEpoch) {
                    _state.update { s ->
                        s.copy(
                            pendingReview = s.pendingReview?.let { if (it.id == reviewId) it.copy(text = clean) else it },
                            reviews = s.reviews.map { if (it.id == reviewId) it.copy(text = clean) else it }
                        )
                    }
                }
            } finally {
                reviewGenInFlight = false
            }
        }
    }

    /** Swap a freshly-spawned chat's template opener for an LLM-voiced one. Bounded by a timeout; on
     *  timeout / blank / a shift change it silently keeps the template — so it can never stall or break. */
    private fun launchOpener(convo: Conversation) {
        openerGenInFlight = true
        val myEpoch = shiftEpoch
        shiftScope.launch {
            try {
                val gen = runCatching { withTimeoutOrNull(GameConfig.OPENING_TIMEOUT_MS) { brain.opening(convo) } }.getOrNull()
                val clean = gen?.trim()?.takeIf { it.isNotBlank() }
                if (clean != null && shiftEpoch == myEpoch) {
                    updateConvo(convo.id) { c ->
                        // Re-check the epoch inside the update: a stale opener must never touch a new shift's chats.
                        if (c.messages.isEmpty() || shiftEpoch != myEpoch) c
                        else c.copy(messages = c.messages.mapIndexed { i, m -> if (i == 0) m.copy(text = clean) else m })
                    }
                }
            } finally {
                openerGenInFlight = false   // always release the guard, even if generation/update throws
            }
        }
    }

    // ---- Patience / impatience (waiting customers, no LLM) ----

    private fun startPatienceLoop() {
        shiftScope.launch {
            while (isActive) {
                delay(GameConfig.PATIENCE_TICK_MS)
                val st = _state.value
                if (st.screen != Screen.PLAYING || st.sarjActive || shiftEnding) continue
                val openId = st.openConversationId
                val abandons = ArrayList<Long>()
                _state.update { s ->
                    s.copy(conversations = s.conversations.map { c ->
                        if (c.status != ConversationStatus.ONGOING) return@map c
                        if (c.id == openId) return@map c   // the chat you're serving is frozen — no drain, no abandon
                        val dec = GameConfig.PATIENCE_TICK_MS.toFloat() / c.patienceMaxMs.coerceAtLeast(1L)
                        val np = (c.patience - dec).coerceAtLeast(0f)
                        if (np <= 0f && c.patience > 0f) abandons.add(c.id)
                        val target = when {
                            np <= GameConfig.NUDGE3_AT -> 3
                            np <= GameConfig.NUDGE2_AT -> 2
                            np <= GameConfig.NUDGE1_AT -> 1
                            else -> 0
                        }
                        if (target > c.nudgeLevel && np > 0f) {
                            val line = PersonaLines.nudge(c.customer.persona.archetype, target)
                            c.copy(patience = np, nudgeLevel = target, unread = true,
                                messages = c.messages + ChatMessage(nextMsgId++, Sender.CUSTOMER, line))
                        } else {
                            c.copy(patience = np)
                        }
                    })
                }
                for (id in abandons) {
                    val c = _state.value.conversations.firstOrNull { it.id == id } ?: continue
                    if (c.status != ConversationStatus.ONGOING) continue
                    updateConvo(id) { it.copy(messages = it.messages + ChatMessage(nextMsgId++, Sender.CUSTOMER, "forget it, i'm done waiting.")) }
                    applyOutcome(id, stars = if (c.customer.persona.harsh) 1 else 2, abandoned = true, byAgent = false)
                }
            }
        }
    }

    // ---- Summon Sarj ----

    fun summonSarj() {
        val s = _state.value
        if (s.sarjActive || s.tokens < shiftMods.sarjCost) return
        _state.update {
            it.copy(tokens = it.tokens - shiftMods.sarjCost, sarjActive = true, sarjUsed = it.sarjUsed + 1, tab = Tab.CHATS, openConversationId = null)
        }
        soundManager.play(Sfx.SARJ_WHOOSH)
        saveProfile { it.copy(tokens = _state.value.tokens) }
        progressObjective(ObjectiveKind.SARJ_1)
        shiftScope.launch {
            // Clear ONLY the chats already in your queue — never conjure new ones.
            while (isActive) {
                val next = _state.value.conversations.firstOrNull { it.status == ConversationStatus.ONGOING } ?: break
                delay(GameConfig.SARJ_RESOLVE_EACH_MS)
                updateConvo(next.id) { it.copy(messages = it.messages + ChatMessage(nextMsgId++, Sender.CUSTOMER, "wow, that was instant — thank you!")) }
                applyOutcome(next.id, stars = 5, abandoned = false, byAgent = false, sarj = true)
            }
            delay(700)
            _state.update { it.copy(sarjActive = false) }
        }
    }

    // ---- helpers ----

    private val empathyWords = listOf(
        "sorry", "apolog", "thank", "please", "understand", "hi ", "hello", "of course",
        "no problem", "happy to", "sure", "right away", "i'll", "let me", "don't worry"
    )

    private fun hasEmpathy(t: String): Boolean {
        val l = " ${t.lowercase()} "
        return empathyWords.any { l.contains(it) }
    }

    private inline fun updateConvo(id: Long, crossinline f: (Conversation) -> Conversation) {
        _state.update { s -> s.copy(conversations = s.conversations.map { if (it.id == id) f(it) else it }) }
    }

    private fun String.normalizedNumber(): String = uppercase().filter { !it.isWhitespace() }

    override fun onCleared() {
        super.onCleared()
        shiftScope.cancel()
        brain.close()
        soundManager.release()
    }
}
