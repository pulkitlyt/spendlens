package com.spendlens.app.parse

import com.spendlens.app.model.TxnType

data class ParsedSms(
    val amount: Double,
    val type: TxnType,
    val merchant: String?,
    val account: String?,
)

/**
 * Turns a bank / card / UPI alert SMS into a transaction. Pure Kotlin (no Android
 * classes) so it can be unit tested on the JVM.
 */
object SmsParser {
    private val IC = RegexOption.IGNORE_CASE

    // ---- filters: things that look like money but are not a completed transaction ----
    private val personalSender = Regex("""^\+?[0-9 ]{10,}$""")
    private val otp = Regex("""\b(otp|one[\s-]?time[\s-]?password|verification code|passcode)\b""", IC)
    private val notYet = Regex(
        """\b(will be (?:debited|credited|deducted|charged)|is due|due (?:on|date|by)|has requested|collect request|requested (?:money|rs|inr)|payment request|min(?:imum)?\.? amo?u?n?t due|total amount due|bill (?:is )?generated|statement)\b""",
        IC,
    )
    private val failed = Regex("""\b(failed|declined|unsuccessful|not successful|could not be|insufficient)\b""", IC)
    private val promo = Regex(
        """\b(pre-?approved|offers?|apply now|eligible|congratulations|win|voucher|coupon|discount|hurry|limited period|upgrade)\b""",
        IC,
    )
    private val strongTxn = Regex("""\b(debited|credited|spent|withdrawn|deducted)\b""", IC)
    private val ccAckReceived = Regex("""\b(received|credited|thank)""", IC)
    private val ccAckBlock = Regex("""\b(spent|debited|used|withdrawn|using)\b""", IC)

    // ---- amount ----
    private val currencyAmount = Regex("""(?<![a-z])(?:rs\.?|inr|₹)\s*([0-9][0-9,]*(?:\.[0-9]+)?)""", IC)
    private val verbAmount = Regex(
        """\b(?:debited|credited)\s+(?:by|with|for)\s+(?:rs\.?|inr|₹)?\s*([0-9][0-9,]*(?:\.[0-9]+)?)""",
        IC,
    )
    private val balanceHint = Regex("""(bal|balance|lmt|limit|available|avl|avbl|outstanding|o/s)[^0-9]{0,6}$""", IC)

    // ---- direction ----
    private val cardWords = Regex("""\b(credit|debit)\s*card\b""", IC)
    private val debitKw = Regex(
        """\b(debited|debit|spent|paid|sent|withdrawn|withdrawal|purchase|purchased|deducted|charged|used|using)\b""",
        IC,
    )
    private val creditKw = Regex(
        """\b(credited|credit|received|deposited|refund|refunded|reversed|reversal|cashback)\b""",
        IC,
    )

    // ---- account ----
    private val accountRx = Regex(
        """(?:(?<![a-z0-9])(?:x+|\*+)\s?|ending\s*(?:with\s*|in\s*)?)([0-9]{3,6})(?![0-9])""",
        IC,
    )

    // ---- merchant ----
    private const val STOP =
        """(?=\s+(?:on|via|using|ref|refno|txn|for|with|avl|avbl|from|upi|imps|neft|dated|is|has|was|not)\b|\s*[,;(\[]|\.\s|\.$|\s+-|\n|$)"""
    private val upiPath = Regex("""UPI/(?:P2[AMP]/)?[0-9]+/([^/\n]{2,40}?)(?=/|\s+not\b|\n|\.\s|$)""", IC)
    private val atRx = Regex("""\bat\s+(.{2,60}?)$STOP""", IC)
    private val toRx = Regex(
        """\b(?:trf to|transferred to|paid to|sent to|to)\s+(?:vpa\s+|upi id\s*:?\s*)?(.{2,60}?)$STOP""",
        IC,
    )
    private val infoRx = Regex("""\binfo\s*[:\-]\s*(.{2,60}?)$STOP""", IC)
    private val towardsRx = Regex("""\btowards\s+(.{2,60}?)$STOP""", IC)
    private val onRx = Regex("""\bon\s+(.{2,60}?)$STOP""", IC)
    private val fromRx = Regex("""\b(?:by|from)\s+(?:vpa\s+)?(.{2,60}?)$STOP""", IC)
    private val vpaRx = Regex("""\b([a-z0-9][a-z0-9._\-]+)@[a-z][a-z0-9]+\b""", IC)

    private val rejectStart = Regex(
        """^(your|a/c|ac|acct|account|you|the|card|beneficiary|linked|mobile|registered|self|dispute|report|block|call|sms|know|check|stop|register|unblock|raise|contact|visit|avail|claim|convert|get|pay|activate|login|download|update|view|cancel|date|bank|us|our|customer|dear|rs|inr|jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec|mon|tue|wed|thu|fri|sat|sun)\b""",
        IC,
    )
    private val rejectAnywhere = Regex("""\b(a/c|acct|account|card|avl|bal|balance|helpline|customer care)\b""", IC)
    private val dateLike = Regex(
        """\b\d{1,2}[-/ ]?(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*[-/ ]?\d{2,4}\b|\b\d{1,4}[-/.]\d{1,2}[-/.]\d{1,4}\b""",
        IC,
    )
    private val prefixes = Regex("""^(?:vpa|upi|upi id|imps|neft|rtgs|nach|ach|pos|ecom|bil|onl|mb|ib)[\s/:*\-]+""", IC)
    private val acronyms = setOf("ATM", "LIC", "IRCTC", "KFC", "PVR", "HP", "IOCL", "HPCL", "BPCL", "DTH", "SBI", "HDFC", "ICICI", "LLP", "BSNL", "ACT", "OYO")

    fun parse(sender: String, body: String): ParsedSms? {
        if (personalSender.matches(sender.trim())) return null
        if (otp.containsMatchIn(body) || notYet.containsMatchIn(body) || failed.containsMatchIn(body)) return null
        if (promo.containsMatchIn(body) && !strongTxn.containsMatchIn(body)) return null
        val lower = body.lowercase()
        // "Payment of Rs X received on your credit card" — that money already left via a bank debit SMS.
        if ("card" in lower && "payment" in lower &&
            ccAckReceived.containsMatchIn(body) && !ccAckBlock.containsMatchIn(body)
        ) return null

        val amount = findAmount(body) ?: return null
        val type = findType(body) ?: return null
        return ParsedSms(
            amount = amount,
            type = type,
            merchant = findMerchant(body, type),
            account = accountRx.find(body)?.groupValues?.get(1),
        )
    }

    private fun findAmount(body: String): Double? {
        for (m in currencyAmount.findAll(body)) {
            val before = body.substring(maxOf(0, m.range.first - 20), m.range.first)
            if (balanceHint.containsMatchIn(before)) continue
            val v = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: continue
            if (v > 0) return v
        }
        return verbAmount.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()?.takeIf { it > 0 }
    }

    private fun findType(body: String): TxnType? {
        val norm = cardWords.replace(body, "card")
        val d = debitKw.find(norm)?.range?.first
        val c = creditKw.find(norm)?.range?.first
        return when {
            d == null && c == null -> null
            c == null -> TxnType.DEBIT
            d == null -> TxnType.CREDIT
            d < c -> TxnType.DEBIT
            else -> TxnType.CREDIT
        }
    }

    private fun findMerchant(body: String, type: TxnType): String? {
        upiPath.find(body)?.let { m -> clean(m.groupValues[1])?.let { return it } }
        val order = if (type == TxnType.DEBIT) {
            listOf(atRx, toRx, infoRx, towardsRx, onRx)
        } else {
            listOf(fromRx, infoRx, atRx)
        }
        for (rx in order) {
            for (m in rx.findAll(body)) {
                clean(m.groupValues[1])?.let { return it }
            }
        }
        vpaRx.find(body)?.let { m -> clean(m.value)?.let { return it } }
        return null
    }

    private fun clean(raw: String): String? {
        var s = raw.trim()
        if (rejectStart.containsMatchIn(s) || dateLike.containsMatchIn(s)) return null
        if ('@' in s) s = s.substringBefore('@').replace(Regex("""[._\-]+"""), " ")
        while (true) {
            val n = prefixes.replace(s, "")
            if (n == s) break
            s = n
        }
        s = s.replace(Regex("""[/_*]+"""), " ")
            .replace(Regex("""\b\w*[0-9]{4,}\w*\b"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '.', '-', ':', ',')
        if (s.count { it.isLetter() } < 3) return null
        if (rejectStart.containsMatchIn(s) || rejectAnywhere.containsMatchIn(s)) return null
        if (s.length > 32) s = s.take(32).trimEnd()
        return prettify(s)
    }

    private fun prettify(s: String): String {
        if (s != s.uppercase() && s != s.lowercase()) return s
        return s.split(' ').joinToString(" ") { w ->
            if (w.uppercase() in acronyms) w.uppercase()
            else w.lowercase().replaceFirstChar { it.titlecase() }
        }
    }
}
