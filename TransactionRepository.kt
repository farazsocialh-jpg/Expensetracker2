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
    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions().map { it.map { e -> e.toDomain() } }

    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>> =
        transactionDao.getRecentTransactions(limit).map { it.map { e -> e.toDomain() } }

    fun getTransactionsByDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateRange(start.format(fmt), end.format(fmt))
            .map { it.map { e -> e.toDomain() } }

    fun searchTransactions(query: String): Flow<List<Transaction>> =
        transactionDao.searchTransactions(query).map { it.map { e -> e.toDomain() } }

    fun getTransactionsByCategory(category: ExpenseCategory): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCategory(category.name).map { it.map { e -> e.toDomain() } }

    fun getTransactionsByCard(card: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCard(card).map { it.map { e -> e.toDomain() } }

    fun getDistinctCards(): Flow<List<String>> = transactionDao.getDistinctCards()

    suspend fun getDashboardStats(now: LocalDate = LocalDate.now()): DashboardStats {
        val dayStart  = now.atStartOfDay()
        val weekStart = now.minusDays(now.dayOfWeek.value.toLong() - 1).atStartOfDay()
        val monthStart = now.withDayOfMonth(1).atStartOfDay()
        val end = now.plusDays(1).atStartOfDay()

        val daily   = transactionDao.getTotalForPeriod(dayStart.format(fmt), end.format(fmt)) ?: 0.0
        val weekly  = transactionDao.getTotalForPeriod(weekStart.format(fmt), end.format(fmt)) ?: 0.0
        val monthly = transactionDao.getTotalForPeriod(monthStart.format(fmt), end.format(fmt)) ?: 0.0

        val summaries = ExpenseCategory.values().mapNotNull { cat ->
            val total = transactionDao.getTotalForCategoryAndPeriod(cat.name, monthStart.format(fmt), end.format(fmt)) ?: 0.0
            val count = transactionDao.getCountForCategoryAndPeriod(cat.name, monthStart.format(fmt), end.format(fmt))
            if (total <= 0.0 && count == 0) null
            else {
                val budget = budgetDao.getBudgetForCategory(cat.name, now.monthValue, now.year)
                CategorySummary(cat, total, count, budget?.monthlyLimit)
            }
        }.sortedByDescending { it.totalAmount }

        return DashboardStats(daily, weekly, monthly, summaries, emptyList())
    }

    suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insertTransaction(transaction.toEntity())

    suspend fun insertFromSms(parsed: SmsParser.ParsedTransaction): Long? {
        if (transactionDao.findBySmsHash(parsed.smsHash) != null) return null
        return transactionDao.insertTransaction(parsed.toEntity())
    }

    suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.updateTransaction(transaction.toEntity())

    suspend fun deleteTransaction(id: Long) = transactionDao.deleteById(id)

    suspend fun updateAccountLabel(cardNumber: String, label: String) {
        // We load all matching entities and update their label
        // Room doesn't expose a bulk update by field easily without a custom query
        // so we add a DAO query for this
        transactionDao.updateAccountLabel(cardNumber, label)
    }

    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>> =
        budgetDao.getBudgetsForMonth(month, year).map { it.map { e -> e.toDomain() } }

    suspend fun saveBudget(budget: Budget): Long = budgetDao.insertBudget(budget.toEntity())
    suspend fun deleteBudget(budget: Budget) = budgetDao.deleteBudget(budget.toEntity())
}
