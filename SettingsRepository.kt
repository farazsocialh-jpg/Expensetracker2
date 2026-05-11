package com.expensetracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.expensetracker.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val CURRENCY        = stringPreferencesKey("currency")
        val TRUSTED_SENDERS = stringPreferencesKey("trusted_senders")
        val DEBIT_KEYWORDS  = stringPreferencesKey("debit_keywords")
        val CREDIT_KEYWORDS = stringPreferencesKey("credit_keywords")
        val AUTO_SCAN       = booleanPreferencesKey("auto_scan")
        val MONTH_START_DAY = intPreferencesKey("month_start_day")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            currencySymbol  = prefs[Keys.CURRENCY] ?: "QAR",
            trustedSenders  = prefs[Keys.TRUSTED_SENDERS]?.split("|")?.filter { it.isNotBlank() }
                ?: listOf("QNB", "DOHA BANK", "CBQ", "MASRAF", "HSBC", "QIIB", "DUKHAN", "AHLIBANK"),
            debitKeywords   = prefs[Keys.DEBIT_KEYWORDS]?.split("|")?.filter { it.isNotBlank() }
                ?: listOf("debited", "payment", "purchase", "withdrawn", "charged"),
            creditKeywords  = prefs[Keys.CREDIT_KEYWORDS]?.split("|")?.filter { it.isNotBlank() }
                ?: listOf("credited", "received", "refund"),
            autoScanEnabled = prefs[Keys.AUTO_SCAN] ?: true,
            monthStartDay   = prefs[Keys.MONTH_START_DAY] ?: 1
        )
    }

    suspend fun saveSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CURRENCY]        = settings.currencySymbol
            prefs[Keys.TRUSTED_SENDERS] = settings.trustedSenders.joinToString("|")
            prefs[Keys.DEBIT_KEYWORDS]  = settings.debitKeywords.joinToString("|")
            prefs[Keys.CREDIT_KEYWORDS] = settings.creditKeywords.joinToString("|")
            prefs[Keys.AUTO_SCAN]       = settings.autoScanEnabled
            prefs[Keys.MONTH_START_DAY] = settings.monthStartDay
        }
    }

    suspend fun first(): AppSettings = settings.first()
}
