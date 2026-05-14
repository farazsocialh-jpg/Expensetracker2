package com.expensetracker.service

import com.expensetracker.domain.model.ExpenseCategory
import java.security.MessageDigest
import java.time.LocalDateTime

object SmsParser {

    data class ParsedTransaction(
        val amount: Double,
        val merchant: String,
        val balance: Double?,
        val category: ExpenseCategory,
        val dateTime: LocalDateTime,
        val rawSms: String,
        val smsHash: String,
        val sender: String = "",
        val confidenceScore: Float = 0.8f,
        val isRefund: Boolean = false,
        val isTransfer: Boolean = false
    )

    data class ParserConfig(
        val currencySymbol: String = "QAR",
        val trustedSenders: List<String> = emptyList(),
        val debitKeywords: List<String> = emptyList(),
        val creditKeywords: List<String> = emptyList()
    )

    private val DEFAULT_DEBIT = listOf(
        "debited","deducted","spent","paid","payment","purchase",
        "withdrawn","charged","debit","pos ","txn","transaction","transferred","sent"
    )
    private val DEFAULT_CREDIT = listOf(
        "credited","received","credit","deposited","added","refund","cashback","salary","neft received"
    )
    private val OTP_WORDS = listOf(
        "otp","one time","verification code","do not share","expires in","login code"
    )
    private val REFUND_WORDS = listOf("refund","reversal","reversed","cashback")
    private val TRANSFER_WORDS = listOf("transfer","neft","imps","rtgs","sent to","transferred to")

    private fun amountPatterns(currency: String) = listOf(
        Regex("""(?:${Regex.escape(currency)}|QAR|SAR|AED|KWD|BHD|OMR|Rs\.?|INR|USD|\$|EUR|GBP)\s*([0-9,]+(?:\.[0-9]{1,3})?)""", RegexOption.IGNORE_CASE),
        Regex("""([0-9,]+(?:\.[0-9]{1,3})?)\s*(?:${Regex.escape(currency)}|QAR|SAR|AED|KWD)""", RegexOption.IGNORE_CASE),
        Regex("""(?:amount|amt)\s*(?:of|:)?\s*(?:[A-Z]{2,4})?\s*([0-9,]+(?:\.[0-9]{1,3})?)""", RegexOption.IGNORE_CASE),
        Regex("""(?:debited|paid|spent|charged)\s+(?:with\s+)?(?:[A-Z]{2,4}\s+)?([0-9,]+(?:\.[0-9]{1,3})?)""", RegexOption.IGNORE_CASE)
    )

    private val BALANCE_PATTERNS = listOf(
        Regex("""(?:avl|avail|available|bal|balance)\s*(?:bal|balance|amt)?\s*(?:is|:|-|=)?\s*(?:[A-Z]{2,4})?\s*([0-9,]+(?:\.[0-9]{1,3})?)""", RegexOption.IGNORE_CASE),
        Regex("""(?:[A-Z]{2,4})\s*([0-9,]+(?:\.[0-9]{1,3})?)\s*(?:available|avl|bal|balance)""", RegexOption.IGNORE_CASE)
    )

    private val MERCHANT_PATTERNS = listOf(
        Regex("""(?:at|to|At|To|merchant[:\s]+)\s+([A-Za-z0-9 \-&'.\/]+?)(?:\s+on\s+|\s+ref|\s+txn|\s*\.|,|\s{2,}|$)"""),
        Regex("""(?:purchase|payment|paid)\s+(?:at|to)\s+([A-Za-z0-9 \-&'.]+?)(?:\s+on|\.|,|$)""", RegexOption.IGNORE_CASE),
        Regex("""POS\s+([A-Za-z0-9 \-&'.]+?)(?:\s+\d|\.|,|$)"""),
        Regex("""(?:towards|for)\s+([A-Za-z0-9 \-&'.]+?)(?:\s+on|\s+ref|\.|,|$)""", RegexOption.IGNORE_CASE)
    )

    val CATEGORY_KEYWORDS: Map<ExpenseCategory, List<String>> = mapOf(
        ExpenseCategory.FOOD to listOf(
            "restaurant","cafe","coffee","pizza","burger","kfc","mcdonalds","subway","food",
            "dining","eatery","biryani","shawarma","zomato","talabat","starbucks","tim hortons",
            "hardees","popeyes","wendys","dominos","papa johns","careem food","noon food",
            "shake shack","five guys","texas roadhouse","applebees","chilis"
        ),
        ExpenseCategory.GROCERY to listOf(
            "lulu","carrefour","safari","al meera","monoprix","spinneys","waitrose",
            "family food","geant","hypermarket","supermarket","grocery","fresh","spar","grand mart",
            "al khor","megamart","al noor"
        ),
        ExpenseCategory.TRANSPORT to listOf(
            "uber","careem","taxi","metro","mowasalat","bus","karwa",
            "parking","salik","toll","lyft","bolt","indrive","limousine"
        ),
        ExpenseCategory.FUEL to listOf(
            "woqod","fuel","petrol","gas station","filling station",
            "shell","total","q8","qatar fuel","petroq","caltex"
        ),
        ExpenseCategory.SHOPPING to listOf(
            "amazon","noon","namshi","ounass","zara","h&m","ikea",
            "centrepoint","max fashion","splash","landmark","ace hardware",
            "home centre","pottery barn","virgin megastore","plug","istore",
            "apple store","samsung","lulu electronics","sharafdg","sharaf dg"
        ),
        ExpenseCategory.BILLS to listOf(
            "ooredoo","vodafone qatar","kahramaa","electricity","water",
            "utility","internet","broadband","telecom","stc",
            "du ","etisalat","phone bill","mobile bill","postpaid","recharge"
        ),
        ExpenseCategory.SUBSCRIPTION to listOf(
            "netflix","spotify","apple","google play","microsoft","adobe","aws",
            "shahid","osn","beinsport","bein sport","disney","hotstar","amazon prime",
            "youtube premium","icloud","dropbox","linkedin","zoom","slack"
        ),
        ExpenseCategory.HEALTH to listOf(
            "hospital","clinic","pharmacy","medical","doctor","dental",
            "hamad","aster","american hospital","sidra","qmc","al ahli",
            "medicine","optician","lab","diagnostic","health","wellness"
        ),
        ExpenseCategory.ENTERTAINMENT to listOf(
            "cinema","movie","vox","reel","muvi","bowling","escape room",
            "theme park","aqua","gaming","playstation","xbox","steam",
            "concert","event","ticket","entertainment","adventure"
        ),
        ExpenseCategory.EDUCATION to listOf(
            "school","university","tuition","course","education","training",
            "books","stationery","qatar university","cmu","nu-q","hec",
            "british council","ielts","udemy","coursera"
        ),
        ExpenseCategory.TRAVEL to listOf(
            "qatar airways","airline","flight","airport","hotel","booking",
            "airbnb","holiday","travel","resort","marriott","hilton",
            "hyatt","rotana","intercontinental","expedia","agoda","makemytrip"
        ),
        ExpenseCategory.SALARY to listOf("salary","payroll","wage","income","monthly pay"),
        ExpenseCategory.TRANSFER to listOf("transfer","neft","imps","rtgs","sent to","wire")
    )

    fun getCategoryKeywords(cat: ExpenseCategory): List<String> = CATEGORY_KEYWORDS[cat] ?: emptyList()

    fun isDebitTransaction(sms: String, config: ParserConfig = ParserConfig()): Boolean {
        val lower = sms.lowercase()
        if (OTP_WORDS.any { lower.contains(it) }) return false
        val allDebit  = DEFAULT_DEBIT  + config.debitKeywords
        val allCredit = DEFAULT_CREDIT + config.creditKeywords
        val hasDebit  = allDebit.any  { lower.contains(it) }
        val hasCredit = allCredit.any { lower.contains(it) }
        if (hasCredit && !hasDebit) return false
        return hasDebit && amountPatterns(config.currencySymbol).any { it.containsMatchIn(sms) }
    }

    fun isTrustedSender(sender: String, config: ParserConfig): Boolean {
        if (config.trustedSenders.isEmpty()) return true
        val lower = sender.lowercase()
        return config.trustedSenders.any { lower.contains(it.lowercase()) }
    }

    fun parse(
        sms: String,
        sender: String = "",
        receivedAt: LocalDateTime = LocalDateTime.now(),
        config: ParserConfig = ParserConfig()
    ): ParsedTransaction? {
        if (!isDebitTransaction(sms, config)) return null
        val patterns = amountPatterns(config.currencySymbol)
        val amount   = extractAmount(sms, patterns) ?: return null
        val merchant = extractMerchant(sms).ifBlank { sender.ifBlank { "Unknown" } }
        val balance  = extractBalance(sms)
        val lower    = sms.lowercase()
        val isRefund = REFUND_WORDS.any { lower.contains(it) }
        val isTransfer = TRANSFER_WORDS.any { lower.contains(it) }
        val category = categorize(merchant, sms)
        val confidence = computeConfidence(sms, merchant, category)

        return ParsedTransaction(
            amount = amount, merchant = merchant, balance = balance,
            category = category, dateTime = receivedAt, rawSms = sms,
            smsHash = hash(sms), sender = sender,
            confidenceScore = confidence, isRefund = isRefund, isTransfer = isTransfer
        )
    }

    fun categorize(merchant: String, sms: String = ""): ExpenseCategory {
        val combined = (merchant + " " + sms).lowercase()
        for ((cat, kws) in CATEGORY_KEYWORDS) {
            if (kws.any { combined.contains(it) }) return cat
        }
        return ExpenseCategory.OTHER
    }

    private fun computeConfidence(sms: String, merchant: String, category: ExpenseCategory): Float {
        var score = 0.5f
        val lower = sms.lowercase()
        if (amountPatterns("QAR").first().containsMatchIn(sms)) score += 0.2f
        if (merchant != "Unknown" && merchant.length > 3) score += 0.15f
        if (CATEGORY_KEYWORDS[category]?.any { lower.contains(it) } == true) score += 0.15f
        return score.coerceIn(0f, 1f)
    }

    private fun extractAmount(sms: String, patterns: List<Regex>): Double? {
        for (p in patterns) {
            val v = p.find(sms)?.groupValues?.get(1)?.replace(",","")?.toDoubleOrNull()
            if (v != null && v > 0 && v < 10_000_000) return v
        }
        return null
    }

    private fun extractMerchant(sms: String): String {
        for (p in MERCHANT_PATTERNS) {
            val m = p.find(sms)?.groupValues?.get(1)?.trim() ?: continue
            if (m.length >= 2) return m.replace(Regex("""\b\d{4,}\b"""), "").trim().take(50)
        }
        return ""
    }

    private fun extractBalance(sms: String): Double? {
        for (p in BALANCE_PATTERNS) {
            val v = p.find(sms)?.groupValues?.get(1)?.replace(",","")?.toDoubleOrNull()
            if (v != null) return v
        }
        return null
    }

    fun hash(sms: String): String =
        MessageDigest.getInstance("SHA-256").digest(sms.toByteArray())
            .joinToString("") { "%02x".format(it) }.take(16)
}
