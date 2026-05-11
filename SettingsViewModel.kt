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
    private val settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository,
    private val smsInboxScanner: SmsInboxScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        settingsRepository.settings
            .onEach { s -> _uiState.update { it.copy(settings = s) } }
            .launchIn(viewModelScope)
    }

    fun onNewSenderInput(v: String)    = _uiState.update { it.copy(newSenderInput = v) }
    fun onNewDebitKeyword(v: String)   = _uiState.update { it.copy(newDebitKeyword = v) }
    fun onNewCreditKeyword(v: String)  = _uiState.update { it.copy(newCreditKeyword = v) }

    fun addSender() {
        val v = _uiState.value.newSenderInput.trim()
        if (v.isBlank()) return
        save(_uiState.value.settings.copy(trustedSenders = _uiState.value.settings.trustedSenders + v))
        _uiState.update { it.copy(newSenderInput = "") }
    }

    fun removeSender(s: String) =
        save(_uiState.value.settings.copy(trustedSenders = _uiState.value.settings.trustedSenders - s))

    fun addDebitKeyword() {
        val v = _uiState.value.newDebitKeyword.trim().lowercase()
        if (v.isBlank()) return
        save(_uiState.value.settings.copy(debitKeywords = _uiState.value.settings.debitKeywords + v))
        _uiState.update { it.copy(newDebitKeyword = "") }
    }

    fun removeDebitKeyword(k: String) =
        save(_uiState.value.settings.copy(debitKeywords = _uiState.value.settings.debitKeywords - k))

    fun addCreditKeyword() {
        val v = _uiState.value.newCreditKeyword.trim().lowercase()
        if (v.isBlank()) return
        save(_uiState.value.settings.copy(creditKeywords = _uiState.value.settings.creditKeywords + v))
        _uiState.update { it.copy(newCreditKeyword = "") }
    }

    fun removeCreditKeyword(k: String) =
        save(_uiState.value.settings.copy(creditKeywords = _uiState.value.settings.creditKeywords - k))

    fun setCurrency(c: String)          = save(_uiState.value.settings.copy(currencySymbol = c.uppercase()))
    fun toggleAutoScan(on: Boolean)     = save(_uiState.value.settings.copy(autoScanEnabled = on))
    fun setMonthStartDay(day: Int)      = save(_uiState.value.settings.copy(monthStartDay = day.coerceIn(1, 28)))

    private fun save(s: AppSettings) { viewModelScope.launch { settingsRepository.saveSettings(s) } }

    fun scanInbox() {
        if (_uiState.value.scanProgress.isScanning) return
        _uiState.update { it.copy(scanProgress = ScanProgress(isScanning = true)) }
        viewModelScope.launch {
            try {
                val settings = _uiState.value.settings
                val config = SmsParser.ParserConfig(
                    currencySymbol = settings.currencySymbol,
                    trustedSenders = settings.trustedSenders,
                    debitKeywords  = settings.debitKeywords,
                    creditKeywords = settings.creditKeywords
                )
                val rawList = smsInboxScanner.scanInbox(settings) { scanned, total ->
                    _uiState.update { it.copy(scanProgress = it.scanProgress.copy(scanned = scanned, total = total)) }
                }
                var imported = 0
                rawList.forEach { raw ->
                    val parsed = SmsParser.parse(raw.body, raw.sender, raw.timestamp, config)
                    if (parsed != null && transactionRepository.insertFromSms(parsed) != null) imported++
                }
                _uiState.update {
                    it.copy(scanProgress = ScanProgress(done = true, imported = imported,
                        scanned = rawList.size, total = rawList.size))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(scanProgress = ScanProgress(done = true, error = e.message)) }
            }
        }
    }

    fun dismissScanResult() = _uiState.update { it.copy(scanProgress = ScanProgress()) }
}
