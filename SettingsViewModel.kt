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
            .onEach { settings -> _uiState.update { it.copy(settings = settings) } }
            .launchIn(viewModelScope)
    }

    fun onNewSenderInput(v: String) = _uiState.update { it.copy(newSenderInput = v) }
    fun onNewDebitKeyword(v: String) = _uiState.update { it.copy(newDebitKeyword = v) }
    fun onNewCreditKeyword(v: String) = _uiState.update { it.copy(newCreditKeyword = v) }

    fun addSender() {
        val v = _uiState.value.newSenderInput.trim()
        if (v.isBlank()) return
        val updated = _uiState.value.settings.trustedSenders + v
        saveSettings(_uiState.value.settings.copy(trustedSenders = updated))
        _uiState.update { it.copy(newSenderInput = "") }
    }

    fun removeSender(s: String) {
        val updated = _uiState.value.settings.trustedSenders - s
        saveSettings(_uiState.value.settings.copy(trustedSenders = updated))
    }

    fun addDebitKeyword() {
        val v = _uiState.value.newDebitKeyword.trim().lowercase()
        if (v.isBlank()) return
        val updated = _uiState.value.settings.debitKeywords + v
        saveSettings(_uiState.value.settings.copy(debitKeywords = updated))
        _uiState.update { it.copy(newDebitKeyword = "") }
    }

    fun removeDebitKeyword(k: String) {
        val updated = _uiState.value.settings.debitKeywords - k
        saveSettings(_uiState.value.settings.copy(debitKeywords = updated))
    }

    fun addCreditKeyword() {
        val v = _uiState.value.newCreditKeyword.trim().lowercase()
        if (v.isBlank()) return
        val updated = _uiState.value.settings.creditKeywords + v
        saveSettings(_uiState.value.settings.copy(creditKeywords = updated))
        _uiState.update { it.copy(newCreditKeyword = "") }
    }

    fun removeCreditKeyword(k: String) {
        val updated = _uiState.value.settings.creditKeywords - k
        saveSettings(_uiState.value.settings.copy(creditKeywords = updated))
    }

    fun setCurrency(c: String) = saveSettings(_uiState.value.settings.copy(currencySymbol = c.uppercase()))

    fun toggleAutoScan(enabled: Boolean) = saveSettings(_uiState.value.settings.copy(autoScanEnabled = enabled))

    private fun saveSettings(s: AppSettings) {
        viewModelScope.launch { settingsRepository.saveSettings(s) }
    }

    /** Scans the full SMS inbox and imports matching transactions */
    fun scanInbox() {
        if (_uiState.value.scanProgress.isScanning) return
        _uiState.update { it.copy(scanProgress = ScanProgress(isScanning = true)) }

        viewModelScope.launch {
            try {
                val settings = _uiState.value.settings
                val config = SmsParser.ParserConfig(
                    currencySymbol = settings.currencySymbol,
                    trustedSenders = settings.trustedSenders,
                    debitKeywords = settings.debitKeywords,
                    creditKeywords = settings.creditKeywords
                )

                val rawList = smsInboxScanner.scanInbox(settings) { scanned, total ->
                    _uiState.update {
                        it.copy(scanProgress = it.scanProgress.copy(scanned = scanned, total = total))
                    }
                }

                var imported = 0
                rawList.forEach { raw ->
                    val parsed = SmsParser.parse(raw.body, raw.sender, raw.timestamp, config)
                    if (parsed != null) {
                        val result = transactionRepository.insertFromSms(parsed)
                        if (result != null) imported++
                    }
                }

                _uiState.update {
                    it.copy(scanProgress = ScanProgress(done = true, imported = imported,
                        scanned = rawList.size, total = rawList.size))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(scanProgress = ScanProgress(done = true, error = e.message))
                }
            }
        }
    }

    fun dismissScanResult() {
        _uiState.update { it.copy(scanProgress = ScanProgress()) }
    }
}
