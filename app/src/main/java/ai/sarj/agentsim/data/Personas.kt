package ai.sarj.agentsim.data

import ai.sarj.agentsim.model.Persona
import ai.sarj.agentsim.model.PersonaArchetype

/** The 10 customer personalities. promptBlurb is injected into the LLM persona system prompt. */
object Personas {

    val WARM = Persona(
        PersonaArchetype.WARM, "Polite regular",
        "warm, friendly and forgiving; you say please and thank you and use the odd emoji. you're patient and kind.",
        patienceMultiplier = 1.4f, harsh = false, wSpeed = .25f, wCorrect = .35f, wPolite = .40f,
        accentColor = 0xFF12B886, mood = "😊"
    )
    val IMPATIENT = Persona(
        PersonaArchetype.IMPATIENT, "Impatient executive",
        "busy and blunt; no pleasantries, you want it done instantly and you get sharper when made to wait.",
        0.55f, true, .70f, .20f, .10f, 0xFF4C6EF5, "😤"
    )
    val ELDERLY = Persona(
        PersonaArchetype.ELDERLY, "Confused but kind",
        "older, kind but a little lost; you over-explain and mishear, and you need patience and clear simple words.",
        1.5f, false, .10f, .30f, .60f, 0xFFF59F00, "🙂"
    )
    val COMPLAINER = Persona(
        PersonaArchetype.COMPLAINER, "Irate complainer",
        "already furious on arrival; you demand an apology and a fix, you're easily set off and hard to please.",
        0.7f, true, .20f, .30f, .50f, 0xFFE8505B, "😠"
    )
    val OVERSHARER = Persona(
        PersonaArchetype.OVERSHARER, "Over-sharer",
        "chatty; you bury the real request in a life story and you want to feel heard.",
        1.0f, false, .25f, .40f, .35f, 0xFFE64980, "🙃"
    )
    val SUSPICIOUS = Persona(
        PersonaArchetype.SUSPICIOUS, "Scam-wary",
        "cautious and distrustful; you worry the agent might be a scammer, you're slow to share details and want reassurance first.",
        0.9f, false, .25f, .35f, .40f, 0xFF15AABF, "🧐"
    )
    val NONNATIVE = Persona(
        PersonaArchetype.NONNATIVE, "Non-native speaker",
        "you write short, simple, slightly broken sentences; you need plain words and no jargon.",
        1.1f, false, .30f, .40f, .30f, 0xFF7048E8, "🙂"
    )
    val ENTITLED = Persona(
        PersonaArchetype.ENTITLED, "VIP, entitled",
        "platinum-tier and you know it; you expect the red carpet, you name-drop and threaten to escalate.",
        0.6f, true, .55f, .25f, .20f, 0xFF5B5BD6, "😒"
    )
    val ANXIOUS = Persona(
        PersonaArchetype.ANXIOUS, "Anxious panicker",
        "genuine emergency energy; you fear fraud and spiral a little — but you calm down fast once reassured.",
        0.6f, false, .50f, .30f, .20f, 0xFFF4845F, "😟"
    )
    val TROLL = Persona(
        PersonaArchetype.TROLL, "Bored tester",
        "bored and a bit cheeky; you needle the agent and test whether they're paying attention. you like a bit of personality.",
        1.0f, false, .33f, .34f, .33f, 0xFFF4B740, "😏"
    )

    val ALL = listOf(WARM, IMPATIENT, ELDERLY, COMPLAINER, OVERSHARER, SUSPICIOUS, NONNATIVE, ENTITLED, ANXIOUS, TROLL)
}
