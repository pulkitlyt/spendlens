package com.spendlens.app.model

enum class TxnType { DEBIT, CREDIT }

enum class Kind { EXPENSE, INCOME, EXCLUDED }

enum class Category(val label: String, val emoji: String, val color: Long, val kind: Kind) {
    FOOD("Food & Dining", "🍔", 0xFFE4572E, Kind.EXPENSE),
    GROCERIES("Groceries", "🛒", 0xFF4CAF50, Kind.EXPENSE),
    SHOPPING("Shopping", "🛍️", 0xFFAB47BC, Kind.EXPENSE),
    TRANSPORT("Transport & Fuel", "🚕", 0xFFFFA000, Kind.EXPENSE),
    TRAVEL("Travel", "✈️", 0xFF29B6F6, Kind.EXPENSE),
    BILLS("Bills & Recharge", "💡", 0xFFFFD54F, Kind.EXPENSE),
    ENTERTAINMENT("Entertainment", "🎬", 0xFFEC407A, Kind.EXPENSE),
    HEALTH("Health & Fitness", "💊", 0xFF26A69A, Kind.EXPENSE),
    EDUCATION("Education", "📚", 0xFF5C6BC0, Kind.EXPENSE),
    RENT("Rent & Housing", "🏠", 0xFF8D6E63, Kind.EXPENSE),
    EMI("EMI & Loans", "🏦", 0xFF78909C, Kind.EXPENSE),
    INVESTMENT("Investments", "📈", 0xFF00897B, Kind.EXPENSE),
    INSURANCE("Insurance", "🛡️", 0xFF3949AB, Kind.EXPENSE),
    CASH("ATM / Cash", "🏧", 0xFF9CCC65, Kind.EXPENSE),
    TRANSFER("Sent to People", "🔁", 0xFF7E57C2, Kind.EXPENSE),
    OTHER("Other", "📦", 0xFFBDBDBD, Kind.EXPENSE),

    SALARY("Salary", "💰", 0xFF2E7D32, Kind.INCOME),
    REFUND("Refunds & Cashback", "↩️", 0xFF66BB6A, Kind.INCOME),
    INCOME_OTHER("Money Received", "📥", 0xFF81C784, Kind.INCOME),

    CC_BILL("Card Bill Payment (not counted)", "💳", 0xFF90A4AE, Kind.EXCLUDED),
    IGNORED("Ignore / Self Transfer", "🚫", 0xFF9E9E9E, Kind.EXCLUDED);

    /** Expense categories only make sense for debits, income ones only for credits. */
    fun allowedFor(type: TxnType): Boolean =
        kind == Kind.EXCLUDED || (kind == Kind.INCOME) == (type == TxnType.CREDIT)
}

data class Transaction(
    val id: Long,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val amount: Double,
    val type: TxnType,
    val merchant: String,
    val account: String?,
    val category: Category,
)
