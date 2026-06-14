package ai.sarj.agentsim.data

import ai.sarj.agentsim.model.PersonaArchetype
import ai.sarj.agentsim.model.PersonaArchetype.ANXIOUS
import ai.sarj.agentsim.model.PersonaArchetype.COMPLAINER
import ai.sarj.agentsim.model.PersonaArchetype.ELDERLY
import ai.sarj.agentsim.model.PersonaArchetype.ENTITLED
import ai.sarj.agentsim.model.PersonaArchetype.IMPATIENT
import ai.sarj.agentsim.model.PersonaArchetype.NONNATIVE
import ai.sarj.agentsim.model.PersonaArchetype.OVERSHARER
import ai.sarj.agentsim.model.PersonaArchetype.SUSPICIOUS
import ai.sarj.agentsim.model.PersonaArchetype.TROLL
import ai.sarj.agentsim.model.PersonaArchetype.WARM

/**
 * Persona-templated text for WAITING customers (proactive nudges) and end-of-chat reviews.
 * These are pure templates — zero LLM calls — so a queue of impatient customers stays instant.
 */
object PersonaLines {

    enum class Bucket { GREAT, GOOD, OK, BAD, ABANDON }

    fun bucketFor(stars: Int, abandoned: Boolean): Bucket = when {
        abandoned -> Bucket.ABANDON
        stars >= 5 -> Bucket.GREAT
        stars == 4 -> Bucket.GOOD
        stars == 3 -> Bucket.OK
        else -> Bucket.BAD
    }

    /** level is 1..3 (nudge escalation). */
    fun nudge(archetype: PersonaArchetype, level: Int): String =
        (nudges[archetype] ?: nudges.getValue(WARM)).getOrElse(level - 1) { listOf("hello? any update?") }.random()

    fun review(archetype: PersonaArchetype, bucket: Bucket): String =
        (reviews[archetype]?.get(bucket) ?: DEFAULT_REVIEWS.getValue(bucket)).random()

    private val nudges: Map<PersonaArchetype, List<List<String>>> = mapOf(
        WARM to listOf(
            listOf("hi, still there? 😊", "no rush, just checking in!"),
            listOf("sorry to bug you — any update?", "whenever you get a sec 🙏"),
            listOf("i've been waiting a little while now 😕", "could someone help me please?")
        ),
        IMPATIENT to listOf(
            listOf("hello? you there?", "any update?"),
            listOf("why is this taking so long", "i don't have all day here"),
            listOf("seriously? still waiting.", "this is ridiculous.")
        ),
        ELDERLY to listOf(
            listOf("hello dear, are you still there?", "did my message go through?"),
            listOf("i'm not sure this is working…", "is someone able to help me?"),
            listOf("oh dear, i've waited a while", "hello? anyone there?")
        ),
        COMPLAINER to listOf(
            listOf("are you ignoring me too now?", "hello??"),
            listOf("unbelievable. still waiting.", "this is exactly the problem."),
            listOf("i want a manager. now.", "i'm closing my account over this.")
        ),
        OVERSHARER to listOf(
            listOf("anyway, you still there? 😅", "oh, did you see my message?"),
            listOf("so should i keep waiting or…?", "hellooo? 🙂"),
            listOf("i've got things to do you know!", "still here, still waiting…")
        ),
        SUSPICIOUS to listOf(
            listOf("are you a real person?", "is anyone actually there?"),
            listOf("this delay is making me nervous", "why is no one responding?"),
            listOf("i knew this felt off…", "i'm not waiting much longer.")
        ),
        NONNATIVE to listOf(
            listOf("hello? you there?", "please, you help me?"),
            listOf("still i wait. please answer", "is problem? please reply"),
            listOf("long time waiting. please", "hello?? please help")
        ),
        ENTITLED to listOf(
            listOf("i expect a faster response.", "do you know who i am?"),
            listOf("this is not platinum service.", "i will be escalating this."),
            listOf("absolutely unacceptable.", "your manager will hear about this.")
        ),
        ANXIOUS to listOf(
            listOf("is everything ok?", "did my message send??"),
            listOf("i'm getting really worried here", "please tell me it's being handled"),
            listOf("i'm panicking, please respond 😣", "is anyone there?!")
        ),
        TROLL to listOf(
            listOf("lol did you fall asleep?", "tap tap, anyone home?"),
            listOf("bot must be buffering 🤖", "c'mon, i'm bored over here"),
            listOf("guess i'll wait forever then", "wow, great service 👏")
        )
    )

    private val DEFAULT_REVIEWS: Map<Bucket, List<String>> = mapOf(
        Bucket.GREAT to listOf("fast and got it right — thank you!", "exactly what i needed, great help", "sorted in no time, appreciate it"),
        Bucket.GOOD to listOf("got there in the end, thanks", "decent help, did the job", "thanks, that worked"),
        Bucket.OK to listOf("took a bit, but ok i guess", "got my answer eventually", "fine, i suppose"),
        Bucket.BAD to listOf("that was slow and confusing", "not great, honestly", "expected better than that"),
        Bucket.ABANDON to listOf("gave up waiting — terrible", "no one helped me at all", "left me hanging, awful")
    )

    private val reviews: Map<PersonaArchetype, Map<Bucket, List<String>>> = mapOf(
        WARM to mapOf(
            Bucket.GREAT to listOf("so quick and kind, thank you! 😊", "lovely service, really appreciate it"),
            Bucket.BAD to listOf("a bit slow, but i know you're busy 😕"),
            Bucket.ABANDON to listOf("oh no one came… maybe next time 😔")
        ),
        IMPATIENT to mapOf(
            Bucket.GREAT to listOf("finally, fast service. good.", "quick. that's all i ask."),
            Bucket.GOOD to listOf("acceptable. could be faster."),
            Bucket.BAD to listOf("far too slow. waste of my time.", "two stars. you were dragging."),
            Bucket.ABANDON to listOf("i left. you were useless.", "zero urgency. zero stars.")
        ),
        COMPLAINER to mapOf(
            Bucket.GREAT to listOf("okay… you actually fixed it. fine.", "about time someone helped."),
            Bucket.OK to listOf("barely acceptable.", "still not impressed."),
            Bucket.BAD to listOf("typical. useless.", "worst service ever. one star."),
            Bucket.ABANDON to listOf("ignored me completely. disgraceful.", "i'm closing my account.")
        ),
        ENTITLED to mapOf(
            Bucket.GREAT to listOf("acceptable for someone of my status.", "good. as it should be."),
            Bucket.BAD to listOf("not the platinum treatment i expect.", "your manager will hear about this."),
            Bucket.ABANDON to listOf("kept a VIP waiting? appalling.", "escalating this immediately.")
        ),
        TROLL to mapOf(
            Bucket.GREAT to listOf("ok you actually got jokes AND skills 👏", "not bad, human. 5 stars 🤖"),
            Bucket.OK to listOf("mid. but you tried 😏"),
            Bucket.BAD to listOf("robotic and slow, yikes", "1 star, no rizz"),
            Bucket.ABANDON to listOf("ghosted me lmao. brutal.", "speedrun to one star 🐌")
        ),
        ANXIOUS to mapOf(
            Bucket.GREAT to listOf("oh thank you, you calmed me right down 🙏", "so relieved, thank you!!"),
            Bucket.BAD to listOf("that made me more anxious honestly 😟"),
            Bucket.ABANDON to listOf("no one came and i panicked 😭")
        )
    )
}
