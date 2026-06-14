package ai.sarj.agentsim.llm

import ai.sarj.agentsim.model.Conversation
import ai.sarj.agentsim.model.Intent
import kotlinx.coroutines.delay

/**
 * Deterministic fallback "brain": keyword/state heuristics, no model. Fully offline, instant.
 * Streams its reply word-by-word so the UI behaves identically to the LLM brain.
 */
class ScriptedCustomerBrain : CustomerBrain {

    override val label = "Basic"

    override suspend fun turn(
        conversation: Conversation,
        agentText: String,
        appResolved: Boolean,
        onToken: (String) -> Unit
    ): BrainTurn {
        val resolved: Boolean
        val satisfaction: Int
        val reply: String

        when {
            appResolved -> {
                resolved = true; satisfaction = 92
                reply = listOf(
                    "Oh perfect — thank you so much!",
                    "That's exactly what I needed. Appreciate the quick help!",
                    "Great, that's sorted then. Thanks a lot!"
                ).random()
            }
            conversation.intent == Intent.COMPLAINT -> {
                // First reply just vents — the agent must acknowledge AND properly address it to resolve.
                resolved = false; satisfaction = 40
                reply = listOf(
                    "okay, but you're not really fixing the actual problem here.",
                    "i appreciate that, but i need it sorted, not just an apology.",
                    "that's a start, but this keeps happening to me — what will you do?"
                ).random()
            }
            else -> {
                resolved = false; satisfaction = 52
                reply = nudge(conversation.intent)
            }
        }

        for (word in reply.split(" ")) {
            onToken("$word ")
            delay(28)
        }
        return BrainTurn(reply.trim(), resolved, satisfaction)
    }

    private fun nudge(intent: Intent): String = when (intent) {
        Intent.BALANCE -> listOf(
            "I still don't have the number — can you check my account?",
            "Were you able to look up my balance?"
        ).random()
        Intent.STATEMENT -> listOf(
            "I haven't received the statement yet.",
            "Can you send the statement over?"
        ).random()
        Intent.FREEZE_CARD -> listOf(
            "Is the card frozen now? I'm really worried.",
            "Please hurry and freeze the card!"
        ).random()
        Intent.RESCHEDULE_BOOKING -> listOf(
            "So is my appointment moved to the new date?",
            "Did the new date go through?"
        ).random()
        Intent.MAKE_PAYMENT -> listOf(
            "It still shows as pending on my side.",
            "Has the payment gone through yet?"
        ).random()
        Intent.COMPLAINT -> "I'm still waiting for someone to take this seriously."
        Intent.DISPUTE_CHARGE -> listOf(
            "Have you been able to dispute that charge yet?",
            "Is the disputed charge being handled?"
        ).random()
        Intent.REPORT_FRAUD -> listOf(
            "Please tell me that fraud charge is flagged!",
            "Is the fraudulent transaction reported yet?"
        ).random()
        Intent.REPLACE_CARD -> listOf(
            "Is my replacement card on the way?",
            "Did you order me a new card yet?"
        ).random()
    }
}
