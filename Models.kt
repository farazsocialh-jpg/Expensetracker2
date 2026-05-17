package com.expensetracker.domain.model

import java.time.LocalDateTime
import java.util.UUID

enum class ExpenseCategory(val displayName: String, val emoji: String, val color: Long) {
    FOOD("Food & Dining",        "🍔", 0xFFFF6B35),
    GROCERY("Grocery",           "🛒", 0xFF4CAF50),
    TRANSPORT("Transport",       "🚗", 0xFF2196F3),
    FUEL("Fuel",                 "⛽", 0xFFFF9800),
    SHOPPING("Shopping",         "🛍️", 0xFFE91E63),
    BILLS("Bills & Utilities",   "💡", 0xFF9C27B0),
    HEALTH("Health & Medical",   "🏥", 0xFFF44336),
    ENTERTAINMENT("Entertainment","🎬",0xFF00BCD4),
    EDUCATION("Education",       "📚", 0xFF3F51B5),
    TRAVEL("Travel",             "✈️", 0xFF009688),
    SUBSCRIPTION("Subscriptions","🔄", 0xFF607D8B),
    SALARY("Salary / Income",    "💼", 0xFF43A047),
    TRANSFER("Transfer",         "↔️", 0xFF90A4AE),
    OTHER("Other",               "💰", 0xFF78909C)
}

enum class PaymentMethod { DEBIT_CARD, CREDIT_CARD, CASH, BANK_TRANSFER, WALLET, OTHER }
enum class TransactionType { DEBIT, CREDIT, TRANSFER }
enum class RecategorizeScope { ONCE, ALL_SIMILAR, FUTURE_ONLY }

data class Transaction(
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val amount: Double,
    val currency: String = "QAR",
    val merchant: String,
    val normalizedMerchant: String = "",
    val category: ExpenseCategory,
    val subcategory: String = "",
    val dateTime: LocalDateTime,
    val bankName: String = "",
    val accountLast4: String = "",
    val accountLabel: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.DEBIT_CARD,
    val transactionType: TransactionType = TransactionType.DEBIT,
    val balance: Double? = null,
    val rawSms: String? = null,
    val isManual: Boolean = false,
    val isExcluded: Boolean = false,
    val isRecurring: Boolean = false,
    val isTransfer: Boolean = false,
    val isRefund: Boolean = false,
    val note: String = "",
    val tags: List<String> = emptyList(),
    val confidenceScore: Float = 1.0f,
    val userEdited: Boolean = false,
    val sender: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

data class MerchantRule(
    val id: Long = 0,
    val pattern: String,
    val displayName: String,
    val category: ExpenseCategory,
    val applyToFuture: Boolean = true,
    val applyToPast: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class Budget(
    val id: Long = 0,
    val category: ExpenseCategory,
    val monthlyLimit: Double,
    val month: Int,
    val year: Int,
    val rollover: Boolean = false
)

data class CategorySummary(
    val category: ExpenseCategory,
    val totalAmount: Double,
    val transactionCount: Int,
    val budgetLimit: Double? = null,
    val percentOfTotal: Float = 0f
)

data class DashboardStats(
    val dailyTotal: Double = 0.0,
    val weeklyTotal: Double = 0.0,
    val monthlyTotal: Double = 0.0,
    val categorySummaries: List<CategorySummary> = emptyList(),
    val topMerchants: List<MerchantSummary> = emptyList(),
    val dailyAverage: Double = 0.0,
    val projectedMonthly: Double = 0.0,
    val alerts: List<SpendingAlert> = emptyList()
)

data class MerchantSummary(
    val name: String,
    val totalAmount: Double,
    val count: Int,
    val category: ExpenseCategory
)

data class SpendingAlert(
    val type: AlertType,
    val message: String,
    val category: ExpenseCategory? = null
)

enum class AlertType { BUDGET_EXCEEDED, BUDGET_WARNING, UNUSUAL_SPENDING }

data class AppSettings(
    val currencySymbol: String = "QAR",
    val trustedSenders: List<String> = listOf(
        "QNB","DOHA BANK","CBQ","MASRAF","HSBC","QIIB","DUKHAN","AHLIBANK","OOREDOO","VODAFONE"
    ),
    val debitKeywords: List<String> = listOf(
        "debited","payment","purchase","withdrawn","charged","paid","debit"
    ),
    val creditKeywords: List<String> = listOf("credited","received","refund","salary"),
    val autoScanEnabled: Boolean = true,
    val monthStartDay: Int = 1,
    val darkTheme: Boolean = true,
    val amoledTheme: Boolean = false,
    val hideBalances: Boolean = false,
    val onboardingDone: Boolean = false
)
