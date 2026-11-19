package com.spendlens.app

import com.spendlens.app.model.Category
import com.spendlens.app.model.TxnType
import com.spendlens.app.parse.Categorizer
import com.spendlens.app.parse.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SmsParserTest {

    private fun check(
        sender: String,
        body: String,
        amount: Double,
        type: TxnType,
        category: Category,
        merchant: String? = null,
        account: String? = null,
    ) {
        val p = SmsParser.parse(sender, body)
        assertNotNull("should parse: $body", p)
        p!!
        assertEquals(amount, p.amount, 0.001)
        assertEquals(type, p.type)
        if (merchant != null) assertEquals(merchant, p.merchant)
        if (account != null) assertEquals(account, p.account)
        assertEquals(category, Categorizer.categorize(p.type, p.merchant, body))
    }

    @Test fun upiToVpa() = check(
        "VM-HDFCBK",
        "Rs.250.00 debited from A/c XX1234 on 12-11-26 to VPA swiggy@icici (UPI Ref No 432112345678). Not you? Call 18001234",
        250.0, TxnType.DEBIT, Category.FOOD, merchant = "Swiggy", account = "1234",
    )

    @Test fun cardSpendOnMerchant() = check(
        "JD-ICICIT",
        "INR 1,250.00 spent using ICICI Bank Card XX1234 on 10-Nov-26 on AMAZON PAY. Avl Limit: INR 50,000.00. To dispute call 18001080",
        1250.0, TxnType.DEBIT, Category.SHOPPING, merchant = "Amazon Pay", account = "1234",
    )

    @Test fun sbiStyleNoCurrency() = check(
        "AD-SBIUPI",
        "Dear UPI user A/C X1234 debited by 120.0 on date 10Nov26 trf to ZOMATO Refno 612345678901. If not u? call 1800111109. -SBI",
        120.0, TxnType.DEBIT, Category.FOOD, merchant = "Zomato", account = "1234",
    )

    @Test fun creditCardThankYou() = check(
        "VM-HDFCBK",
        "Thank you for using your HDFC Bank Credit Card ending 1234 for Rs 450.00 at SWIGGY on 2026-11-10 12:00:00",
        450.0, TxnType.DEBIT, Category.FOOD, merchant = "Swiggy", account = "1234",
    )

    @Test fun axisUpiPath() = check(
        "AX-AXISBK",
        "INR 500.00 debited\nA/c no. XX1234\n10-11-26, 12:30:00\nUPI/P2M/432112345678/BLINKIT\nNot you? SMS BLOCKUPI Cust ID to 919951860002\nAxis Bank",
        500.0, TxnType.DEBIT, Category.GROCERIES, merchant = "Blinkit",
    )

    @Test fun atmWithdrawal() = check(
        "AD-SBIINB",
        "Rs.2000.00 withdrawn at ATM S1AW000123 on 10-11-26 from A/c XX1234. Avl Bal Rs 5000",
        2000.0, TxnType.DEBIT, Category.CASH,
    )

    @Test fun salaryCredit() = check(
        "VM-HDFCBK",
        "Rs.50,000.00 credited to your A/c XX1234 on 01-11-26 by NEFT-ACME CORP SALARY NOV. Avl Bal Rs.75,000.00",
        50000.0, TxnType.CREDIT, Category.SALARY,
    )

    @Test fun receivedFromPerson() = check(
        "BZ-KOTAKB",
        "Received Rs.500.00 in your Kotak Bank AC X1234 from rahul.sharma@okaxis on 10-11-26.UPI Ref:123456789012.",
        500.0, TxnType.CREDIT, Category.INCOME_OTHER, merchant = "Rahul Sharma",
    )

    @Test fun refund() = check(
        "VM-HDFCBK",
        "Refund of Rs 299.00 from MYNTRA has been credited to your HDFC Bank Card XX1234",
        299.0, TxnType.CREDIT, Category.REFUND,
    )

    @Test fun ignoresOtp() =
        assertNull(SmsParser.parse("VM-AMAZON", "123456 is your OTP for txn of Rs 500.00 at AMAZON. Do not share it with anyone."))

    @Test fun ignoresPromo() =
        assertNull(SmsParser.parse("VM-HDFCBK", "Get a pre-approved Personal Loan of up to Rs 5,00,000. Apply now: http://x.co/abc"))

    @Test fun ignoresPersonalNumbers() =
        assertNull(SmsParser.parse("+919876543210", "Sent Rs 500 to you"))

    @Test fun ignoresCollectRequests() =
        assertNull(SmsParser.parse("VM-PAYTM", "ZOMATO has requested money of Rs 300.00 from you. Approve on your UPI app"))

    @Test fun ignoresCardPaymentAcknowledgement() =
        assertNull(SmsParser.parse("VM-ICICIB", "Payment of Rs 5,000 received on your Credit Card XX1234. Thank you."))
}
