package com.expensetracker.presentation.transactions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.domain.model.ExpenseCategory
import com.expensetracker.domain.model.Transaction
import com.expensetracker.service.SmsParser
import com.expensetracker.utils.CsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
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
    val showDatePicker: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(TransactionFilter())
    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    init {
        // Observe transactions reactively
        _filter.flatMapLatest { filter ->
            val (start, end) = resolveDateRange(filter)
            when {
                filter.searchQuery.isNotBlank() -> repository.searchTransactions(filter.searchQuery)
                filter.cardNumber != null -> repository.getTransactionsByCard(filter.cardNumber)
                filter.category != null && start != null && end != null ->
                    repository.getTransactionsByDateRange(start, end)
                        .map { list -> list.filter { it.category == filter.category } }
                filter.category != null -> repository.getTransactionsByCategory(filter.category)
                start != null && end != null -> repository.getTransactionsByDateRange(start, end)
                else -> repository.getAllTransactions()
            }
        }.onEach { transactions ->
            _uiState.update { it.copy(transactions = transactions) }
        }.launchIn(viewModelScope)

        // Observe distinct cards
        repository.getDistinctCards()
            .onEach { cards -> _uiState.update { it.copy(availableCards = cards) } }
            .launchIn(viewModelScope)
    }

    private fun resolveDateRange(filter: TransactionFilter): Pair<LocalDateTime?, LocalDateTime?> {
        val now = LocalDate.now()
        return when (filter.datePreset) {
            DateRangePreset.ALL -> null to null
            DateRangePreset.TODAY ->
                now.atStartOfDay() to now.plusDays(1).atStartOfDay()
            DateRangePreset.THIS_WEEK ->
                now.minusDays(now.dayOfWeek.value.toLong() - 1).atStartOfDay() to now.plusDays(1).atStartOfDay()
            DateRangePreset.THIS_MONTH ->
                now.withDayOfMonth(1).atStartOfDay() to now.plusDays(1).atStartOfDay()
            DateRangePreset.LAST_MONTH -> {
                val first = now.minusMonths(1).withDayOfMonth(1)
                first.atStartOfDay() to first.plusMonths(1).atStartOfDay()
            }
            DateRangePreset.CUSTOM -> filter.startDate to filter.endDate
        }
    }

    fun setFilter(filter: TransactionFilter) {
        _filter.value = filter
        _uiState.update { it.copy(filter = filter, selectedIds = emptySet(), isSelectionMode = false) }
    }

    fun setDatePreset(preset: DateRangePreset) = setFilter(_uiState.value.filter.copy(datePreset = preset))

    fun setCustomDates(start: LocalDateTime, end: LocalDateTime) =
        setFilter(_uiState.value.filter.copy(datePreset = DateRangePreset.CUSTOM, startDate = start, endDate = end))

    // Selection
    fun toggleSelectionMode() {
        _uiState.update { it.copy(isSelectionMode = !it.isSelectionMode, selectedIds = emptySet()) }
    }

    fun toggleSelect(id: Long) {
        _uiState.update { state ->
            val newIds = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id
            state.copy(selectedIds = newIds)
        }
    }

    fun selectAll() {
        _uiState.update { it.copy(selectedIds = it.transactions.map { t -> t.id }.toSet()) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
    }

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

    /** Re-categorise a transaction using a new override category */
    fun recategorize(transaction: Transaction, newCategory: ExpenseCategory) {
        viewModelScope.launch {
            repository.updateTransaction(transaction.copy(category = newCategory))
            _uiState.update { it.copy(detailTransaction = transaction.copy(category = newCategory)) }
        }
    }

    /** Rename the account label for a card */
    fun setAccountLabel(cardNumber: String, label: String) {
        viewModelScope.launch {
            repository.updateAccountLabel(cardNumber, label)
        }
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
        // Show which category keywords matched for this transaction
        val sms = transaction.rawSms ?: return emptyList()
        val combined = (transaction.merchant + " " + sms).lowercase()
        return SmsParser.getCategoryKeywords(transaction.category).map { keyword ->
            keyword to combined.contains(keyword)
        }
    }
}
