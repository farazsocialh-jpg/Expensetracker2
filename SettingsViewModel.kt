package com.expensetracker.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.SettingsRepository
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.domain.model.AppSettings
import com.expensetracker.service.SmsInboxScanner
import com.expensetracker.service.SmsParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanProgress(
    val isScanning: Boolean = false,
    val scanned: Int = 0,
    val total: Int = 0,
    val imported: Int = 0,
    val done: Boolean = false,
    val error: String? = null
)

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val scanProgress: ScanProgress = ScanProgress(),
    val newSenderInput: String = "",
    val newDebitKeyword: String = "",
    val newCreditKeyword: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val txRepo: TransactionRepository,
    private val scanner: SmsInboxScanner
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        settingsRepo.settings.onEach { s -> _state.update { it.copy(settings = s) } }.launchIn(viewModelScope)
    }

    fun onNewSenderInput(v: String)   = _state.update { it.copy(newSenderInput = v) }
    fun onNewDebitKeyword(v: String)  = _state.update { it.copy(newDebitKeyword = v) }
    fun onNewCreditKeyword(v: String) = _state.update { it.copy(newCreditKeyword = v) }

    fun addSender() {
        val v = _state.value.newSenderInput.trim().ifBlank { return }
        save(_state.value.settings.copy(trustedSenders = _state.value.settings.trustedSenders + v))
        _state.update { it.copy(newSenderInput = "") }
    }
    fun removeSender(s: String)        = save(_state.value.settings.copy(trustedSenders = _state.value.settings.trustedSenders - s))
    fun addDebitKeyword() {
        val v = _state.value.newDebitKeyword.trim().lowercase().ifBlank { return }
        save(_state.value.settings.copy(debitKeywords = _state.value.settings.debitKeywords + v))
        _state.update { it.copy(newDebitKeyword = "") }
    }
    fun removeDebitKeyword(k: String)  = save(_state.value.settings.copy(debitKeywords = _state.value.settings.debitKeywords - k))
    fun addCreditKeyword() {
        val v = _state.value.newCreditKeyword.trim().lowercase().ifBlank { return }
        save(_state.value.settings.copy(creditKeywords = _state.value.settings.creditKeywords + v))
        _state.update { it.copy(newCreditKeyword = "") }
    }
    fun removeCreditKeyword(k: String) = save(_state.value.settings.copy(creditKeywords = _state.value.settings.creditKeywords - k))
    fun setCurrency(c: String)         = save(_state.value.settings.copy(currencySymbol = c.uppercase()))
    fun toggleAutoScan(on: Boolean)    = save(_state.value.settings.copy(autoScanEnabled = on))
    fun setMonthStartDay(d: Int)       = save(_state.value.settings.copy(monthStartDay = d.coerceIn(1, 28)))
    fun toggleDarkTheme(on: Boolean)   = save(_state.value.settings.copy(darkTheme = on))
    fun toggleAmoled(on: Boolean)      = save(_state.value.settings.copy(amoledTheme = on))
    fun toggleHideBalances(on: Boolean)= save(_state.value.settings.copy(hideBalances = on))
    private fun save(s: AppSettings) { viewModelScope.launch { settingsRepo.saveSettings(s) } }

    fun scanInbox() {
        if (_state.value.scanProgress.isScanning) return
        _state.update { it.copy(scanProgress = ScanProgress(isScanning = true)) }
        viewModelScope.launch {
            try {
                val s = _state.value.settings
                val config = SmsParser.ParserConfig(s.currencySymbol, s.trustedSenders, s.debitKeywords, s.creditKeywords)
                val rawList = scanner.scanInbox(s) { scanned, total ->
                    _state.update { it.copy(scanProgress = it.scanProgress.copy(scanned = scanned, total = total)) }
                }
                var imported = 0
                rawList.forEach { raw ->
                    val parsed = SmsParser.parse(raw.body, raw.sender, raw.timestamp, config)
                    if (parsed != null && txRepo.insertFromSms(parsed) != null) imported++
                }
                _state.update { it.copy(scanProgress = ScanProgress(done = true, imported = imported, scanned = rawList.size, total = rawList.size)) }
            } catch (e: Exception) {
                _state.update { it.copy(scanProgress = ScanProgress(done = true, error = e.message)) }
            }
        }
    }

    fun dismissScanResult() = _state.update { it.copy(scanProgress = ScanProgress()) }
}
