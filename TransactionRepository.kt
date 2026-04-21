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
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao
) {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions().map { it.map { e -> e.toDomain() } }

    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>> =
        transactionDao.getRecentTransactions(limit).map { it.map { e -> e.toDomain() } }

    fun getTransactionsByDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateRange(
            start.format(formatter), end.format(formatter)
        ).map { it.map { e -> e.toDomain() } }

    fun searchTransactions(query: String): Flow<List<Transaction>> =
        transactionDao.searchTransactions(query).map { it.map { e -> e.toDomain() } }

    fun getTransactionsByCategory(category: ExpenseCategory): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCategory(category.name).map { it.map { e -> e.toDomain() } }

    suspend fun getDashboardStats(now: LocalDate = LocalDate.now()): DashboardStats {
        val dayStart = now.atStartOfDay()
        val weekStart = now.minusDays(now.dayOfWeek.value.toLong() - 1).atStartOfDay()
        val monthStart = now.withDayOfMonth(1).atStartOfDay()
        val end = now.plusDays(1).atStartOfDay()

        val dailyTotal = transactionDao.getTotalForPeriod(dayStart.format(formatter), end.format(formatter)) ?: 0.0
        val weeklyTotal = transactionDao.getTotalForPeriod(weekStart.format(formatter), end.format(formatter)) ?: 0.0
        val monthlyTotal = transactionDao.getTotalForPeriod(monthStart.format(formatter), end.format(formatter)) ?: 0.0

        val budgetEntities = budgetDao.getBudgetsForMonth(now.monthValue, now.year)
        val summaries = ExpenseCategory.values().mapNotNull { cat ->
            val total = transactionDao.getTotalForCategoryAndPeriod(
                cat.name, monthStart.format(formatter), end.format(formatter)
            ) ?: 0.0
            val count = transactionDao.getCountForCategoryAndPeriod(
                cat.name, monthStart.format(formatter), end.format(formatter)
            )
            if (total <= 0.0 && count == 0) null
            else {
                val budget = budgetDao.getBudgetForCategory(cat.name, now.monthValue, now.year)
                CategorySummary(cat, total, count, budget?.monthlyLimit)
            }
        }.sortedByDescending { it.totalAmount }

        return DashboardStats(
            dailyTotal = dailyTotal,
            weeklyTotal = weeklyTotal,
            monthlyTotal = monthlyTotal,
            categorySummaries = summaries,
            recentTransactions = emptyList()
        )
    }

    suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insertTransaction(transaction.toEntity())

    suspend fun insertFromSms(parsed: SmsParser.ParsedTransaction): Long? {
        // Dedup check
        val existing = transactionDao.findBySmsHash(parsed.smsHash)
        if (existing != null) return null
        return transactionDao.insertTransaction(parsed.toEntity())
    }

    suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.updateTransaction(transaction.toEntity())

    suspend fun deleteTransaction(id: Long) =
        transactionDao.deleteById(id)

    // Budget operations
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>> =
        budgetDao.getBudgetsForMonth(month, year).map { it.map { e -> e.toDomain() } }

    suspend fun saveBudget(budget: Budget): Long =
        budgetDao.insertBudget(budget.toEntity())

    suspend fun deleteBudget(budget: Budget) =
        budgetDao.deleteBudget(budget.toEntity())
}
