package com.expensetracker.service

import android.content.Context
import android.provider.Telephony
import com.expensetracker.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class RawSms(val body: String, val sender: String, val timestamp: LocalDateTime)

@Singleton
class SmsInboxScanner @Inject constructor(@ApplicationContext private val context: Context) {

    fun scanInbox(settings: AppSettings, onProgress: (Int, Int) -> Unit = { _, _ -> }): List<RawSms> {
        val results = mutableListOf<RawSms>()
        val config = SmsParser.ParserConfig(
            currencySymbol = settings.currencySymbol,
            trustedSenders = settings.trustedSenders,
            debitKeywords  = settings.debitKeywords,
            creditKeywords = settings.creditKeywords
        )
        val cursor = try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.BODY, Telephony.Sms.ADDRESS, Telephony.Sms.DATE),
                null, null, "${Telephony.Sms.DATE} DESC"
            )
        } catch (_: SecurityException) { null } ?: return results

        cursor.use { c ->
            val total  = c.count
            var done   = 0
            val bodyI  = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val addrI  = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val dateI  = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (c.moveToNext()) {
                done++
                onProgress(done, total)
                val body   = c.getString(bodyI) ?: continue
                val sender = c.getString(addrI) ?: ""
                val dt     = LocalDateTime.ofInstant(Instant.ofEpochMilli(c.getLong(dateI)), ZoneId.systemDefault())
                if (!SmsParser.isTrustedSender(sender, config)) continue
                if (!SmsParser.isDebitTransaction(body, config)) continue
                results.add(RawSms(body, sender, dt))
            }
        }
        return results
    }
}
