package com.expensetracker.data.db

import com.expensetracker.domain.model.Budget
import com.expensetracker.domain.model.ExpenseCategory
import com.expensetracker.domain.model.Transaction
import com.expensetracker.service.SmsParser
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    amount = amount,
    merchant = merchant,
    category = ExpenseCategory.valueOf(category),
    dateTime = LocalDateTime.parse(dateTime, FORMATTER),
    balance = balance,
    rawSms = rawSms,
    isManual = isManual,
    note = note
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    amount = amount,
    merchant = merchant,
    category = category.name,
    dateTime = dateTime.format(FORMATTER),
    balance = balance,
    rawSms = rawSms,
    isManual = isManual,
    note = note
)

fun SmsParser.ParsedTransaction.toEntity() = TransactionEntity(
    amount = amount,
    merchant = merchant,
    category = category.name,
    dateTime = dateTime.format(FORMATTER),
    balance = balance,
    rawSms = rawSms,
    isManual = false,
    note = "",
    smsHash = smsHash
)

fun BudgetEntity.toDomain() = Budget(
    id = id,
    category = ExpenseCategory.valueOf(category),
    monthlyLimit = monthlyLimit,
    month = month,
    year = year
)

fun Budget.toEntity() = BudgetEntity(
    id = id,
    category = category.name,
    monthlyLimit = monthlyLimit,
    month = month,
    year = year
)
