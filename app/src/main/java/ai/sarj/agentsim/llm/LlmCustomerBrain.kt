package ai.sarj.agentsim.llm

import android.content.Context
import ai.sarj.agentsim.config.GameConfig
import ai.sarj.agentsim.model.Conversation
import ai.sarj.agentsim.model.Intent
import ai.sarj.agentsim.model.Sender
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * On-device customer played by Qwen2.5 (MediaPipe LLM Inference).
 *
 * IMPORTANT: an instruct model's `assistant` role is RLHF-trained to be a helpful agent, so mapping the
 * customer onto assistant turns makes it flip into "the support agent". Instead we use a single
 * **roleplay/script-completion** prompt: the model is told to output ONLY the customer's next line of a
 * transcript. That keeps it firmly in the customer's voice.
 */
class LlmCustomerBrain private constructor(private val llm: LlmInference) : CustomerBrain {

    override val label = "On-device AI"

    // The single LlmInference engine isn't safe for concurrent inference — serialize all calls.
    private val genMutex = Mutex()

    override suspend fun turn(
        conversation: Conversation,
        agentText: String,
        appResolved: Boolean,
        onToken: (String) -> Unit
    ): BrainTurn {
        val prompt = buildPrompt(conversation, appResolved)
        val raw = runCatching { generate(prompt) }.getOrDefault("")
        val base = sanitize(raw, conversation.customer.name).ifBlank { fallback(appResolved) }
        val reply = guardReply(conversation, appResolved, base)   // block role-reversal leaks before streaming
        for (word in reply.split(" ")) {
            onToken("$word ")
            delay(10)
        }
        return BrainTurn(reply, appResolved, if (appResolved) 90 else 55)
    }

    override val writesOpeners = true

    /** Author the customer's first message in-character. Only called for flavour-only intents. */
    override suspend fun opening(conversation: Conversation): String? {
        val c = conversation
        val name = c.customer.name
        val system = buildString {
            append("ROLEPLAY. You voice ONE person: $name — a BANK CUSTOMER opening a support chat for the FIRST time.\n")
            append("You are the CUSTOMER, never the agent. You have no system access and cannot look anything up. You never state a balance or account figure, and you never offer help. You only ASK for what you need.\n")
            append("WHO YOU ARE: ${c.customer.persona.promptBlurb}\n")
            append("WHAT YOU WANT: ${goalLine(c)}\n")
            append("EXAMPLE (copy the role + style, not the words): \"hi, i lost my card — can you freeze it for me please?\"\n")
            append("Write $name's FIRST message ONLY — one short casual WhatsApp-style sentence, first person, in character, clearly saying what you need. Never write 'Agent:'. Never act as support.")
        }
        val user = "Write $name's opening message now:"
        val prompt = "<|im_start|>system\n$system<|im_end|>\n<|im_start|>user\n$user<|im_end|>\n<|im_start|>assistant\n$name:"
        val raw = runCatching { generate(prompt) }.getOrDefault("")
        val clean = sanitize(raw, name).takeIf { it.isNotBlank() } ?: return null
        // Reject any opener that slipped into agent-voice or leaked a figure — keep the clean template.
        if (looksLikeAgent(conversation, clean) ||
            (conversation.intent == Intent.BALANCE && hasFigure(clean))) return null
        return clean
    }

    /** Author the customer's end-of-chat review in their own voice, matching the stars. */
    override suspend fun review(conversation: Conversation, stars: Int): String? {
        val name = conversation.customer.name
        val mood = when {
            stars >= 5 -> "delighted"
            stars == 4 -> "satisfied"
            stars == 3 -> "lukewarm, just okay"
            stars == 2 -> "annoyed"
            else -> "angry and unhappy"
        }
        val system = "ROLEPLAY. You are $name, a bank customer who just finished a support chat and feels $mood " +
            "($stars out of 5 stars). ${conversation.customer.persona.promptBlurb} " +
            "Write ONE very short review of the agent — max 12 words, first person, in your own voice, clearly matching " +
            "$stars/5. No quotes, no star symbols, no numbers, no emojis — just the words."
        val prompt = "<|im_start|>system\n$system<|im_end|>\n<|im_start|>user\nYour review:<|im_end|>\n<|im_start|>assistant\n"
        val raw = runCatching { generate(prompt) }.getOrDefault("")
        return sanitize(raw, name).takeIf { it.isNotBlank() }?.take(120)
    }

    // Phrases only a support AGENT would say. If the model emits any of these, it has flipped roles.
    private val agentTells = listOf(
        "i'll look", "i will look", "let me look", "i'll check", "let me check", "i can check", "i'll get that",
        "i have your", "i've got your", "your balance is", "the balance is", "your current balance", "on my end",
        "i'll freeze", "i'll process", "i'll send", "i've processed", "i've sent", "i've frozen", "i've gone ahead",
        "i'll dispute", "i'll report", "i'll reissue", "i'll arrange", "i'll order", "i can do that for you",
        "i can help", "i'm happy to help", "happy to help", "glad to help", "how can i help", "how may i help",
        "how can i assist", "i can assist", "i'd be happy", "let me assist", "i'm here to help", "feel free to",
        "is there anything else", "anything else i can", "for security", "verify your identity", "to verify you",
        "your account shows", "thanks for reaching out", "thank you for contacting", "thanks for contacting",
        "we apologize", "we appreciate your", "rest assured", "i'll make sure", "i'll go ahead", "you're all set",
        "has been processed", "has been frozen", "has been sent", "has been issued", "your request has", "right away,"
    )

    /** True if the reply reads like the support agent (role-reversal), not the customer. */
    private fun looksLikeAgent(c: Conversation, reply: String): Boolean {
        val l = " ${reply.lowercase()} "
        if (agentTells.any { l.contains(it) }) return true
        // The customer would never address themselves by their own first name.
        val first = c.customer.name.substringBefore(' ').lowercase()
        return first.length >= 3 && (l.contains(" $first!") || l.contains(" $first,") || l.contains(" $first."))
    }

    /** A guaranteed in-character customer line, used whenever the model role-reverses or leaks the answer. */
    private fun inCharacterFallback(c: Conversation, appResolved: Boolean): String {
        if (appResolved) return listOf(
            "oh perfect, thank you so much!", "great, that's sorted — appreciate it!",
            "thanks a lot, that's exactly what I needed!"
        ).random()
        return when (c.intent) {
            Intent.BALANCE -> "sorry, could you look up my balance for me?"
            Intent.STATEMENT -> "could you send my statement over please?"
            Intent.FREEZE_CARD -> "please can you freeze my card?"
            Intent.RESCHEDULE_BOOKING -> "are you able to move my appointment?"
            Intent.MAKE_PAYMENT -> "can you push my payment through?"
            Intent.COMPLAINT -> "I really need this sorted out, please."
            Intent.DISPUTE_CHARGE -> "can you dispute that charge for me?"
            Intent.REPORT_FRAUD -> "please flag that charge as fraud!"
            Intent.REPLACE_CARD -> "can you order me a new card?"
        }
    }

    /**
     * Hard role-lock: if the generated reply reads like the AGENT (offers help, recites a figure, says the
     * customer's own name), replace it with a guaranteed in-character customer line BEFORE streaming. So the
     * player only ever sees the customer — regardless of what the small model does.
     */
    private fun guardReply(c: Conversation, appResolved: Boolean, reply: String): String {
        val balanceLeak = c.intent == Intent.BALANCE && !appResolved && hasFigure(reply)
        return if (balanceLeak || looksLikeAgent(c, reply)) inCharacterFallback(c, appResolved) else reply
    }

    /** A balance customer must never state a money figure. Comma/space-proof: "4,230" / "4 230" / "4230". */
    private fun hasFigure(text: String): Boolean =
        Regex("\\d{3,}").containsMatchIn(text.replace(",", "").replace(" ", ""))

    // ---- Prompt: roleplay/script completion (the model writes only the CUSTOMER's next line) ----

    private fun buildPrompt(c: Conversation, appResolved: Boolean): String {
        val name = c.customer.name
        val system = buildString {
            append("ROLEPLAY. You voice ONE person: $name — a BANK CUSTOMER texting a support agent for help.\n")
            append("HARD RULES:\n")
            append("- You are the CUSTOMER, never the agent. You write ONLY from the customer's side.\n")
            append("- You have NO system access and CANNOT look anything up. You NEVER state a balance, amount, card number or any account detail, and you NEVER offer help or say 'how can I help' — only the agent does that.\n")
            append("- You only ASK for what you need and REACT to what the agent says.\n")
            append("WHO YOU ARE: ${c.customer.persona.promptBlurb}\n")
            append("WHAT YOU WANT: ${goalLine(c)}\n")
            append("PRIVATE CONTEXT (never recite it — only the agent can give it to you): ${groundTruth(c)}\n")
            append("EXAMPLE — copy the ROLE and STYLE only, never the words or any numbers:\n")
            append("Agent: Hello, how can I help today?\n")
            append("$name: hi, i lost my card — can you freeze it for me please?\n")
            append("Agent: Done — your card is frozen now.\n")
            append("$name: oh thank you so much, that's a relief!\n")
            append("Now continue the REAL chat. Reply as $name ONLY — ONE short casual WhatsApp-style message (1-2 sentences), first person, in character. Never write 'Agent:'. Never state any number or figure yourself. Never answer your own question. Never act as support.")
            if (appResolved) append(" The agent just correctly did what you asked — react happily and thank them.")
        }
        val transcript = windowedTranscript(c)
        val user = "REAL chat so far (you are $name, the customer):\n$transcript\n\nWrite $name's next message:"
        return "<|im_start|>system\n$system<|im_end|>\n<|im_start|>user\n$user<|im_end|>\n<|im_start|>assistant\n$name:"
    }

    private fun goalLine(c: Conversation): String = when (c.intent) {
        Intent.BALANCE -> "you want to know your current account balance."
        Intent.STATEMENT -> "you want your latest account statement sent to you."
        Intent.FREEZE_CARD -> "you lost your card and need it frozen right away."
        Intent.RESCHEDULE_BOOKING -> "you want to move your appointment to ${c.targetDate ?: "another day"}."
        Intent.MAKE_PAYMENT -> "your pending payment is stuck and you want it processed."
        Intent.COMPLAINT -> "you were wrongly charged and you're upset about it."
        Intent.DISPUTE_CHARGE -> "you want a specific transaction you didn't make to be disputed."
        Intent.REPORT_FRAUD -> "you spotted a charge you don't recognise and want it reported as fraud."
        Intent.REPLACE_CARD -> "your card is unusable and you want a replacement card issued."
    }

    private fun groundTruth(c: Conversation): String = when (c.intent) {
        Intent.BALANCE -> "you do NOT know your balance and you cannot check it yourself — that is exactly why you are asking. Never state, guess, or invent any balance figure; you are satisfied only once the AGENT tells you a clear specific amount."
        Intent.STATEMENT -> "you're satisfied once the agent confirms the statement has been sent."
        Intent.FREEZE_CARD -> "you're only truly safe once the agent confirms your card is FROZEN. Do not recite your card number."
        Intent.RESCHEDULE_BOOKING -> "you're satisfied once the agent confirms your appointment is moved to ${c.targetDate ?: "the new day"}."
        Intent.MAKE_PAYMENT -> "you're satisfied once the agent confirms the payment has gone through."
        Intent.COMPLAINT -> "you want a genuine apology and to hear it'll be fixed; an empty or robotic reply won't satisfy you."
        Intent.DISPUTE_CHARGE -> "you're satisfied once the agent confirms the charge has been disputed for you."
        Intent.REPORT_FRAUD -> "you're satisfied once the agent confirms the suspicious charge has been flagged as fraud."
        Intent.REPLACE_CARD -> "you're satisfied once the agent confirms a replacement card has been issued."
    }

    /** Transcript as "Agent: …" / "{name}: …" lines, keeping the opener + most recent within budget. */
    private fun windowedTranscript(c: Conversation): String {
        val lines = c.messages.mapNotNull { m ->
            when (m.sender) {
                Sender.CUSTOMER -> "${c.customer.name}: ${m.text}"
                Sender.AGENT -> "Agent: ${m.text}"
                else -> null
            }
        }.filter { it.substringAfter(": ", "").isNotBlank() }
        if (lines.size <= 2) return lines.joinToString("\n")
        val opener = lines.first()
        val budget = GameConfig.MODEL_MAX_TOKENS - 200 - estTokens(opener)
        val kept = ArrayDeque<String>()
        var used = estTokens(opener)
        for (line in lines.drop(1).asReversed()) {
            val t = estTokens(line)
            if (used + t > budget) break
            kept.addFirst(line); used += t
        }
        return (listOf(opener) + kept).joinToString("\n")
    }

    private fun estTokens(s: String): Int = (s.length / 3.5f).toInt() + 4

    /** Strip template tokens, any leading "Name:"/"Agent:" label, and cut at a new speaker line. */
    private fun sanitize(s: String, name: String): String {
        var t = s.substringBefore("<|im_end|>").substringBefore("<|im_start|>")
            .substringBefore("<end_of_turn>").substringBefore("<start_of_turn>")
        t = t.substringBefore("\nAgent:").substringBefore("\n$name:").substringBefore("\nCustomer:")
        t = t.trim().replaceFirst(Regex("^[\\w .'-]{1,24}:\\s*"), "") // drop a leading speaker label
        t = t.replace("Agent:", "").trim()
        return t.take(240).trim()
    }

    private fun fallback(appResolved: Boolean): String =
        if (appResolved) "oh great, thank you so much!" else "okay… are you able to sort this out for me?"

    // ---- Engine ----

    private suspend fun generate(prompt: String): String = genMutex.withLock {
        withContext(Dispatchers.Default) {
            val session = LlmInferenceSession.createFromOptions(
                llm,
                LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTopK(40)
                    .setTopP(0.95f)
                    .setTemperature(0.8f)
                    .build()
            )
            try {
                session.addQueryChunk(prompt)
                session.generateResponse()
            } finally {
                session.close()
            }
        }
    }

    override fun close() {
        runCatching { llm.close() }
    }

    companion object {
        /** Build off the main thread; CPU first (q8 unstable on GPU). Null if it can't load here. */
        suspend fun create(context: Context, modelPath: String): LlmCustomerBrain? =
            withContext(Dispatchers.Default) {
                val ctx = context.applicationContext
                fun engine(backend: LlmInference.Backend): LlmInference =
                    LlmInference.createFromOptions(
                        ctx,
                        LlmInference.LlmInferenceOptions.builder()
                            .setModelPath(modelPath)
                            .setMaxTokens(GameConfig.MODEL_MAX_TOKENS)
                            .setPreferredBackend(backend)
                            .build()
                    )
                val llm = runCatching { engine(LlmInference.Backend.CPU) }.getOrNull()
                    ?: runCatching { engine(LlmInference.Backend.GPU) }.getOrNull()
                    ?: return@withContext null
                LlmCustomerBrain(llm)
            }
    }
}
