package ai.sarj.agentsim.data

import ai.sarj.agentsim.model.Intent

/** Opening-line templates per intent. {date}/{merchant}/{amount} are filled at spawn time. */
object ConversationSeeds {

    val openings: Map<Intent, List<String>> = mapOf(
        Intent.BALANCE to listOf(
            "Hi! Can you tell me how much is in my account right now?",
            "Hello, I'd like to check my current balance please.",
            "Quick one — what's my available balance at the moment?"
        ),
        Intent.STATEMENT to listOf(
            "Could you send me last month's statement please?",
            "Hi, I need a copy of my latest account statement.",
            "Can you email over my recent statement? I need it for my records."
        ),
        Intent.FREEZE_CARD to listOf(
            "I lost my card! Please freeze it right away.",
            "I think my card was stolen — can you freeze it immediately?",
            "Freeze my card now please, I can't find it anywhere."
        ),
        Intent.RESCHEDULE_BOOKING to listOf(
            "I need to move my appointment to {date}, is that possible?",
            "Can we reschedule my meeting to {date}?",
            "Something came up — can you push my appointment to {date}?"
        ),
        Intent.MAKE_PAYMENT to listOf(
            "My pending payment didn't go through — can you process it?",
            "Please process my scheduled payment, it's still showing as pending.",
            "Can you push my payment through? It's been stuck on pending."
        ),
        Intent.COMPLAINT to listOf(
            "I've been charged twice for the same thing and I'm really frustrated.",
            "Third time I'm contacting you about the same issue. This is unacceptable.",
            "Honestly the service lately has been terrible and I want it sorted."
        ),
        Intent.DISPUTE_CHARGE to listOf(
            "There's a {amount} SAR charge from \"{merchant}\" I didn't authorise — please dispute it.",
            "I was charged {amount} SAR by \"{merchant}\" but I never made that purchase. Can you dispute it?",
            "Please raise a dispute on the \"{merchant}\" charge for {amount} SAR — it's not mine."
        ),
        Intent.REPORT_FRAUD to listOf(
            "I just saw a charge from \"{merchant}\" for {amount} SAR I don't recognise at all — I think it's fraud!",
            "Something's wrong — there's a \"{merchant}\" charge for {amount} SAR I never made. Please flag it as fraud.",
            "Help! A \"{merchant}\" transaction for {amount} SAR looks fraudulent. Can you report it?"
        ),
        Intent.REPLACE_CARD to listOf(
            "My card is damaged and won't work — can you order me a replacement?",
            "I think I've lost my card for good. Please reissue a new one for me.",
            "My card snapped in the ATM. Can you send out a replacement card?"
        )
    )

    val rescheduleDates = listOf("Thu, 12 Jun", "Fri, 13 Jun", "Mon, 16 Jun", "Wed, 18 Jun", "Sun, 22 Jun")

    fun opening(intent: Intent, date: String? = null, merchant: String? = null, amount: Int? = null): String {
        var s = openings.getValue(intent).random()
        if (date != null) s = s.replace("{date}", date)
        if (merchant != null) s = s.replace("{merchant}", merchant)
        if (amount != null) s = s.replace("{amount}", "%,d".format(amount))
        return s
    }
}
