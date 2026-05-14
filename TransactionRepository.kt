package com.expensetracker.data.repository

import com.expensetracker.data.db.*
import com.expensetracker.domain.model.*
import com.expensetracker.service.SmsParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val txDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val ruleDao: MerchantRuleDao
) {
    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private fun now() = LocalDateTime.now().format(fmt)

    // ─── Transactions ────────────────────────────────────────────────────────

    fun getAll(): Flow<List<Transaction>> = txDao.getAllTransactions().map { it.map { e -> e.toDomain() } }

    fun getRecent(limit: Int = 8): Flow<List<Transaction>> = txDao.getRecent(limit).map { it.map { e -> e.toDomain() } }

    fun getByDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<Transaction>> =
        txDao.getByDateRange(start.format(fmt), end.format(fmt)).map { it.map { e -> e.toDomain() } }

    fun search(q: String): Flow<List<Transaction>> = txDao.search(q).map { it.map { e -> e.toDomain() } }

    fun getByCategory(cat: ExpenseCategory): Flow<List<Transaction>> =
        txDao.getByCategory(cat.name).map { it.map { e -> e.toDomain() } }

    fun getByCard(card: String): Flow<List<Transaction>> =
        txDao.getByCard(card).map { it.map { e -> e.toDomain() } }

    fun getRecurring(): Flow<List<Transaction>> =
        txDao.getRecurring().map { it.map { e -> e.toDomain() } }

    fun getDistinctCards(): Flow<List<String>> = txDao.getDistinctCards()

    // ─── Dashboard stats ─────────────────────────────────────────────────────

    suspend fun getDashboardStats(
        now: LocalDate = LocalDate.now(),
        monthStartDay: Int = 1
    ): DashboardStats {
        val startDay = monthStartDay.coerceIn(1, 28)
        val today = now
        val dayStart   = today.atStartOfDay()
        val weekStart  = today.minusDays(today.dayOfWeek.value.toLong() - 1).atStartOfDay()
        val monthStart = if (today.dayOfMonth >= startDay)
            today.withDayOfMonth(startDay).atStartOfDay()
        else today.minusMonths(1).withDayOfMonth(startDay).atStartOfDay()
        val end = today.plusDays(1).atStartOfDay()

        val daily   = txDao.sumForPeriod(dayStart.format(fmt), end.format(fmt)) ?: 0.0
        val weekly  = txDao.sumForPeriod(weekStart.format(fmt), end.format(fmt)) ?: 0.0
        val monthly = txDao.sumForPeriod(monthStart.format(fmt), end.format(fmt)) ?: 0.0

        val monthBudgets = budgetDao.getForMonth(today.monthValue, today.year)
        val totalBudget  = 0.0  // computed from flow elsewhere

        val summaries = ExpenseCategory.values().mapNotNull { cat ->
            val total = txDao.sumForCategoryPeriod(cat.name, monthStart.format(fmt), end.format(fmt)) ?: 0.0
            val count = txDao.countForCategoryPeriod(cat.name, monthStart.format(fmt), end.format(fmt))
            if (total <= 0.0 && count == 0) null
            else {
                val budget = budgetDao.getForCategory(cat.name, today.monthValue, today.year)
                val pct    = if (monthly > 0) (total / monthly * 100).toFloat() else 0f
                CategorySummary(cat, total, count, budget?.monthlyLimit, pct)
            }
        }.sortedByDescending { it.totalAmount }

        val topMerchants = txDao.getTopMerchants(monthStart.format(fmt), end.format(fmt), 5)
            .map { row ->
                MerchantSummary(
                    name = row.merchant,
                    totalAmount = row.total,
                    count = row.cnt,
                    category = try { ExpenseCategory.valueOf(row.category) } catch (_: Exception) { ExpenseCategory.OTHER }
                )
            }

        val daysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(monthStart, end.toLocalDate()).coerceAtLeast(1)
        val dailyAvg = monthly / daysInPeriod
        val daysInMonth = 30L
        val projected = dailyAvg * daysInMonth

        val alerts = buildAlerts(summaries, monthly, projected)

        return DashboardStats(
            dailyTotal = daily, weeklyTotal = weekly, monthlyTotal = monthly,
            monthlyBudget = totalBudget, categorySummaries = summaries,
            recentTransactions = emptyList(), topMerchants = topMerchants,
            recurringTotal = 0.0, savingsRate = 0.0,
            dailyAverage = dailyAvg, projectedMonthly = projected, alerts = alerts
        )
    }

    private fun buildAlerts(summaries: List<CategorySummary>, total: Double, projected: Double): List<SpendingAlert> {
        val alerts = mutableListOf<SpendingAlert>()
        summaries.forEach { s ->
            val limit = s.budgetLimit ?: return@forEach
            when {
                s.totalAmount >= limit ->
                    alerts.add(SpendingAlert(AlertType.BUDGET_EXCEEDED,
                        "${s.category.emoji} ${s.category.displayName} budget exceeded!", s.totalAmount, s.category))
                s.totalAmount >= limit * 0.8 ->
                    alerts.add(SpendingAlert(AlertType.BUDGET_WARNING,
                        "${s.category.emoji} ${s.category.displayName} at ${((s.totalAmount/limit)*100).toInt()}% of budget", s.totalAmount, s.category))
            }
        }
        return alerts
    }

    // ─── CRUD ────────────────────────────────────────────────────────────────

    suspend fun insert(t: Transaction): Long = txDao.insert(t.toEntity())

    suspend fun insertFromSms(parsed: SmsParser.ParsedTransaction): Long? {
        if (txDao.findBySmsHash(parsed.smsHash) != null) return null
        // Apply merchant rules
        val rule = ruleDao.findMatchingRule(normalizeMerchant(parsed.merchant))
        val entity = parsed.toEntity()
        val finalEntity = if (rule != null && rule.applyToFuture)
            entity.copy(category = rule.category.name) else entity
        return txDao.insert(finalEntity)
    }

    suspend fun update(t: Transaction) = txDao.update(t.toEntity())
    suspend fun delete(id: Long)        = txDao.deleteById(id)

    suspend fun updateAccountLabel(card: String, label: String) =
        txDao.updateAccountLabel(card, label)

    // ─── Merchant rules ───────────────────────────────────────────────────────

    suspend fun applyMerchantRule(
        merchantNormalized: String,
        category: ExpenseCategory,
        displayName: String,
        applyToPast: Boolean
    ) {
        val rule = MerchantRuleEntity(
            pattern = merchantNormalized, displayName = displayName,
            category = category.name, applyToFuture = true, applyToPast = applyToPast,
            confidenceScore = 1f, createdAt = LocalDateTime.now().format(fmt)
        )
        ruleDao.insert(rule)
        if (applyToPast) {
            txDao.bulkUpdateCategory(merchantNormalized, category.name, LocalDateTime.now().format(fmt))
        }
    }

    fun getMerchantRules(): Flow<List<MerchantRule>> =
        ruleDao.getAll().map { it.map { e -> e.toDomain() } }

    suspend fun deleteMerchantRule(rule: MerchantRule) = ruleDao.delete(rule.toEntity())

    // ─── Budgets ─────────────────────────────────────────────────────────────

    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>> =
        budgetDao.getForMonth(month, year).map { it.map { e -> e.toDomain() } }

    suspend fun saveBudget(b: Budget): Long = budgetDao.insert(b.toEntity())
    suspend fun deleteBudget(b: Budget)     = budgetDao.delete(b.toEntity())
}
