package com.expensetracker.service

import com.expensetracker.domain.model.ExpenseCategory
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

/**
 * SMS Parser — Rule-based engine to extract transaction details from bank SMS.
 *
 * HOW IT WORKS:
 * 1. First checks if the SMS is a debit/expense message (not credit, OTP, or promo)
 * 2. Extracts amount using bank-specific regex patterns
 * 3. Extracts merchant name from "at/to/for" patterns
 * 4. Extracts remaining balance
 * 5. Categorizes the merchant using keyword matching
 *
 * SUPPORTED BANK FORMATS:
 * - SBI, HDFC, ICICI, Axis, Kotak, Yes Bank (Indian banks)
 * - Generic international debit patterns
 */
object SmsParser {

    data class ParsedTransaction(
        val amount: Double,
        val merchant: String,
        val balance: Double?,
        val category: ExpenseCategory,
        val dateTime: LocalDateTime,
        val rawSms: String,
        val smsHash: String
    )

    // ─── DEBIT DETECTION KEYWORDS ──────────────────────────────────────────────
    private val DEBIT_KEYWORDS = listOf(
        "debited", "deducted", "spent", "paid", "payment", "purchase",
        "withdrawn", "txn of", "transaction of", "charged", "debit",
        "pos ", "upi ", "neft sent", "imps sent"
    )

    private val CREDIT_KEYWORDS = listOf(
        "credited", "received", "credit", "deposited", "added",
        "refund", "cashback", "reward", "neft received", "imps received"
    )

    private val OTP_KEYWORDS = listOf(
        "otp", "one time password", "verification code", "login code",
        "do not share", "not share"
    )

    private val PROMO_KEYWORDS = listOf(
        "offer", "discount", "cashback offer", "win", "free", "click here",
        "limited time", "exclusive", "congratulations"
    )

    // ─── AMOUNT PATTERNS ────────────────────────────────────────────────────────
    // Matches: Rs.1,234.56 | INR 1234 | $12.50 | ₹ 500 | USD 100.00
    private val AMOUNT_PATTERNS = listOf(
        Regex("""(?:Rs\.?|INR|₹|USD|\$|EUR|GBP)\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
        Regex("""([0-9,]+(?:\.[0-9]{1,2})?)\s*(?:Rs\.?|INR|₹)""", RegexOption.IGNORE_CASE),
        Regex("""(?:debited|credited|spent|paid|deducted)\s+(?:with\s+)?(?:Rs\.?|INR|₹|USD|\$)?\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
        Regex("""(?:amount|txn|transaction)\s+(?:of\s+)?(?:Rs\.?|INR|₹)?\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
    )

    // ─── BALANCE PATTERNS ───────────────────────────────────────────────────────
    private val BALANCE_PATTERNS = listOf(
        Regex("""(?:avl|avail|available|bal|balance)\s*(?:bal|balance)?\s*(?:is|:)?\s*(?:Rs\.?|INR|₹)?\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
        Regex("""(?:Rs\.?|INR|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)\s*(?:avl|avail|available|bal|balance)""", RegexOption.IGNORE_CASE)
    )

    // ─── MERCHANT PATTERNS ──────────────────────────────────────────────────────
    private val MERCHANT_PATTERNS = listOf(
        // "at MERCHANT_NAME on/for" — most common
        Regex("""(?:at|to|At|To)\s+([A-Za-z0-9\s\-&'.]+?)(?:\s+on\s+|\s+for\s+|\s+ref|\s+upi|\s+txn|\s*\.|,|\s{2,}|$)"""),
        // "purchase at MERCHANT"
        Regex("""purchase\s+at\s+([A-Za-z0-9\s\-&'.]+?)(?:\s+on|\.|,|$)""", RegexOption.IGNORE_CASE),
        // "POS MERCHANT" (card swipe)
        Regex("""POS\s+([A-Za-z0-9\s\-&'.]+?)(?:\s+\d|\.|,|$)"""),
        // "UPI/NEFT to NAME"
        Regex("""(?:UPI|NEFT|IMPS)\s+(?:to|TO)\s+([A-Za-z0-9\s\-&'.@]+?)(?:\s+ref|\s+txn|\.|,|$)""")
    )

    // ─── CATEGORY KEYWORDS ──────────────────────────────────────────────────────
    private val CATEGORY_KEYWORDS: Map<ExpenseCategory, List<String>> = mapOf(
        ExpenseCategory.FOOD to listOf(
            "zomato", "swiggy", "dominos", "pizza", "kfc", "mcdonalds", "mcdonald",
            "burger", "restaurant", "cafe", "coffee", "starbucks", "dunkin", "subway",
            "food", "dining", "eatery", "hotel", "biryani", "dine", "taste",
            "eat", "meals", "snack", "bakery", "juice", "tea", "chai"
        ),
        ExpenseCategory.GROCERY to listOf(
            "bigbasket", "grofers", "blinkit", "zepto", "dmart", "reliance fresh",
            "spencer", "grocery", "supermarket", "mart", "bazaar", "kirana",
            "vegetables", "fruits", "milk", "dairy", "nature's basket"
        ),
        ExpenseCategory.TRANSPORT to listOf(
            "uber", "ola", "rapido", "metro", "bus", "train", "cab", "taxi",
            "auto", "irctc", "railway", "redbus", "makemytrip transport",
            "transport", "ride", "parking", "toll"
        ),
        ExpenseCategory.FUEL to listOf(
            "petrol", "diesel", "fuel", "hp ", "hpcl", "bpcl", "iocl",
            "indian oil", "bharat petroleum", "hindustan petroleum",
            "shell", "reliance petro", "gas station", "cng"
        ),
        ExpenseCategory.SHOPPING to listOf(
            "amazon", "flipkart", "myntra", "ajio", "nykaa", "meesho",
            "snapdeal", "jabong", "shopclues", "walmart", "target",
            "shopping", "store", "mall", "retail", "fashion", "cloth",
            "apparel", "electronics", "reliance digital", "croma", "vijay sales"
        ),
        ExpenseCategory.BILLS to listOf(
            "electricity", "water", "gas", "airtel", "jio", "vi ", "vodafone",
            "idea", "bsnl", "broadband", "wifi", "internet", "dth", "tata sky",
            "dish tv", "recharge", "bill", "utility", "mahanagar gas",
            "paytm bill", "phone bill", "mobile bill", "postpaid"
        ),
        ExpenseCategory.HEALTH to listOf(
            "pharmacy", "medical", "hospital", "clinic", "doctor", "medicine",
            "apollo", "medplus", "1mg", "netmeds", "pharmeasy", "diagnostic",
            "lab", "health", "dental", "optician", "wellness", "chemist"
        ),
        ExpenseCategory.ENTERTAINMENT to listOf(
            "netflix", "spotify", "amazon prime", "hotstar", "zee5", "sony liv",
            "disney", "bookmyshow", "pvr", "inox", "cinepolis", "movie",
            "cinema", "theatre", "game", "steam", "gaming", "concert",
            "event", "ticket", "youtube premium"
        ),
        ExpenseCategory.EDUCATION to listOf(
            "school", "college", "university", "course", "udemy", "coursera",
            "byjus", "byju", "unacademy", "vedantu", "tuition", "fees",
            "books", "stationery", "education", "learning", "upskill"
        ),
        ExpenseCategory.TRAVEL to listOf(
            "indigo", "air india", "spicejet", "goair", "vistara", "airindia",
            "flight", "airline", "hotel booking", "oyo", "treebo", "fabhotel",
            "makemytrip", "yatra", "goibibo", "cleartrip", "booking.com",
            "agoda", "airbnb", "travel", "holiday", "resort", "stay"
        )
    )

    // ─── PUBLIC API ──────────────────────────────────────────────────────────────

    /**
     * Returns true if this SMS looks like a bank debit transaction.
     */
    fun isDebitTransaction(sms: String): Boolean {
        val lower = sms.lowercase()

        // Reject OTPs and promos first
        if (OTP_KEYWORDS.any { lower.contains(it) }) return false
        if (PROMO_KEYWORDS.count { lower.contains(it) } >= 2) return false

        // Must not be a pure credit message
        val hasCredit = CREDIT_KEYWORDS.any { lower.contains(it) }
        val hasDebit = DEBIT_KEYWORDS.any { lower.contains(it) }

        if (hasCredit && !hasDebit) return false

        // Must contain an amount pattern
        val hasAmount = AMOUNT_PATTERNS.any { it.containsMatchIn(sms) }
        return hasDebit && hasAmount
    }

    /**
     * Parse a debit SMS into structured transaction data.
     * Returns null if parsing fails.
     */
    fun parse(sms: String, receivedAt: LocalDateTime = LocalDateTime.now()): ParsedTransaction? {
        if (!isDebitTransaction(sms)) return null

        val amount = extractAmount(sms) ?: return null
        val merchant = extractMerchant(sms).ifBlank { "Unknown Merchant" }
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
            smsHash = hash
        )
    }

    // ─── PRIVATE HELPERS ────────────────────────────────────────────────────────

    private fun extractAmount(sms: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(sms) ?: continue
            val raw = match.groupValues[1].replace(",", "")
            return raw.toDoubleOrNull()
        }
        return null
    }

    private fun extractMerchant(sms: String): String {
        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(sms) ?: continue
            val merchant = match.groupValues[1].trim()
            if (merchant.length >= 2) return cleanMerchantName(merchant)
        }
        return ""
    }

    private fun cleanMerchantName(raw: String): String {
        return raw
            .replace(Regex("""\b\d{4,}\b"""), "") // Remove long numbers
            .replace(Regex("""[^A-Za-z0-9\s\-&'.]"""), "")
            .trim()
            .take(40)
    }

    private fun extractBalance(sms: String): Double? {
        for (pattern in BALANCE_PATTERNS) {
            val match = pattern.find(sms) ?: continue
            val raw = match.groupValues[1].replace(",", "")
            return raw.toDoubleOrNull()
        }
        return null
    }

    fun categorize(merchant: String, sms: String = ""): ExpenseCategory {
        val combined = (merchant + " " + sms).lowercase()
        for ((category, keywords) in CATEGORY_KEYWORDS) {
            if (keywords.any { combined.contains(it) }) return category
        }
        return ExpenseCategory.OTHER
    }

    private fun computeHash(sms: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(sms.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
    }

    // ─── SAMPLE REGEX TEST PATTERNS ─────────────────────────────────────────────
    /**
     * Sample bank SMS formats this parser handles:
     *
     * SBI:
     *   "Your A/c XXXX1234 is debited for Rs.500.00 on 21/04/24. Info: UPI/Zomato. Avl Bal:Rs.12345.67"
     *
     * HDFC:
     *   "Rs 1,250.00 debited from HDFC Bank a/c XXXX1234 on 21-Apr-24. Info: POS at DMART MUMBAI. Avl bal: Rs 45,678.90"
     *
     * ICICI:
     *   "ICICI Bank: Rs 350.00 debited from ac XX1234 on 21-Apr-24 at SWIGGY TECHNOLOGIES. Avail Bal: Rs 23,456.78"
     *
     * AXIS:
     *   "Your Axis Bank a/c XXXX1234 has been debited with INR 2,500.00 on 21/04/24. UPI ref: Uber India"
     *
     * KOTAK:
     *   "INR 899.00 debited from Kotak a/c XXXX1234 on 21-Apr-24 at NETFLIX. Bal: INR 15,678.90"
     *
     * GENERIC CARD:
     *   "Transaction of Rs.1,200.00 using card ending 1234 at AMAZON INDIA on 21/04/2024"
     */
    val SAMPLE_SMS_FOR_TESTING = listOf(
        "Your A/c XXXX1234 is debited for Rs.500.00 on 21/04/24. Info: UPI/Zomato. Avl Bal:Rs.12345.67",
        "Rs 1,250.00 debited from HDFC Bank a/c XXXX1234 on 21-Apr-24. Info: POS at DMART MUMBAI. Avl bal: Rs 45,678.90",
        "ICICI Bank: Rs 350.00 debited from ac XX1234 on 21-Apr-24 at SWIGGY TECHNOLOGIES. Avail Bal: Rs 23,456.78",
        "Your Axis Bank a/c XXXX1234 has been debited with INR 2,500.00 on 21/04/24. UPI ref: Uber India",
        "INR 899.00 debited from Kotak a/c XXXX1234 on 21-Apr-24 at NETFLIX. Bal: INR 15,678.90",
        "Transaction of Rs.1,200.00 using card ending 1234 at AMAZON INDIA on 21/04/2024. Balance: Rs.34,500",
        "Dear Customer, your a/c XX1234 debited Rs.75.00 for Petrol at HP PETROL PUMP on 21Apr24. Avl Bal INR 8,234.00",
        "Your debit card ending 5678 used for Rs 150.00 at STARBUCKS CONNAUGHT PL on 21-04-2024 14:35:22. Balance: Rs 5,678.50"
    )
}
