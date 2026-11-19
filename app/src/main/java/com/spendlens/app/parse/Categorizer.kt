package com.spendlens.app.parse

import com.spendlens.app.model.Category
import com.spendlens.app.model.TxnType

/**
 * Keyword based categorisation. The merchant name is checked first against every
 * rule; only if nothing matches is the full SMS body checked (with a stricter
 * keyword list, since bodies contain footers like "toll free" or "convert to EMI").
 */
object Categorizer {

    private class Rule(val category: Category, merchantKw: List<String>, bodyKw: List<String> = merchantKw) {
        val merchantRx = build(merchantKw)
        val bodyRx: Regex? = if (bodyKw.isEmpty()) null else build(bodyKw)
    }

    /** Keywords of 4 chars or less must match a whole word; longer ones match a word prefix. */
    private fun build(kws: List<String>): Regex = Regex(
        kws.joinToString("|") { kw ->
            val e = Regex.escape(kw)
            if (kw.length <= 4) "(?<![a-z0-9])$e(?![a-z0-9])" else "(?<![a-z0-9])$e"
        },
        RegexOption.IGNORE_CASE,
    )

    private val transport = listOf(
        "uber", "ola", "olacabs", "ola cabs", "ani technologies", "rapido", "namma yatri", "blusmart",
        "metro", "dmrc", "bmrcl", "fuel", "petrol", "diesel", "hpcl", "bpcl", "iocl", "indian oil",
        "indianoil", "bharat petroleum", "hindustan petroleum", "shell", "nayara", "fastag", "parking",
        "toll", "toll plaza", "yulu", "bounce", "cab", "cabs", "auto", "taxi", "railway", "bus", "ksrtc",
        "msrtc", "bmtc", "ev charging",
    )
    private val education = listOf(
        "school", "college", "university", "institute", "academy", "tuition", "coaching", "udemy",
        "coursera", "byju", "byjus", "unacademy", "vedantu", "upgrad", "simplilearn", "physicswallah",
        "physics wallah", "fees", "fee", "exam", "books", "stationery", "library",
    )

    private val rules = listOf(
        Rule(
            Category.CC_BILL,
            listOf("credit card bill", "credit card payment", "card bill", "cc bill", "cc payment", "cred", "cred club"),
        ),
        Rule(
            Category.CASH,
            listOf("atm", "cash withdrawal", "cash wdl", "atm wdl", "nwd", "cash"),
            listOf("atm", "cash withdrawal", "cash wdl", "atm wdl", "withdrawn at"),
        ),
        Rule(
            Category.INVESTMENT,
            listOf(
                "zerodha", "groww", "upstox", "angel one", "angelone", "kuvera", "indmoney", "paytm money",
                "smallcase", "mutual fund", "sip", "nps", "ppf", "iccl", "nse clearing", "bse ltd",
            ),
        ),
        Rule(
            Category.INSURANCE,
            listOf(
                "insurance", "lic", "policybazaar", "hdfc life", "icici pru", "sbi life", "max life", "tata aia",
                "acko", "star health", "niva bupa", "care health", "bajaj allianz", "go digit",
            ),
        ),
        Rule(
            Category.EMI,
            listOf("emi", "loan", "nach", "ecs", "bajaj fin", "home credit", "finance", "lending", "kreditbee", "moneyview"),
            listOf(
                "loan a/c", "loan account", "loan repayment", "emi of", "emi for", "emi amount", "nach", "ecs",
                "bajaj fin", "home loan", "personal loan", "car loan",
            ),
        ),
        Rule(
            Category.RENT,
            listOf(
                "rent", "house rent", "nobroker", "no broker", "nestaway", "housing", "magicbricks",
                "society", "maintenance", "landlord", "stanza", "zolo", "colive",
            ),
            listOf("rent", "house rent", "nobroker", "nestaway", "society maintenance"),
        ),
        Rule(
            Category.GROCERIES,
            listOf(
                "bigbasket", "big basket", "blinkit", "zepto", "instamart", "swiggy instamart", "dmart", "d mart",
                "avenue supermarts", "grofers", "jiomart", "reliance fresh", "reliance smart", "more retail",
                "more supermarket", "spencer", "natures basket", "nature's basket", "supermarket", "grocery",
                "groceries", "kirana", "milkbasket", "country delight", "licious", "freshtohome", "dunzo",
                "bbnow", "bb now", "star bazaar", "ratnadeep", "vegetable", "fruits", "dairy",
            ),
        ),
        Rule(
            Category.FOOD,
            listOf(
                "swiggy", "zomato", "restaurant", "restro", "cafe", "café", "dominos", "domino's", "pizza",
                "mcdonald", "kfc", "burger", "starbucks", "chaayos", "chai point", "haldiram", "eatfit", "eatsure",
                "faasos", "box8", "behrouz", "barbeque", "bbq", "biryani", "bakery", "bakers", "sweets", "dhaba",
                "kitchen", "food", "foods", "dining", "canteen", "juice", "subway", "wow momo", "momos", "dunkin",
                "baskin", "tea", "coffee", "pub", "brewery", "thali", "mess",
            ),
        ),
        Rule(
            Category.ENTERTAINMENT,
            listOf(
                "netflix", "spotify", "hotstar", "jiohotstar", "disney", "prime video", "primevideo",
                "amazon prime", "youtube", "bookmyshow", "pvr", "inox", "cinepolis", "zee5", "sonyliv",
                "jiocinema", "apple music", "gaana", "wynk", "steam", "playstation", "xbox", "dream11", "mpl",
                "my11circle", "cinema", "movie", "multiplex", "audible", "kindle",
            ),
        ),
        Rule(
            Category.TRAVEL,
            listOf(
                "irctc", "makemytrip", "make my trip", "goibibo", "cleartrip", "yatra", "ixigo", "easemytrip",
                "indigo", "interglobe", "air india", "airindia", "vistara", "spicejet", "akasa", "airline",
                "airways", "airport", "oyo", "airbnb", "booking.com", "agoda", "treebo", "fabhotels", "fab hotels",
                "redbus", "abhibus", "hotel", "hotels", "resort", "holiday", "thomas cook", "zostel",
            ),
        ),
        Rule(
            Category.HEALTH,
            listOf(
                "pharmacy", "pharma", "apollo", "medplus", "1mg", "tata 1mg", "pharmeasy", "netmeds", "hospital",
                "clinic", "medical", "medicals", "diagnostic", "lab", "labs", "thyrocare", "lal path", "dr lal",
                "metropolis", "practo", "cult.fit", "cultfit", "curefit", "gym", "fitness", "healthify", "dental",
                "doctor", "chemist", "health", "wellness", "optical", "eye care",
            ),
        ),
        Rule(Category.TRANSPORT, transport, transport - setOf("auto", "toll", "bus")),
        Rule(
            Category.BILLS,
            listOf(
                "electricity", "bescom", "tata power", "adani elec", "msedcl", "mahadiscom", "bses",
                "torrent power", "tneb", "tangedco", "kseb", "cesc", "uppcl", "power", "airtel", "jio",
                "vodafone", "bsnl", "mtnl", "act fibernet", "hathway", "broadband", "recharge", "postpaid",
                "prepaid", "dth", "tata play", "tatasky", "tata sky", "dish tv", "dishtv", "sun direct", "d2h",
                "indane", "hp gas", "bharat gas", "bharatgas", "mahanagar gas", "igl", "gas", "lpg", "water",
                "municipal", "property tax", "bbps", "bill", "billdesk", "utility", "wifi", "fiber", "fibre",
            ),
        ),
        Rule(Category.EDUCATION, education, education - setOf("fee", "fees", "books", "exam")),
        Rule(
            Category.SHOPPING,
            listOf(
                "amazon", "amzn", "flipkart", "myntra", "ajio", "meesho", "nykaa", "tatacliq", "tata cliq",
                "snapdeal", "lenskart", "decathlon", "ikea", "croma", "reliance digital", "reliance trends",
                "trends", "lifestyle", "westside", "pantaloons", "max fashion", "zara", "h&m", "uniqlo",
                "shoppers stop", "firstcry", "purplle", "mamaearth", "boat", "apple", "samsung", "oneplus",
                "xiaomi", "mall", "store", "stores", "retail", "mart", "fashion", "shop", "shopping",
                "electronics", "jewel", "jewellers", "tanishq", "caratlane", "bewakoof", "souled store",
                "urbanic", "shein",
            ),
        ),
    )

    private val salaryRx = Regex("""\b(salary|sal|payroll|wages)\b""", RegexOption.IGNORE_CASE)
    private val refundRx = Regex("""\b(refund|refunded|reversal|reversed|cashback|cash back|chargeback)\b""", RegexOption.IGNORE_CASE)
    private val transferRx = Regex("""\b(neft|imps|rtgs|p2a|p2p|fund transfer)\b""", RegexOption.IGNORE_CASE)
    private val upiRx = Regex("""\bupi\b""", RegexOption.IGNORE_CASE)
    private val p2mRx = Regex("""\bp2m\b""", RegexOption.IGNORE_CASE)
    private val personLike = Regex("""^[A-Za-z]+(?: [A-Za-z.]+){0,3}$""")
    private val businessWord = Regex(
        """\b(ltd|limited|pvt|private|llp|inc|store|stores|shop|mart|enterprises?|traders?|services?|solutions|industries|agency|agencies|center|centre|hotel|restaurant|foods?|retail|merchant|technologies|tech|india|co)\b""",
        RegexOption.IGNORE_CASE,
    )

    fun categorize(type: TxnType, merchant: String?, body: String): Category {
        if (type == TxnType.CREDIT) {
            return when {
                salaryRx.containsMatchIn(body) -> Category.SALARY
                refundRx.containsMatchIn(body) -> Category.REFUND
                else -> Category.INCOME_OTHER
            }
        }
        if (merchant != null) {
            rules.firstOrNull { it.merchantRx.containsMatchIn(merchant) }?.let { return it.category }
        }
        rules.firstOrNull { it.bodyRx?.containsMatchIn(body) == true }?.let { return it.category }

        if (p2mRx.containsMatchIn(body)) return Category.OTHER
        if (transferRx.containsMatchIn(body)) return Category.TRANSFER
        if (upiRx.containsMatchIn(body)) {
            // UPI to a phone-number VPA, or to something that reads like a person's name.
            if (merchant == null) return Category.TRANSFER
            if (personLike.matches(merchant) && !businessWord.containsMatchIn(merchant)) return Category.TRANSFER
        }
        return Category.OTHER
    }
}
