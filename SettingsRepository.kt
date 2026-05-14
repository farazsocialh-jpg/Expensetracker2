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
        val DARK_THEME      = booleanPreferencesKey("dark_theme")
        val AMOLED_THEME    = booleanPreferencesKey("amoled_theme")
        val HIDE_BALANCES   = booleanPreferencesKey("hide_balances")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val COUNTRY         = stringPreferencesKey("country")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            currencySymbol  = p[Keys.CURRENCY] ?: "QAR",
            trustedSenders  = p[Keys.TRUSTED_SENDERS]?.split("|")?.filter { it.isNotBlank() }
                ?: listOf("QNB","DOHA BANK","CBQ","MASRAF","HSBC","QIIB","DUKHAN","AHLIBANK","OOREDOO","VODAFONE","COMMERCIAL BANK"),
            debitKeywords   = p[Keys.DEBIT_KEYWORDS]?.split("|")?.filter { it.isNotBlank() }
                ?: listOf("debited","payment","purchase","withdrawn","charged","paid","debit"),
            creditKeywords  = p[Keys.CREDIT_KEYWORDS]?.split("|")?.filter { it.isNotBlank() }
                ?: listOf("credited","received","refund","salary"),
            autoScanEnabled = p[Keys.AUTO_SCAN] ?: true,
            monthStartDay   = p[Keys.MONTH_START_DAY] ?: 1,
            darkTheme       = p[Keys.DARK_THEME] ?: true,
            amoledTheme     = p[Keys.AMOLED_THEME] ?: false,
            hideBalances    = p[Keys.HIDE_BALANCES] ?: false,
            onboardingDone  = p[Keys.ONBOARDING_DONE] ?: false,
            country         = p[Keys.COUNTRY] ?: "QA"
        )
    }

    suspend fun saveSettings(s: AppSettings) {
        context.dataStore.edit { p ->
            p[Keys.CURRENCY]        = s.currencySymbol
            p[Keys.TRUSTED_SENDERS] = s.trustedSenders.joinToString("|")
            p[Keys.DEBIT_KEYWORDS]  = s.debitKeywords.joinToString("|")
            p[Keys.CREDIT_KEYWORDS] = s.creditKeywords.joinToString("|")
            p[Keys.AUTO_SCAN]       = s.autoScanEnabled
            p[Keys.MONTH_START_DAY] = s.monthStartDay
            p[Keys.DARK_THEME]      = s.darkTheme
            p[Keys.AMOLED_THEME]    = s.amoledTheme
            p[Keys.HIDE_BALANCES]   = s.hideBalances
            p[Keys.ONBOARDING_DONE] = s.onboardingDone
            p[Keys.COUNTRY]         = s.country
        }
    }

    suspend fun first(): AppSettings = settings.first()
}
