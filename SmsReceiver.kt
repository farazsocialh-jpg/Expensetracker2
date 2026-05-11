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

    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        scope.launch {
            val settings = settingsRepository.settings.first()
            if (!settings.autoScanEnabled) return@launch

            val config = SmsParser.ParserConfig(
                currencySymbol = settings.currencySymbol,
                trustedSenders = settings.trustedSenders,
                debitKeywords = settings.debitKeywords,
                creditKeywords = settings.creditKeywords
            )

            messages.forEach { smsMessage ->
                val body = smsMessage.messageBody ?: return@forEach
                val sender = smsMessage.originatingAddress ?: ""

                // Only process from trusted senders
                if (!SmsParser.isTrustedSender(sender, config)) return@forEach

                val parsed = SmsParser.parse(body, sender, LocalDateTime.now(), config)
                    ?: return@forEach

                transactionRepository.insertFromSms(parsed)
            }
        }
    }
}
