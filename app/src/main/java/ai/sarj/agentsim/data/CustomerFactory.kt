package ai.sarj.agentsim.data

import ai.sarj.agentsim.model.Account
import ai.sarj.agentsim.model.Bank
import ai.sarj.agentsim.model.Booking
import ai.sarj.agentsim.model.Card
import ai.sarj.agentsim.model.CardStatus
import ai.sarj.agentsim.model.Customer
import ai.sarj.agentsim.model.Payment
import ai.sarj.agentsim.model.Txn
import kotlin.random.Random

/**
 * Procedurally generates a FRESH roster of customers + a matching bank every shift.
 *
 * The whole game is "programmatically impossible to break from bad LLM output" because everything the
 * resolution logic checks — account numbers, balances, card endings, the planted fraud transaction —
 * is generated HERE in deterministic code and merely *injected* into the LLM's prompt as flavour.
 */
object CustomerFactory {

    private val firstNames = listOf(
        "Omar", "Sara", "Daniel", "Aisha", "Khalid", "Maya", "George", "Lena", "Wei", "Nadia",
        "Yusuf", "Fatima", "Liam", "Priya", "Hassan", "Mariam", "Noah", "Sofia", "Ahmed", "Layla",
        "Ethan", "Ananya", "Tariq", "Hana", "Lucas", "Mei", "Ibrahim", "Zara", "Diego", "Yuki",
        "Amara", "Samuel", "Ingrid", "Rahul", "Nour", "Marco", "Leila", "Kenji", "Fatou", "Adam",
        "Salma", "Oliver", "Reem", "Jin", "Carlos", "Aaliyah", "Faisal", "Elena", "Bushra", "Hugo"
    )
    private val lastNames = listOf(
        "Haddad", "Nasser", "Reed", "Karimi", "Otaibi", "Lopez", "Park", "Vogel", "Al-Fares", "Khan",
        "Ali", "Smith", "Patel", "Hassan", "Tanaka", "Muller", "Costa", "Ibrahim", "Said", "Johnson",
        "Rahman", "Mansour", "Nguyen", "Silva", "Okafor", "Yousef", "Brown", "Sharma", "Saleh", "Kim",
        "Rossi", "Farah", "Dubois", "Mendes", "Zhang", "Qureshi", "Aziz", "Eriksson", "Baig", "Romero"
    )

    private val spendMerchants = listOf(
        "Grocery Mart", "Corner Cafe", "Fuel Station", "Pharmacy Plus", "Online Store", "Restaurant",
        "Ride Share", "Phone Top-up", "Streaming Sub", "Bookshop", "Electronics Hub", "Clothing Outlet",
        "Supermarket", "Bakery", "Hardware Store", "Coffee House", "Cinema", "Home Delivery"
    )
    private val incomeLabels = listOf("Salary", "Transfer In", "Refund", "Pension", "Dividend", "Remittance", "Bonus")

    /** The planted "fraud" transactions — exactly one of these per account; the REPORT_FRAUD target. */
    val SUSPICIOUS_MERCHANTS = listOf(
        "Unknown POS — Overseas", "Crypto-X Online", "Unrecognised Charge", "Foreign ATM Withdrawal",
        "QuickCash Ltd", "Lucky Draw Intl", "Gift-Card Reseller", "Anonymous Wallet"
    )

    private val bookingTypes = listOf(
        "Advisor call", "Loan review", "Branch visit", "Mortgage consult",
        "Investment review", "Account opening", "Card pickup", "Wealth review"
    )
    private val payees = listOf(
        "Landlord", "Utility Company", "Phone Provider", "Insurance Co.", "Tuition Office",
        "Gym Membership", "Electric Company", "Travel Agency", "Charity", "Internet Provider"
    )

    /** Generate `perArchetype` customers for each of the 10 personas + a bank that matches them. */
    fun generate(seed: Long, perArchetype: Int = 2): Pair<List<Customer>, Bank> {
        val rnd = Random(seed)
        val customers = ArrayList<Customer>()
        val usedAccounts = HashSet<String>()
        val usedLast4 = HashSet<String>()
        var idx = 0
        for (persona in Personas.ALL) {
            repeat(perArchetype) {
                val name = "${firstNames.random(rnd)} ${lastNames.random(rnd)}"
                customers.add(
                    Customer(
                        id = "c${idx++}",
                        name = name,
                        accountNumber = uniqueAccount(rnd, usedAccounts),
                        cardNumber = uniqueCard(rnd, usedLast4),
                        phone = randomPhone(rnd),
                        persona = persona
                    )
                )
            }
        }
        return customers to buildBank(customers, rnd)
    }

    private fun uniqueAccount(rnd: Random, used: HashSet<String>): String {
        while (true) {
            val acc = "SA" + (1..12).joinToString("") { rnd.nextInt(10).toString() }
            if (used.add(acc)) return acc
        }
    }

    private fun uniqueCard(rnd: Random, usedLast4: HashSet<String>): String {
        val prefix = if (rnd.nextBoolean()) "4" else "5"   // Visa / Mastercard-ish
        while (true) {
            val card = prefix + (1..15).joinToString("") { rnd.nextInt(10).toString() }
            if (usedLast4.add(card.takeLast(4))) return card
        }
    }

    private fun randomPhone(rnd: Random): String =
        "+966 5" + (10_000_000 + rnd.nextInt(89_999_999)).toString()

    private fun buildBank(customers: List<Customer>, rnd: Random): Bank {
        val accounts = HashMap<String, Account>()
        val cards = HashMap<String, Card>()
        val bookings = HashMap<String, Booking>()
        val payments = HashMap<String, Payment>()
        for (c in customers) {
            accounts[c.accountNumber] = Account(c.accountNumber, c.name, 500 + rnd.nextInt(200_000), transactions = buildTxns(rnd))
            cards[c.cardNumber] = Card(c.cardNumber, c.name, CardStatus.ACTIVE)
            bookings[c.id] = Booking(c.id, bookingTypes.random(rnd), ConversationSeeds.rescheduleDates.random(rnd))
            payments["p_${c.id}"] = Payment("p_${c.id}", c.id, payees.random(rnd), 50 + rnd.nextInt(5000))
        }
        return Bank(accounts, cards, bookings, payments)
    }

    private fun buildTxns(rnd: Random): List<Txn> {
        val n = 5 + rnd.nextInt(3)            // 5..7 rows total
        val list = ArrayList<Txn>()
        list.add(Txn(incomeLabels.random(rnd), 1000 + rnd.nextInt(15_000)))      // one income
        repeat(n - 2) { list.add(Txn(spendMerchants.random(rnd), -(20 + rnd.nextInt(800)))) }  // normal spends
        list.add(Txn(SUSPICIOUS_MERCHANTS.random(rnd), -(400 + rnd.nextInt(4000))))   // exactly one planted fraud
        list.shuffle(rnd)
        return list
    }

    /** Index of the planted suspicious transaction in an account (REPORT_FRAUD target), or -1. */
    fun fraudTxnIndex(account: Account): Int =
        account.transactions.indexOfFirst { it.desc in SUSPICIOUS_MERCHANTS }

    fun isSuspicious(txn: Txn): Boolean = txn.desc in SUSPICIOUS_MERCHANTS
}
