package com.expensetracker.service

import java.security.MessageDigest
import java.time.LocalDateTime

/**
 * SMS Parser — fully configurable, currency-agnostic.
 * Supports user-defined sender numbers/names and custom keywords.
 * Currency symbol is configurable (default: QAR).
 */
object SmsParser {

    data class ParsedTransaction(
        val amount: Double,
        val merchant: String,
        val balance: Double?,
        val category: com.expensetracker.domain.model.ExpenseCategory,
        val dateTime: LocalDateTime,
        val rawSms: String,
        val smsHash: String,
        val sender: String = ""
    )

    data class ParserConfig(
        val currencySymbol: String = "QAR",
        val trustedSenders: List<String> = emptyList(),   // e.g. ["QNB", "DOHABANK", "+97444123456"]
        val debitKeywords: List<String> = emptyList(),    // extra user keywords
        val creditKeywords: List<String> = emptyList()    // words that mark a credit (to ignore)
    )

    // ── Built-in debit signals ───────────────────────────────────────────────
    private val DEFAULT_DEBIT_KEYWORDS = listOf(
        "debited", "deducted", "spent", "paid", "payment", "purchase",
        "withdrawn", "charged", "debit", "pos ", "txn", "transaction",
        "transferred", "sent", "withdrawn"
    )

    private val DEFAULT_CREDIT_KEYWORDS = listOf(
        "credited", "received", "credit", "deposited", "added",
        "refund", "cashback", "salary", "neft received"
    )

    private val OTP_KEYWORDS = listOf(
        "otp", "one time", "verification code", "do not share",
        "expiry", "expires in", "login code", "auth code"
    )

    // ── Amount patterns — currency-agnostic + QAR specific ──────────────────
    private fun buildAmountPatterns(currency: String): List<Regex> = listOf(
        Regex("""(?:${Regex.escape(currency)}|QAR|SAR|AED|KWD|BHD|OMR|Rs\.?|INR|USD|\$|EUR|GBP|£|€)\s*([0-9,]+(?:\.[0-9]{1,3})?)""", RegexOption.IGNORE_CASE),
        Regex("""([0-9,]+(?:\.[0-9]{1,3})?)\s*(?:${Regex.escape(currency)}|QAR|SAR|AED|KWD)""", RegexOption.IGNORE_CASE),
        Regex("""(?:amount|amt|sum)\s*(?:of|:)?\s*(?:[A-Z]{2,4})?\s*([0-9,]+(?:\.[0-9]{1,3})?)""", RegexOption.IGNORE_CASE),
        Regex("""(?:debited|paid|spent|charged|withdrawn)\s+(?:with\s+)?(?:[A-Z]{2,4}\s+)?([0-9,]+(?:\.[0-9]{1,3})?)""", RegexOption.IGNORE_CASE)
    )

    // ── Balance patterns ─────────────────────────────────────────────────────
    private val BALANCE_PATTERNS = listOf(
        Regex("""(?:avl|avail|available|bal|balance)\s*(?:bal|balance|amt)?\s*(?:is|:|-|=)?\s*(?:[A-Z]{2,4})?\s*([0-9,]+(?:\.[0-9]{1,3})?)""", RegexOption.IGNORE_CASE),
        Regex("""(?:[A-Z]{2,4})\s*([0-9,]+(?:\.[0-9]{1,3})?)\s*(?:available|avl|bal|balance)""", RegexOption.IGNORE_CASE)
    )

    // ── Merchant patterns ────────────────────────────────────────────────────
    private val MERCHANT_PATTERNS = listOf(
        Regex("""(?:at|to|At|To|merchant[:\s]+)\s+([A-Za-z0-9\s\-&'.\/]+?)(?:\s+on\s+|\s+ref|\s+txn|\s+for|\s*\.|,|\s{2,}|$)"""),
        Regex("""(?:purchase|payment|paid)\s+(?:at|to)\s+([A-Za-z0-9\s\-&'.]+?)(?:\s+on|\.|,|$)""", RegexOption.IGNORE_CASE),
        Regex("""POS\s+([A-Za-z0-9\s\-&'.]+?)(?:\s+\d|\.|,|$)"""),
        Regex("""(?:towards|for)\s+([A-Za-z0-9\s\-&'.]+?)(?:\s+on|\s+ref|\.|,|$)""", RegexOption.IGNORE_CASE)
    )

    // ── Category keywords ────────────────────────────────────────────────────
    private val CATEGORY_KEYWORDS: Map<com.expensetracker.domain.model.ExpenseCategory, List<String>> = mapOf(
        com.expensetracker.domain.model.ExpenseCategory.FOOD to listOf(
            "restaurant", "cafe", "coffee", "pizza", "burger", "kfc", "mcdonalds",
            "subway", "food", "dining", "eatery", "biryani", "shawarma", "zomato",
            "talabat", "careem food", "noon food", "starbucks", "tim hortons",
            "hardees", "popeyes", "wendys", "dominos", "papa johns", "little caesars"
        ),
        com.expensetracker.domain.model.ExpenseCategory.GROCERY to listOf(
            "lulu", "carrefour", "safari", "al meera", "monoprix", "spinneys",
            "waitrose", "family food", "géant", "hypermarket", "supermarket",
            "grocery", "fresh", "organic", "co-op", "spar", "grand mart"
        ),
        com.expensetracker.domain.model.ExpenseCategory.TRANSPORT to listOf(
            "uber", "careem", "taxi", "metro", "mowasalat", "bus", "karwa",
            "parking", "salik", "toll", "lyft", "bolt", "indrive"
        ),
        com.expensetracker.domain.model.ExpenseCategory.FUEL to listOf(
            "woqod", "fuel", "petrol", "gas station", "filling station",
            "shell", "total", "q8", "qatar fuel", "petroq"
        ),
        com.expensetracker.domain.model.ExpenseCategory.SHOPPING to listOf(
            "amazon", "noon", "namshi", "ounass", "zara", "h&m", "ikea",
            "centrepoint", "max fashion", "splash", "landmark", "ace hardware",
            "home centre", "pottery barn", "virgin megastore", "plug", "istore"
        ),
        com.expensetracker.domain.model.ExpenseCategory.BILLS to listOf(
            "ooredoo", "vodafone qatar", "kahramaa", "electricity", "water",
            "utility", "internet", "broadband", "du ", "etisalat", "telecom",
            "subscription", "netflix", "spotify", "apple", "google play",
            "microsoft", "adobe", "aws", "stc"
        ),
        com.expensetracker.domain.model.ExpenseCategory.HEALTH to listOf(
            "hospital", "clinic", "pharmacy", "medical", "doctor", "dental",
            "hamad", "aster", "american hospital", "sidra", "qmc", "al ahli",
            "medicine", "optician", "lab", "diagnostic", "health"
        ),
        com.expensetracker.domain.model.ExpenseCategory.ENTERTAINMENT to listOf(
            "cinema", "movie", "vox", "reel", "muvi", "bowling", "escape room",
            "theme park", "aqua", "gaming", "playstation", "xbox", "steam",
            "concert", "event", "ticket", "entertainment"
        ),
        com.expensetracker.domain.model.ExpenseCategory.EDUCATION to listOf(
            "school", "university", "tuition", "course", "education", "training",
            "books", "stationery", "qatar university", "cmu", "nu-q", "hec"
        ),
        com.expensetracker.domain.model.ExpenseCategory.TRAVEL to listOf(
            "qatar airways", "airline", "flight", "airport", "hotel", "booking",
            "airbnb", "holiday", "travel", "resort", "marriott", "hilton",
            "hyatt", "rotana", "intercontinental", "expedia", "agoda"
        )
    )

    // ── Public API ───────────────────────────────────────────────────────────

    fun isDebitTransaction(sms: String, config: ParserConfig = ParserConfig()): Boolean {
        val lower = sms.lowercase()
        if (OTP_KEYWORDS.any { lower.contains(it) }) return false

        val allDebitKeywords = DEFAULT_DEBIT_KEYWORDS + config.debitKeywords
        val allCreditKeywords = DEFAULT_CREDIT_KEYWORDS + config.creditKeywords

        val hasDebit = allDebitKeywords.any { lower.contains(it) }
        val hasCredit = allCreditKeywords.any { lower.contains(it) }
        if (hasCredit && !hasDebit) return false

        val amountPatterns = buildAmountPatterns(config.currencySymbol)
        return hasDebit && amountPatterns.any { it.containsMatchIn(sms) }
    }

    fun isTrustedSender(sender: String, config: ParserConfig): Boolean {
        if (config.trustedSenders.isEmpty()) return true
        val senderLower = sender.lowercase()
        return config.trustedSenders.any { senderLower.contains(it.lowercase()) }
    }

    fun parse(
        sms: String,
        sender: String = "",
        receivedAt: LocalDateTime = LocalDateTime.now(),
        config: ParserConfig = ParserConfig()
    ): ParsedTransaction? {
        if (!isDebitTransaction(sms, config)) return null

        val amountPatterns = buildAmountPatterns(config.currencySymbol)
        val amount = extractAmount(sms, amountPatterns) ?: return null
        val merchant = extractMerchant(sms).ifBlank { sender.ifBlank { "Unknown" } }
        val balance = extractBalance(sms)
        val category = categorize(merchant, sms)
        val hash = computeHash(sms)

        return ParsedTransaction(
            amount = amount,
            merchant = merchant,
            balance = balance,
            category = category,
            dateTime = receivedAt,
            rawSms = sms,
            smsHash = hash,
            sender = sender
        )
    }

    fun categorize(merchant: String, sms: String = ""): com.expensetracker.domain.model.ExpenseCategory {
        val combined = (merchant + " " + sms).lowercase()
        for ((category, keywords) in CATEGORY_KEYWORDS) {
            if (keywords.any { combined.contains(it) }) return category
        }
        return com.expensetracker.domain.model.ExpenseCategory.OTHER
    }

    private fun extractAmount(sms: String, patterns: List<Regex>): Double? {
        for (pattern in patterns) {
            val match = pattern.find(sms) ?: continue
            val raw = match.groupValues[1].replace(",", "")
            val value = raw.toDoubleOrNull()
            if (value != null && value > 0) return value
        }
        return null
    }

    private fun extractMerchant(sms: String): String {
        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(sms) ?: continue
            val m = match.groupValues[1].trim()
            if (m.length >= 2) return m.replace(Regex("""\b\d{4,}\b"""), "").trim().take(50)
        }
        return ""
    }

    private fun extractBalance(sms: String): Double? {
        for (pattern in BALANCE_PATTERNS) {
            val match = pattern.find(sms) ?: continue
            val raw = match.groupValues[1].replace(",", "")
            val value = raw.toDoubleOrNull()
            if (value != null) return value
        }
        return null
    }

    fun computeHash(sms: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(sms.toByteArray())
            .joinToString("") { "%02x".format(it) }.take(16)
    }

    // Sample Qatar bank SMS formats for testing
    val SAMPLE_SMS = listOf(
        "QNB: Your account XX1234 debited QAR 150.00 at LULU HYPERMARKET on 10/05/2025. Avail Bal: QAR 5,432.10",
        "DOHA BANK: Payment of QAR 75.50 made at WOQOD FUEL STATION. Balance: QAR 2,100.00",
        "CBQ Alert: QAR 250.00 debited from a/c XX5678 for AMAZON.COM. Available balance QAR 8,750.00",
        "Masraf Al Rayan: Txn of QAR 45.00 at STARBUCKS DOHA on 10-May-25. Bal QAR 3,200.50",
        "HSBC Qatar: Your card ending 4321 was used for QAR 1,200.00 at QATAR AIRWAYS. Bal: QAR 15,000.00"
    )
}
