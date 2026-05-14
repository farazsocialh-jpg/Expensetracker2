package com.expensetracker.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("dateTime"),
        Index("category"),
        Index("accountLast4"),
        Index("normalizedMerchant"),
        Index("isRecurring"),
        Index("smsHash")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = "",
    val amount: Double,
    val currency: String = "QAR",
    val merchant: String,
    val normalizedMerchant: String = "",
    val merchantId: String = "",
    val category: String,
    val subcategory: String = "",
    val dateTime: String,
    val bankName: String = "",
    val accountLast4: String = "",
    val accountLabel: String = "",
    val paymentMethod: String = "DEBIT_CARD",
    val transactionType: String = "DEBIT",
    val balance: Double? = null,
    val rawSms: String? = null,
    val isManual: Boolean = false,
    val isExcluded: Boolean = false,
    val isRecurring: Boolean = false,
    val recurringGroupId: String = "",
    val isTransfer: Boolean = false,
    val isRefund: Boolean = false,
    val note: String = "",
    val tags: String = "",           // pipe-separated
    val confidenceScore: Float = 1f,
    val userEdited: Boolean = false,
    val sender: String = "",
    val smsHash: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val monthlyLimit: Double,
    val month: Int,
    val year: Int,
    val rollover: Boolean = false
)

@Entity(tableName = "merchant_rules", indices = [Index("pattern", unique = true)])
data class MerchantRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pattern: String,
    val displayName: String,
    val category: String,
    val subcategory: String = "",
    val applyToFuture: Boolean = true,
    val applyToPast: Boolean = false,
    val confidenceScore: Float = 1f,
    val createdAt: String = ""
)
