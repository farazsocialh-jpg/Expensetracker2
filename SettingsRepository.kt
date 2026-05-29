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
private val Context.dataStore:DataStore<Preferences> by preferencesDataStore(name="app_settings")
@Singleton class SettingsRepository @Inject constructor(@ApplicationContext private val ctx:Context){
    private object K{val CUR=stringPreferencesKey("currency");val SND=stringPreferencesKey("senders");val DKW=stringPreferencesKey("debit_kw");val CKW=stringPreferencesKey("credit_kw");val AS=booleanPreferencesKey("auto_scan");val MSD=intPreferencesKey("month_start");val DRK=booleanPreferencesKey("dark");val AML=booleanPreferencesKey("amoled");val HB=booleanPreferencesKey("hide_bal")}
    val settings:Flow<AppSettings>=ctx.dataStore.data.map{p->AppSettings(currencySymbol=p[K.CUR]?:"QAR",trustedSenders=p[K.SND]?.split("|")?.filter{it.isNotBlank()}?:listOf("QNB","DOHA BANK","CBQ","MASRAF","HSBC","QIIB","DUKHAN","AHLIBANK","OOREDOO","VODAFONE"),debitKeywords=p[K.DKW]?.split("|")?.filter{it.isNotBlank()}?:listOf("debited","payment","purchase","withdrawn","charged","paid","debit"),creditKeywords=p[K.CKW]?.split("|")?.filter{it.isNotBlank()}?:listOf("credited","received","refund","salary"),autoScanEnabled=p[K.AS]?:true,monthStartDay=p[K.MSD]?:1,darkTheme=p[K.DRK]?:true,amoledTheme=p[K.AML]?:false,hideBalances=p[K.HB]?:false)}
    suspend fun saveSettings(s:AppSettings){ctx.dataStore.edit{p->p[K.CUR]=s.currencySymbol;p[K.SND]=s.trustedSenders.joinToString("|");p[K.DKW]=s.debitKeywords.joinToString("|");p[K.CKW]=s.creditKeywords.joinToString("|");p[K.AS]=s.autoScanEnabled;p[K.MSD]=s.monthStartDay;p[K.DRK]=s.darkTheme;p[K.AML]=s.amoledTheme;p[K.HB]=s.hideBalances}}
    suspend fun getCurrent():AppSettings=settings.first()
}
