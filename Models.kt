package com.expensetracker.domain.model
import java.time.LocalDateTime
import java.util.UUID

enum class ExpenseCategory(val displayName: String, val emoji: String, val color: Long) {
    FOOD("Food & Dining","🍔",0xFFFF6B35), GROCERY("Grocery","🛒",0xFF4CAF50),
    TRANSPORT("Transport","🚗",0xFF2196F3), FUEL("Fuel","⛽",0xFFFF9800),
    SHOPPING("Shopping","🛍️",0xFFE91E63), BILLS("Bills & Utilities","💡",0xFF9C27B0),
    HEALTH("Health & Medical","🏥",0xFFF44336), ENTERTAINMENT("Entertainment","🎬",0xFF00BCD4),
    EDUCATION("Education","📚",0xFF3F51B5), TRAVEL("Travel","✈️",0xFF009688),
    SUBSCRIPTION("Subscriptions","🔄",0xFF607D8B), SALARY("Income / Salary","💼",0xFF43A047),
    TRANSFER("Transfer","↔️",0xFF90A4AE), SAVINGS("Savings","🏦",0xFF1565C0),
    DEBT("Debt / EMI","💳",0xFFB71C1C), OTHER("Other","💰",0xFF78909C)
}
enum class TransactionType { EXPENSE, INCOME, TRANSFER, REFUND, SUBSCRIPTION, EMI }
enum class PaymentMethod { DEBIT_CARD, CREDIT_CARD, CASH, BANK_TRANSFER, WALLET, UPI, OTHER }
enum class RecategorizeScope { ONCE, ALL_SIMILAR, FUTURE_ONLY }
enum class WalletType { CASH, BANK, CREDIT_CARD, SAVINGS, INVESTMENT, CRYPTO, OTHER }
enum class GoalStatus { ACTIVE, COMPLETED, PAUSED }
enum class AlertType { BUDGET_EXCEEDED, BUDGET_WARNING, UNUSUAL_SPENDING }
enum class BudgetPeriod { WEEKLY, MONTHLY, ANNUAL }

data class Transaction(
    val id: Long = 0, val uuid: String = UUID.randomUUID().toString(),
    val amount: Double, val currency: String = "QAR",
    val merchant: String, val normalizedMerchant: String = "",
    val category: ExpenseCategory, val subcategory: String = "",
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val paymentMethod: PaymentMethod = PaymentMethod.DEBIT_CARD,
    val walletId: Long = 0, val walletName: String = "",
    val dateTime: LocalDateTime, val bankName: String = "",
    val accountLast4: String = "", val accountLabel: String = "",
    val balance: Double? = null, val rawSms: String? = null,
    val isManual: Boolean = false, val isExcluded: Boolean = false,
    val isRecurring: Boolean = false, val recurringGroupId: String = "",
    val isTransfer: Boolean = false, val isRefund: Boolean = false,
    val note: String = "", val tags: List<String> = emptyList(),
    val confidenceScore: Float = 1.0f, val userEdited: Boolean = false,
    val sender: String = "", val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
data class Wallet(val id: Long = 0, val name: String, val type: WalletType = WalletType.BANK, val currency: String = "QAR", val balance: Double = 0.0, val color: Long = 0xFF1565C0, val emoji: String = "🏦", val isHidden: Boolean = false, val isArchived: Boolean = false, val bankName: String = "", val accountLast4: String = "", val createdAt: LocalDateTime = LocalDateTime.now())
data class Budget(val id: Long = 0, val name: String = "", val category: ExpenseCategory, val monthlyLimit: Double, val period: BudgetPeriod = BudgetPeriod.MONTHLY, val month: Int, val year: Int, val rollover: Boolean = false, val alertAt: Float = 0.8f)
data class SavingsGoal(val id: Long = 0, val name: String, val targetAmount: Double, val currentAmount: Double = 0.0, val emoji: String = "🎯", val color: Long = 0xFF1565C0, val deadline: LocalDateTime? = null, val status: GoalStatus = GoalStatus.ACTIVE, val createdAt: LocalDateTime = LocalDateTime.now()) {
    val progress: Float get() = if (targetAmount > 0) (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val remaining: Double get() = (targetAmount - currentAmount).coerceAtLeast(0.0)
}
data class MerchantRule(val id: Long = 0, val pattern: String, val displayName: String, val category: ExpenseCategory, val applyToFuture: Boolean = true, val applyToPast: Boolean = false, val createdAt: LocalDateTime = LocalDateTime.now())
data class CategorySummary(val category: ExpenseCategory, val totalAmount: Double, val transactionCount: Int, val budgetLimit: Double? = null, val percentOfTotal: Float = 0f)
data class MerchantSummary(val name: String, val totalAmount: Double, val count: Int, val category: ExpenseCategory)
data class DailySpend(val date: String, val amount: Double)
data class DashboardStats(val dailyTotal: Double = 0.0, val weeklyTotal: Double = 0.0, val monthlyTotal: Double = 0.0, val monthlyIncome: Double = 0.0, val categorySummaries: List<CategorySummary> = emptyList(), val dailyAverage: Double = 0.0, val projectedMonthly: Double = 0.0, val savingsRate: Double = 0.0, val alerts: List<SpendingAlert> = emptyList(), val financialHealthScore: Int = 0, val weeklySpends: List<DailySpend> = emptyList())
data class SpendingAlert(val type: AlertType, val message: String, val category: ExpenseCategory? = null)
data class AppSettings(val currencySymbol: String = "QAR", val trustedSenders: List<String> = listOf("QNB","DOHA BANK","CBQ","MASRAF","HSBC","QIIB","DUKHAN","AHLIBANK","OOREDOO","VODAFONE"), val debitKeywords: List<String> = listOf("debited","payment","purchase","withdrawn","charged","paid","debit"), val creditKeywords: List<String> = listOf("credited","received","refund","salary"), val autoScanEnabled: Boolean = true, val monthStartDay: Int = 1, val darkTheme: Boolean = true, val amoledTheme: Boolean = false, val hideBalances: Boolean = false, val onboardingDone: Boolean = false)
