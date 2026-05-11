package com.expensetracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val category: String,
    val dateTime: String,
    val balance: Double?,
    val rawSms: String?,
    val isManual: Boolean,
    val note: String,
    val smsHash: String? = null,
    val sender: String = "",
    val cardNumber: String = "",
    val accountLabel: String = ""
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
