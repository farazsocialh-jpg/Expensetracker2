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
    category = try { ExpenseCategory.valueOf(category) } catch (e: Exception) { ExpenseCategory.OTHER },
    dateTime = LocalDateTime.parse(dateTime, FORMATTER),
    balance = balance,
    rawSms = rawSms,
    isManual = isManual,
    note = note,
    sender = sender,
    cardNumber = cardNumber,
    accountLabel = accountLabel
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
    note = note,
    sender = sender,
    cardNumber = cardNumber,
    accountLabel = accountLabel
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
    smsHash = smsHash,
    sender = sender,
    cardNumber = extractCardNumber(rawSms ?: ""),
    accountLabel = ""
)

/** Extract last 4 digits of card number from SMS text */
fun extractCardNumber(sms: String): String {
    val pattern = Regex("""(?:card|a/c|ac|account)\s*(?:no\.?|number|ending|XX+)?[\s*X]*(\d{4})\b""", RegexOption.IGNORE_CASE)
    return pattern.find(sms)?.groupValues?.get(1) ?: ""
}

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
