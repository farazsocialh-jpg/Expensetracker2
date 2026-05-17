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
    private object K {
        val CURRENCY        = stringPreferencesKey("currency")
        val SENDERS         = stringPreferencesKey("trusted_senders")
        val DEBIT_KW        = stringPreferencesKey("debit_keywords")
        val CREDIT_KW       = stringPreferencesKey("credit_keywords")
        val AUTO_SCAN       = booleanPreferencesKey("auto_scan")
        val MONTH_START     = intPreferencesKey("month_start_day")
        val DARK_THEME      = booleanPreferencesKey("dark_theme")
        val AMOLED          = booleanPreferencesKey("amoled_theme")
        val HIDE_BALANCES   = booleanPreferencesKey("hide_balances")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            currencySymbol  = p[K.CURRENCY] ?: "QAR",
            trustedSenders  = p[K.SENDERS]?.split("|")?.filter { it.isNotBlank() }
                ?: listOf("QNB","DOHA BANK","CBQ","MASRAF","HSBC","QIIB","DUKHAN","AHLIBANK","OOREDOO","VODAFONE"),
            debitKeywords   = p[K.DEBIT_KW]?.split("|")?.filter { it.isNotBlank() }
                ?: listOf("debited","payment","purchase","withdrawn","charged","paid","debit"),
            creditKeywords  = p[K.CREDIT_KW]?.split("|")?.filter { it.isNotBlank() }
                ?: listOf("credited","received","refund","salary"),
            autoScanEnabled = p[K.AUTO_SCAN] ?: true,
            monthStartDay   = p[K.MONTH_START] ?: 1,
            darkTheme       = p[K.DARK_THEME] ?: true,
            amoledTheme     = p[K.AMOLED] ?: false,
            hideBalances    = p[K.HIDE_BALANCES] ?: false,
            onboardingDone  = p[K.ONBOARDING_DONE] ?: false
        )
    }

    suspend fun saveSettings(s: AppSettings) {
        context.dataStore.edit { p ->
            p[K.CURRENCY]        = s.currencySymbol
            p[K.SENDERS]         = s.trustedSenders.joinToString("|")
            p[K.DEBIT_KW]        = s.debitKeywords.joinToString("|")
            p[K.CREDIT_KW]       = s.creditKeywords.joinToString("|")
            p[K.AUTO_SCAN]       = s.autoScanEnabled
            p[K.MONTH_START]     = s.monthStartDay
            p[K.DARK_THEME]      = s.darkTheme
            p[K.AMOLED]          = s.amoledTheme
            p[K.HIDE_BALANCES]   = s.hideBalances
            p[K.ONBOARDING_DONE] = s.onboardingDone
        }
    }

    suspend fun getCurrent(): AppSettings = settings.first()
}
