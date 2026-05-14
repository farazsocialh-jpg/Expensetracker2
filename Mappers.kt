package com.expensetracker.data.db

import com.expensetracker.domain.model.*
import com.expensetracker.service.SmsParser
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME
private fun now() = LocalDateTime.now().format(FMT)

fun TransactionEntity.toDomain() = Transaction(
    id = id, uuid = uuid, amount = amount, currency = currency,
    merchant = merchant, normalizedMerchant = normalizedMerchant, merchantId = merchantId,
    category = safeCategory(category), subcategory = subcategory,
    dateTime = LocalDateTime.parse(dateTime, FMT),
    bankName = bankName, accountLast4 = accountLast4, accountLabel = accountLabel,
    paymentMethod = safeEnum<PaymentMethod>(paymentMethod) ?: PaymentMethod.DEBIT_CARD,
    transactionType = safeEnum<TransactionType>(transactionType) ?: TransactionType.DEBIT,
    balance = balance, rawSms = rawSms, isManual = isManual, isExcluded = isExcluded,
    isRecurring = isRecurring, recurringGroupId = recurringGroupId,
    isTransfer = isTransfer, isRefund = isRefund, note = note,
    tags = if (tags.isBlank()) emptyList() else tags.split("|").filter { it.isNotBlank() },
    confidenceScore = confidenceScore, userEdited = userEdited, sender = sender,
    createdAt = parseDateTime(createdAt), updatedAt = parseDateTime(updatedAt)
)

fun Transaction.toEntity() = TransactionEntity(
    id = id, uuid = uuid, amount = amount, currency = currency,
    merchant = merchant, normalizedMerchant = normalizedMerchant, merchantId = merchantId,
    category = category.name, subcategory = subcategory,
    dateTime = dateTime.format(FMT), bankName = bankName,
    accountLast4 = accountLast4, accountLabel = accountLabel,
    paymentMethod = paymentMethod.name, transactionType = transactionType.name,
    balance = balance, rawSms = rawSms, isManual = isManual, isExcluded = isExcluded,
    isRecurring = isRecurring, recurringGroupId = recurringGroupId,
    isTransfer = isTransfer, isRefund = isRefund, note = note,
    tags = tags.joinToString("|"), confidenceScore = confidenceScore,
    userEdited = userEdited, sender = sender,
    createdAt = createdAt.format(FMT), updatedAt = updatedAt.format(FMT)
)

fun SmsParser.ParsedTransaction.toEntity() = TransactionEntity(
    uuid = java.util.UUID.randomUUID().toString(),
    amount = amount, currency = "QAR", merchant = merchant,
    normalizedMerchant = normalizeMerchant(merchant),
    category = category.name, dateTime = dateTime.format(FMT),
    bankName = extractBankName(sender), accountLast4 = extractCardNumber(rawSms),
    paymentMethod = "DEBIT_CARD", transactionType = "DEBIT",
    balance = balance, rawSms = rawSms, isManual = false,
    note = "", tags = "", confidenceScore = confidenceScore,
    sender = sender, smsHash = smsHash,
    createdAt = now(), updatedAt = now()
)

fun MerchantRuleEntity.toDomain() = MerchantRule(
    id = id, pattern = pattern, displayName = displayName,
    category = safeCategory(category), subcategory = subcategory,
    applyToFuture = applyToFuture, applyToPast = applyToPast,
    confidenceScore = confidenceScore,
    createdAt = parseDateTime(createdAt)
)

fun MerchantRule.toEntity() = MerchantRuleEntity(
    id = id, pattern = pattern, displayName = displayName,
    category = category.name, subcategory = subcategory,
    applyToFuture = applyToFuture, applyToPast = applyToPast,
    confidenceScore = confidenceScore, createdAt = createdAt.format(FMT)
)

fun BudgetEntity.toDomain() = Budget(
    id = id, category = safeCategory(category), monthlyLimit = monthlyLimit,
    month = month, year = year, rollover = rollover
)

fun Budget.toEntity() = BudgetEntity(
    id = id, category = category.name, monthlyLimit = monthlyLimit,
    month = month, year = year, rollover = rollover
)

// ─── Helpers ─────────────────────────────────────────────────────────────────

fun normalizeMerchant(raw: String): String =
    raw.lowercase()
        .replace(Regex("[^a-z0-9 ]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(40)

fun extractCardNumber(sms: String): String {
    val p = Regex("""(?:card|a/c|ac|account)\s*(?:no\.?|number|ending|X+)?[\sX*]*(\d{4})\b""", RegexOption.IGNORE_CASE)
    return p.find(sms)?.groupValues?.get(1) ?: ""
}

fun extractBankName(sender: String): String {
    val banks = listOf("QNB","CBQ","DOHA","MASRAF","HSBC","QIIB","DUKHAN","AHLIBANK","COMMERCIAL")
    val up = sender.uppercase()
    return banks.firstOrNull { up.contains(it) } ?: sender.take(20)
}

private fun safeCategory(name: String): ExpenseCategory =
    try { ExpenseCategory.valueOf(name) } catch (_: Exception) { ExpenseCategory.OTHER }

private inline fun <reified T : Enum<T>> safeEnum(name: String): T? =
    try { enumValueOf<T>(name) } catch (_: Exception) { null }

private fun parseDateTime(s: String): LocalDateTime =
    try { LocalDateTime.parse(s, FMT) } catch (_: Exception) { LocalDateTime.now() }
