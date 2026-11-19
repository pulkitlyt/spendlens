package com.spendlens.app.data

import android.content.Context
import android.provider.Telephony
import com.spendlens.app.model.Category
import com.spendlens.app.model.Transaction
import com.spendlens.app.parse.Categorizer
import com.spendlens.app.parse.SmsParser
import java.time.LocalDate
import java.time.ZoneId

/** User corrections: per-transaction category, and "always use X for this merchant" rules. */
class Overrides(context: Context) {
    private val prefs = context.getSharedPreferences("overrides", Context.MODE_PRIVATE)

    fun forTxn(id: Long): Category? = prefs.getString("t_$id", null)?.toCategory()
    fun forMerchant(merchant: String): Category? = prefs.getString("m_${merchant.key()}", null)?.toCategory()

    fun setTxn(id: Long, c: Category) = prefs.edit().putString("t_$id", c.name).apply()
    fun setMerchant(merchant: String, c: Category) = prefs.edit().putString("m_${merchant.key()}", c.name).apply()

    private fun String.key() = lowercase().trim()
    private fun String.toCategory(): Category? = Category.entries.firstOrNull { it.name == this }
}

class TransactionRepository(private val context: Context) {
    val overrides = Overrides(context)

    /** Reads the SMS inbox for the last [monthsBack] months and returns parsed transactions, newest first. */
    fun load(monthsBack: Long = 12): List<Transaction> {
        val since = LocalDate.now().minusMonths(monthsBack).withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val out = ArrayList<Transaction>()
        val seen = HashSet<String>()
        val projection = arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)

        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            "${Telephony.Sms.DATE} >= ?",
            arrayOf(since.toString()),
            "${Telephony.Sms.DATE} DESC",
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(Telephony.Sms._ID)
            val iAddr = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val iBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val iDate = c.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (c.moveToNext()) {
                val body = c.getString(iBody) ?: continue
                val sender = c.getString(iAddr) ?: ""
                val date = c.getLong(iDate)
                val parsed = SmsParser.parse(sender, body) ?: continue

                // Banks occasionally send the exact same alert twice within a minute or two.
                if (!seen.add("${body.trim()}|${date / 120_000}")) continue

                val id = c.getLong(iId)
                val merchant = parsed.merchant ?: senderLabel(sender)
                val category = overrides.forTxn(id)
                    ?: overrides.forMerchant(merchant)?.takeIf { it.allowedFor(parsed.type) }
                    ?: Categorizer.categorize(parsed.type, parsed.merchant, body)

                out += Transaction(
                    id = id,
                    sender = sender,
                    body = body,
                    timestamp = date,
                    amount = parsed.amount,
                    type = parsed.type,
                    merchant = merchant,
                    account = parsed.account,
                    category = category,
                )
            }
        }
        return out
    }

    /** "AD-HDFCBK-S" -> "HDFCBK" */
    private fun senderLabel(sender: String): String =
        sender.split('-').filter { it.length >= 4 }.maxByOrNull { it.length } ?: sender.ifBlank { "Unknown" }
}
