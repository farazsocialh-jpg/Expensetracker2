package com.expensetracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.expensetracker.data.repository.SettingsRepository
import com.expensetracker.data.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {
    @Inject lateinit var txRepo: TransactionRepository
    @Inject lateinit var settingsRepo: SettingsRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        scope.launch {
            val s = settingsRepo.settings.first()
            if (!s.autoScanEnabled) return@launch
            val config = SmsParser.ParserConfig(
                currencySymbol = s.currencySymbol,
                trustedSenders = s.trustedSenders,
                debitKeywords  = s.debitKeywords,
                creditKeywords = s.creditKeywords
            )
            messages.forEach { msg ->
                val body   = msg.messageBody ?: return@forEach
                val sender = msg.originatingAddress ?: ""
                if (!SmsParser.isTrustedSender(sender, config)) return@forEach
                val parsed = SmsParser.parse(body, sender, LocalDateTime.now(), config) ?: return@forEach
                txRepo.insertFromSms(parsed)
            }
        }
    }
}
