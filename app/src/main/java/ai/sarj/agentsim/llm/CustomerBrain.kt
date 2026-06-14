package ai.sarj.agentsim.llm

import ai.sarj.agentsim.model.Conversation

/** Result of one customer turn: the full reply text plus the resolution verdict. */
data class BrainTurn(
    val replyText: String,
    val resolved: Boolean,
    val satisfaction: Int
)

/**
 * Plays the customer. Two implementations sit behind this seam:
 *  - [ScriptedCustomerBrain] (always works, offline, no model)
 *  - LlmCustomerBrain (on-device Gemma via MediaPipe) — dropped in later.
 */
interface CustomerBrain {
    /** Short label for the UI, e.g. "Basic" or "On-device AI". */
    val label: String

    /**
     * Produce the customer's reply to [agentText]. [appResolved] is the app's deterministic check
     * (correct value conveyed / tool action performed in the bank). Stream reply deltas via [onToken].
     */
    suspend fun turn(
        conversation: Conversation,
        agentText: String,
        appResolved: Boolean,
        onToken: (String) -> Unit
    ): BrainTurn

    /** True if this brain can author a persona-voiced opening line (LLM only). */
    val writesOpeners: Boolean get() = false

    /**
     * Author the customer's FIRST opening message, in character. Returns null to keep the template.
     * Only ever called for flavour-only intents — resolution-critical openers (dates, transactions)
     * always use the deterministic template, so the game can't be broken by a vague LLM opener.
     */
    suspend fun opening(conversation: Conversation): String? = null

    /** Author the customer's end-of-chat star review text (LLM only). Returns null to keep the template. */
    suspend fun review(conversation: Conversation, stars: Int): String? = null

    fun close() {}
}
