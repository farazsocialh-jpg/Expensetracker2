package com.expensetracker.domain.model

import java.time.LocalDateTime

enum class ExpenseCategory(val displayName: String, val emoji: String) {
    FOOD("Food & Dining", "🍔"),
    TRANSPORT("Transport", "🚗"),
    SHOPPING("Shopping", "🛍️"),
    BILLS("Bills & Utilities", "💡"),
    HEALTH("Health & Medical", "🏥"),
    ENTERTAINMENT("Entertainment", "🎬"),
    EDUCATION("Education", "📚"),
    TRAVEL("Travel", "✈️"),
    GROCERY("Grocery", "🛒"),
    FUEL("Fuel", "⛽"),
    OTHER("Other", "💰")
}

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val category: ExpenseCategory,
    val dateTime: LocalDateTime,
    val balance: Double? = null,
    val rawSms: String? = null,
    val isManual: Boolean = false,
    val note: String = "",
    val sender: String = "",
    val cardNumber: String = "",
    val accountLabel: String = ""
)

data class Budget(
    val id: Long = 0,
    val category: ExpenseCategory,
    val monthlyLimit: Double,
    val month: Int,
    val year: Int
)

data class CategorySummary(
    val category: ExpenseCategory,
    val totalAmount: Double,
    val transactionCount: Int,
    val budgetLimit: Double? = null
)

data class DashboardStats(
    val dailyTotal: Double,
    val weeklyTotal: Double,
    val monthlyTotal: Double,
    val categorySummaries: List<CategorySummary>,
    val recentTransactions: List<Transaction>
)

data class AppSettings(
    val currencySymbol: String = "QAR",
    val trustedSenders: List<String> = listOf("QNB", "DOHA BANK", "CBQ", "MASRAF", "HSBC", "QIIB", "DUKHAN", "AHLIBANK"),
    val debitKeywords: List<String> = listOf("debited", "payment", "purchase", "withdrawn", "charged"),
    val creditKeywords: List<String> = listOf("credited", "received", "refund"),
    val autoScanEnabled: Boolean = true,
    val monthStartDay: Int = 1   // 1-28, day of month your billing cycle starts
)
