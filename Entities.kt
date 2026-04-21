package com.expensetracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expensetracker.domain.model.ExpenseCategory
import java.time.LocalDateTime

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val category: String, // Store as String enum name
    val dateTime: String, // ISO-8601 string
    val balance: Double?,
    val rawSms: String?,
    val isManual: Boolean,
    val note: String,
    val smsHash: String? = null // For dedup
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val monthlyLimit: Double,
    val month: Int,
    val year: Int
)
