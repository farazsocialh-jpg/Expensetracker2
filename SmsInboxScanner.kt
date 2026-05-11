package com.expensetracker.service

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.expensetracker.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class RawSms(
    val body: String,
    val sender: String,
    val timestamp: LocalDateTime
)

@Singleton
class SmsInboxScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Reads ALL SMS from the inbox, filters by trusted senders if configured,
     * then returns those that look like debit transactions.
     *
     * This is the "scan existing messages" feature — called manually by the user.
     * We only READ messages, never send or modify them.
     * All processing is 100% on-device.
     */
    fun scanInbox(
        settings: AppSettings,
        onProgress: (scanned: Int, total: Int) -> Unit = { _, _ -> }
    ): List<RawSms> {
        val results = mutableListOf<RawSms>()
        val config = SmsParser.ParserConfig(
            currencySymbol = settings.currencySymbol,
            trustedSenders = settings.trustedSenders,
            debitKeywords = settings.debitKeywords,
            creditKeywords = settings.creditKeywords
        )

        val uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.BODY,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE
        )

        val cursor: Cursor? = try {
            context.contentResolver.query(
                uri, projection, null, null,
                "${Telephony.Sms.DATE} DESC"
            )
        } catch (e: SecurityException) {
            null
        }

        cursor?.use { c ->
            val total = c.count
            var scanned = 0
            val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val addrIdx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (c.moveToNext()) {
                scanned++
                onProgress(scanned, total)

                val body = c.getString(bodyIdx) ?: continue
                val sender = c.getString(addrIdx) ?: ""
                val dateMs = c.getLong(dateIdx)
                val dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(dateMs), ZoneId.systemDefault()
                )

                // Filter by trusted sender if list is non-empty
                if (!SmsParser.isTrustedSender(sender, config)) continue

                // Filter to debit transactions only
                if (!SmsParser.isDebitTransaction(body, config)) continue

                results.add(RawSms(body, sender, dateTime))
            }
        }

        return results
    }
}
