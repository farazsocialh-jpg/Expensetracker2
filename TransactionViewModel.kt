package com.expensetracker.presentation.transactions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.SettingsRepository
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.domain.model.ExpenseCategory
import com.expensetracker.domain.model.Transaction
import com.expensetracker.service.SmsParser
import com.expensetracker.utils.CsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

enum class DateRangePreset { ALL, TODAY, THIS_WEEK, THIS_MONTH, LAST_MONTH, CUSTOM }

data class TransactionFilter(
    val category: ExpenseCategory? = null,
    val cardNumber: String? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val searchQuery: String = "",
    val datePreset: DateRangePreset = DateRangePreset.ALL
)

data class TransactionUiState(
    val transactions: List<Transaction> = emptyList(),
    val filter: TransactionFilter = TransactionFilter(),
    val selectedIds: Set<Long> = emptySet(),
    val availableCards: List<String> = emptyList(),
    val isSelectionMode: Boolean = false,
    val showAddDialog: Boolean = false,
    val editingTransaction: Transaction? = null,
    val detailTransaction: Transaction? = null,
    val showCustomDatePicker: Boolean = false,
    val monthStartDay: Int = 1  // user-configurable month start day
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(TransactionFilter())
    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    init {
        // Load month start day from settings
        settingsRepository.settings
            .onEach { s -> _uiState.update { it.copy(monthStartDay = s.monthStartDay) } }
            .launchIn(viewModelScope)

        // Reactive transaction list
        _filter.flatMapLatest { filter ->
            val monthStartDay = _uiState.value.monthStartDay
            val (start, end) = resolveDateRange(filter, monthStartDay)
            when {
                filter.searchQuery.isNotBlank() ->
                    repository.searchTransactions(filter.searchQuery)
                filter.cardNumber != null ->
                    repository.getTransactionsByCard(filter.cardNumber)
                filter.category != null && start != null && end != null ->
                    repository.getTransactionsByDateRange(start, end)
                        .map { list -> list.filter { it.category == filter.category } }
                filter.category != null ->
                    repository.getTransactionsByCategory(filter.category)
                start != null && end != null ->
                    repository.getTransactionsByDateRange(start, end)
                else ->
                    repository.getAllTransactions()
            }
        }.onEach { transactions ->
            _uiState.update { it.copy(transactions = transactions) }
        }.launchIn(viewModelScope)

        // Distinct cards
        repository.getDistinctCards()
            .onEach { cards -> _uiState.update { it.copy(availableCards = cards) } }
            .launchIn(viewModelScope)
    }

    private fun resolveDateRange(
        filter: TransactionFilter,
        monthStartDay: Int
    ): Pair<LocalDateTime?, LocalDateTime?> {
        val today = LocalDate.now()
        return when (filter.datePreset) {
            DateRangePreset.ALL -> null to null

            DateRangePreset.TODAY ->
                today.atStartOfDay() to today.plusDays(1).atStartOfDay()

            DateRangePreset.THIS_WEEK -> {
                // Week starts on Monday (ISO)
                val dayOfWeek = today.dayOfWeek.value  // Mon=1 … Sun=7
                val weekStart = today.minusDays((dayOfWeek - 1).toLong())
                weekStart.atStartOfDay() to today.plusDays(1).atStartOfDay()
            }

            DateRangePreset.THIS_MONTH -> {
                // Month starts on user-defined day
                val startDay = monthStartDay.coerceIn(1, 28)
                val periodStart = if (today.dayOfMonth >= startDay) {
                    today.withDayOfMonth(startDay)
                } else {
                    today.minusMonths(1).withDayOfMonth(startDay)
                }
                periodStart.atStartOfDay() to today.plusDays(1).atStartOfDay()
            }

            DateRangePreset.LAST_MONTH -> {
                val startDay = monthStartDay.coerceIn(1, 28)
                val thisMonthStart = if (today.dayOfMonth >= startDay) {
                    today.withDayOfMonth(startDay)
                } else {
                    today.minusMonths(1).withDayOfMonth(startDay)
                }
                val lastMonthStart = thisMonthStart.minusMonths(1)
                lastMonthStart.atStartOfDay() to thisMonthStart.atStartOfDay()
            }

            DateRangePreset.CUSTOM ->
                filter.startDate to filter.endDate
        }
    }

    fun setFilter(filter: TransactionFilter) {
        _filter.value = filter
        _uiState.update { it.copy(filter = filter, selectedIds = emptySet(), isSelectionMode = false) }
    }

    fun setDatePreset(preset: DateRangePreset) {
        if (preset == DateRangePreset.CUSTOM) {
            _uiState.update { it.copy(showCustomDatePicker = true) }
            // Don't change filter yet — wait for user to pick dates
        } else {
            setFilter(_uiState.value.filter.copy(datePreset = preset, startDate = null, endDate = null))
        }
    }

    fun applyCustomDates(start: LocalDateTime, end: LocalDateTime) {
        setFilter(_uiState.value.filter.copy(
            datePreset = DateRangePreset.CUSTOM,
            startDate = start,
            endDate = end.plusDays(1) // inclusive end
        ))
        _uiState.update { it.copy(showCustomDatePicker = false) }
    }

    fun dismissCustomDatePicker() {
        _uiState.update { it.copy(showCustomDatePicker = false) }
    }

    fun saveMonthStartDay(day: Int) {
        viewModelScope.launch {
            val current = settingsRepository.settings.first()
            settingsRepository.saveSettings(current.copy(monthStartDay = day))
            // Re-apply current filter to refresh with new month start
            _filter.value = _filter.value.copy()
        }
    }

    // Selection
    fun toggleSelectionMode() =
        _uiState.update { it.copy(isSelectionMode = !it.isSelectionMode, selectedIds = emptySet()) }

    fun toggleSelect(id: Long) =
        _uiState.update { state ->
            val ids = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id
            state.copy(selectedIds = ids)
        }

    fun selectAll() =
        _uiState.update { it.copy(selectedIds = it.transactions.map { t -> t.id }.toSet()) }

    fun clearSelection() =
        _uiState.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }

    // Dialogs
    fun showAddDialog() = _uiState.update { it.copy(showAddDialog = true, editingTransaction = null) }
    fun showEditDialog(t: Transaction) = _uiState.update { it.copy(showAddDialog = true, editingTransaction = t) }
    fun showDetail(t: Transaction) = _uiState.update { it.copy(detailTransaction = t) }
    fun hideDetail() = _uiState.update { it.copy(detailTransaction = null) }
    fun hideDialog() = _uiState.update { it.copy(showAddDialog = false, editingTransaction = null) }

    fun saveTransaction(transaction: Transaction) {
        viewModelScope.launch {
            if (transaction.id == 0L) repository.insertTransaction(transaction)
            else repository.updateTransaction(transaction)
            hideDialog()
        }
    }

    fun deleteTransaction(id: Long) = viewModelScope.launch { repository.deleteTransaction(id) }

    fun deleteSelected() {
        viewModelScope.launch {
            _uiState.value.selectedIds.forEach { repository.deleteTransaction(it) }
            clearSelection()
        }
    }

    fun recategorize(transaction: Transaction, newCategory: ExpenseCategory) {
        viewModelScope.launch {
            repository.updateTransaction(transaction.copy(category = newCategory))
            _uiState.update { it.copy(detailTransaction = transaction.copy(category = newCategory)) }
        }
    }

    fun setAccountLabel(cardNumber: String, label: String) {
        viewModelScope.launch { repository.updateAccountLabel(cardNumber, label) }
    }

    fun exportSelected(context: Context) {
        viewModelScope.launch {
            val all = _uiState.value.transactions
            val toExport = if (_uiState.value.selectedIds.isEmpty()) all
                           else all.filter { it.id in _uiState.value.selectedIds }
            val file = CsvExporter.exportToFile(context, toExport)
            CsvExporter.shareFile(context, file)
        }
    }

    fun getSmsMatchDetails(transaction: Transaction): List<Pair<String, Boolean>> {
        val sms = transaction.rawSms ?: return emptyList()
        val combined = (transaction.merchant + " " + sms).lowercase()
        return SmsParser.getCategoryKeywords(transaction.category).map { kw -> kw to combined.contains(kw) }
    }
}
