package com.expensetracker.presentation.transactions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.SettingsRepository
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.domain.model.*
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
    val datePreset: DateRangePreset = DateRangePreset.ALL,
    val showRecurringOnly: Boolean = false
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
    val monthStartDay: Int = 1,
    val pendingRecategorize: Pair<Transaction, ExpenseCategory>? = null,
    val snackbarMessage: String? = null,
    val hideBalances: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repo: TransactionRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(TransactionFilter())
    private val _state  = MutableStateFlow(TransactionUiState())
    val state: StateFlow<TransactionUiState> = _state.asStateFlow()

    init {
        settingsRepo.settings.onEach { s ->
            _state.update { it.copy(monthStartDay = s.monthStartDay, hideBalances = s.hideBalances) }
        }.launchIn(viewModelScope)

        _filter.flatMapLatest { f ->
            val msd = _state.value.monthStartDay
            val (start, end) = resolveDates(f, msd)
            when {
                f.searchQuery.isNotBlank() -> repo.search(f.searchQuery)
                f.showRecurringOnly        -> repo.getRecurring()
                f.cardNumber != null       -> repo.getByCard(f.cardNumber)
                f.category != null && start != null && end != null ->
                    repo.getByDateRange(start, end).map { it.filter { t -> t.category == f.category } }
                f.category != null         -> repo.getByCategory(f.category)
                start != null && end != null -> repo.getByDateRange(start, end)
                else                       -> repo.getAll()
            }
        }.onEach { list ->
            _state.update { it.copy(transactions = list) }
        }.launchIn(viewModelScope)

        repo.getDistinctCards().onEach { cards ->
            _state.update { it.copy(availableCards = cards) }
        }.launchIn(viewModelScope)
    }

    private fun resolveDates(f: TransactionFilter, msd: Int): Pair<LocalDateTime?, LocalDateTime?> {
        val today = LocalDate.now()
        val startDay = msd.coerceIn(1, 28)
        return when (f.datePreset) {
            DateRangePreset.ALL       -> null to null
            DateRangePreset.TODAY     -> today.atStartOfDay() to today.plusDays(1).atStartOfDay()
            DateRangePreset.THIS_WEEK -> {
                val dow = today.dayOfWeek.value
                today.minusDays(dow.toLong() - 1).atStartOfDay() to today.plusDays(1).atStartOfDay()
            }
            DateRangePreset.THIS_MONTH -> {
                val ms = if (today.dayOfMonth >= startDay) today.withDayOfMonth(startDay)
                         else today.minusMonths(1).withDayOfMonth(startDay)
                ms.atStartOfDay() to today.plusDays(1).atStartOfDay()
            }
            DateRangePreset.LAST_MONTH -> {
                val thisMs = if (today.dayOfMonth >= startDay) today.withDayOfMonth(startDay)
                             else today.minusMonths(1).withDayOfMonth(startDay)
                thisMs.minusMonths(1).atStartOfDay() to thisMs.atStartOfDay()
            }
            DateRangePreset.CUSTOM -> f.startDate to f.endDate
        }
    }

    fun setFilter(f: TransactionFilter) {
        _filter.value = f
        _state.update { it.copy(filter = f, selectedIds = emptySet(), isSelectionMode = false) }
    }

    fun setDatePreset(p: DateRangePreset) {
        if (p == DateRangePreset.CUSTOM) { _state.update { it.copy(showCustomDatePicker = true) }; return }
        setFilter(_state.value.filter.copy(datePreset = p, startDate = null, endDate = null))
    }

    fun applyCustomDates(start: LocalDateTime, end: LocalDateTime) {
        setFilter(_state.value.filter.copy(datePreset = DateRangePreset.CUSTOM, startDate = start, endDate = end.plusDays(1)))
        _state.update { it.copy(showCustomDatePicker = false) }
    }

    fun dismissCustomDatePicker() = _state.update { it.copy(showCustomDatePicker = false) }

    // Selection
    fun toggleSelectionMode() = _state.update { it.copy(isSelectionMode = !it.isSelectionMode, selectedIds = emptySet()) }
    fun toggleSelect(id: Long) = _state.update { s ->
        val ids = if (id in s.selectedIds) s.selectedIds - id else s.selectedIds + id
        s.copy(selectedIds = ids)
    }
    fun selectAll()     = _state.update { it.copy(selectedIds = it.transactions.map { t -> t.id }.toSet()) }
    fun clearSelection() = _state.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }

    // Dialogs
    fun showAddDialog()            = _state.update { it.copy(showAddDialog = true, editingTransaction = null) }
    fun showEditDialog(t: Transaction) = _state.update { it.copy(showAddDialog = true, editingTransaction = t) }
    fun showDetail(t: Transaction) = _state.update { it.copy(detailTransaction = t) }
    fun hideDetail()               = _state.update { it.copy(detailTransaction = null) }
    fun hideDialog()               = _state.update { it.copy(showAddDialog = false, editingTransaction = null) }

    fun saveTransaction(t: Transaction) {
        viewModelScope.launch {
            if (t.id == 0L) repo.insert(t) else repo.update(t)
            hideDialog()
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repo.delete(id)
            _state.update { it.copy(snackbarMessage = "Transaction deleted") }
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val count = _state.value.selectedIds.size
            _state.value.selectedIds.forEach { repo.delete(it) }
            clearSelection()
            _state.update { it.copy(snackbarMessage = "$count transactions deleted") }
        }
    }

    /** Show recategorize dialog with rule options */
    fun requestRecategorize(transaction: Transaction, newCategory: ExpenseCategory) {
        _state.update { it.copy(pendingRecategorize = transaction to newCategory) }
    }

    fun dismissRecategorize() = _state.update { it.copy(pendingRecategorize = null) }

    /** Apply recategorization: once / this merchant only / all + future rule */
    fun applyRecategorize(transaction: Transaction, newCategory: ExpenseCategory, scope: RecategorizeScope) {
        viewModelScope.launch {
            when (scope) {
                RecategorizeScope.ONCE -> {
                    repo.update(transaction.copy(category = newCategory, userEdited = true))
                }
                RecategorizeScope.ALL_SIMILAR -> {
                    repo.applyMerchantRule(
                        merchantNormalized = transaction.normalizedMerchant,
                        category = newCategory,
                        displayName = transaction.merchant,
                        applyToPast = true
                    )
                    _state.update { it.copy(snackbarMessage = "Updated all ${transaction.merchant} transactions") }
                }
                RecategorizeScope.FUTURE_ONLY -> {
                    repo.applyMerchantRule(
                        merchantNormalized = transaction.normalizedMerchant,
                        category = newCategory,
                        displayName = transaction.merchant,
                        applyToPast = false
                    )
                    repo.update(transaction.copy(category = newCategory, userEdited = true))
                    _state.update { it.copy(snackbarMessage = "Rule saved for ${transaction.merchant}") }
                }
            }
            // Update detail view
            _state.update { it.copy(detailTransaction = transaction.copy(category = newCategory), pendingRecategorize = null) }
        }
    }

    fun clearSnackbar() = _state.update { it.copy(snackbarMessage = null) }

    fun setAccountLabel(card: String, label: String) {
        viewModelScope.launch { repo.updateAccountLabel(card, label) }
    }

    fun exportSelected(context: Context) {
        viewModelScope.launch {
            val all = _state.value.transactions
            val toExport = if (_state.value.selectedIds.isEmpty()) all
                           else all.filter { it.id in _state.value.selectedIds }
            val file = CsvExporter.exportToFile(context, toExport)
            CsvExporter.shareFile(context, file)
        }
    }

    fun getSmsMatchDetails(t: Transaction): List<Pair<String, Boolean>> {
        val sms = t.rawSms ?: return emptyList()
        val combined = (t.merchant + " " + sms).lowercase()
        return SmsParser.getCategoryKeywords(t.category).map { kw -> kw to combined.contains(kw) }
    }
}

enum class RecategorizeScope { ONCE, ALL_SIMILAR, FUTURE_ONLY }
