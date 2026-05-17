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
    private fun fmt(dt: LocalDateTime) = dt.format(fmt)

    fun getAll(): Flow<List<Transaction>> =
        txDao.getAll().map { list -> list.map { it.toDomain() } }

    fun getRecent(limit: Int = 8): Flow<List<Transaction>> =
        txDao.getRecent(limit).map { list -> list.map { it.toDomain() } }

    fun getByDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<Transaction>> =
        txDao.getByDateRange(fmt(start), fmt(end)).map { list -> list.map { it.toDomain() } }

    fun search(q: String): Flow<List<Transaction>> =
        txDao.search(q).map { list -> list.map { it.toDomain() } }

    fun getByCategory(cat: ExpenseCategory): Flow<List<Transaction>> =
        txDao.getByCategory(cat.name).map { list -> list.map { it.toDomain() } }

    fun getByCard(card: String): Flow<List<Transaction>> =
        txDao.getByCard(card).map { list -> list.map { it.toDomain() } }

    fun getRecurring(): Flow<List<Transaction>> =
        txDao.getRecurring().map { list -> list.map { it.toDomain() } }

    fun getDistinctCards(): Flow<List<String>> = txDao.getDistinctCards()

    fun getMerchantRules(): Flow<List<MerchantRule>> =
        ruleDao.getAll().map { list -> list.map { it.toDomain() } }

    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>> =
        budgetDao.getForMonth(month, year).map { list -> list.map { it.toDomain() } }

    suspend fun getDashboardStats(now: LocalDate = LocalDate.now(), monthStartDay: Int = 1): DashboardStats {
        val startDay   = monthStartDay.coerceIn(1, 28)
        val today      = now
        val dayStart   = today.atStartOfDay()
        val weekStart  = today.minusDays(today.dayOfWeek.value.toLong() - 1).atStartOfDay()
        val monthStart = if (today.dayOfMonth >= startDay)
            today.withDayOfMonth(startDay).atStartOfDay()
        else today.minusMonths(1).withDayOfMonth(startDay).atStartOfDay()
        val end = today.plusDays(1).atStartOfDay()

        val daily   = txDao.sumPeriod(fmt(dayStart),   fmt(end)) ?: 0.0
        val weekly  = txDao.sumPeriod(fmt(weekStart),  fmt(end)) ?: 0.0
        val monthly = txDao.sumPeriod(fmt(monthStart), fmt(end)) ?: 0.0

        val summaries = ExpenseCategory.values().mapNotNull { cat ->
            val total = txDao.sumCategoryPeriod(cat.name, fmt(monthStart), fmt(end)) ?: 0.0
            val count = txDao.countCategoryPeriod(cat.name, fmt(monthStart), fmt(end))
            if (total <= 0.0 && count == 0) null
            else {
                val budget = budgetDao.getForCategory(cat.name, today.monthValue, today.year)
                val pct = if (monthly > 0) (total / monthly * 100).toFloat() else 0f
                CategorySummary(cat, total, count, budget?.monthlyLimit, pct)
            }
        }.sortedByDescending { it.totalAmount }

        val daysElapsed = java.time.temporal.ChronoUnit.DAYS.between(monthStart.toLocalDate(), today).coerceAtLeast(1)
        val dailyAvg    = monthly / daysElapsed
        val projected   = dailyAvg * 30

        val alerts = summaries.mapNotNull { s ->
            val limit = s.budgetLimit ?: return@mapNotNull null
            when {
                s.totalAmount >= limit ->
                    SpendingAlert(AlertType.BUDGET_EXCEEDED, "${s.category.emoji} ${s.category.displayName} budget exceeded!", s.category)
                s.totalAmount >= limit * 0.8 ->
                    SpendingAlert(AlertType.BUDGET_WARNING, "${s.category.emoji} ${s.category.displayName} at ${((s.totalAmount/limit)*100).toInt()}%", s.category)
                else -> null
            }
        }

        return DashboardStats(
            dailyTotal = daily, weeklyTotal = weekly, monthlyTotal = monthly,
            categorySummaries = summaries, topMerchants = emptyList(),
            dailyAverage = dailyAvg, projectedMonthly = projected, alerts = alerts
        )
    }

    suspend fun insert(t: Transaction): Long = txDao.insert(t.toEntity())

    suspend fun insertFromSms(parsed: SmsParser.ParsedTransaction): Long? {
        if (txDao.findBySmsHash(parsed.smsHash) != null) return null
        val entity = parsed.toEntity()
        // Apply merchant rule if exists
        val normalizedName = normalizeMerchant(parsed.merchant)
        val matchedRule = ruleDao.findMatch(normalizedName)
        val categoryStr: String = if (matchedRule != null && matchedRule.applyToFuture) matchedRule.category else entity.category
        return txDao.insert(entity.copy(category = categoryStr))
    }

    suspend fun update(t: Transaction) = txDao.update(t.toEntity())
    suspend fun delete(id: Long)        = txDao.deleteById(id)
    suspend fun updateAccountLabel(card: String, label: String) = txDao.updateAccountLabel(card, label)

    suspend fun applyMerchantRule(
        merchantNormalized: String,
        displayName: String,
        category: ExpenseCategory,
        applyToPast: Boolean
    ) {
        val rule = MerchantRuleEntity(
            pattern = merchantNormalized, displayName = displayName,
            category = category.name, applyToFuture = true,
            applyToPast = applyToPast, createdAt = now()
        )
        ruleDao.insert(rule)
        if (applyToPast) {
            txDao.bulkUpdateCategory(merchantNormalized, category.name, now())
        }
    }

    suspend fun deleteMerchantRule(rule: MerchantRule) = ruleDao.delete(rule.toEntity())

    suspend fun saveBudget(b: Budget): Long = budgetDao.insert(b.toEntity())
    suspend fun deleteBudget(b: Budget)     = budgetDao.delete(b.toEntity())
}
