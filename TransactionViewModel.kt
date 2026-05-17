package com.expensetracker.presentation.transactions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.db.normalizeMerchant
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
    val pendingRecategorize: Pair<Transaction, ExpenseCategory>? = null,
    val snackbarMessage: String? = null,
    val hideBalances: Boolean = false,
    val currency: String = "QAR",
    val monthStartDay: Int = 1
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
            _state.update { it.copy(hideBalances = s.hideBalances, currency = s.currencySymbol, monthStartDay = s.monthStartDay) }
        }.launchIn(viewModelScope)

        combine(_filter, settingsRepo.settings) { f, s -> f to s.monthStartDay }
            .flatMapLatest { (f, msd) ->
                val (start, end) = resolveDates(f, msd)
                when {
                    f.searchQuery.isNotBlank()  -> repo.search(f.searchQuery)
                    f.showRecurringOnly         -> repo.getRecurring()
                    f.cardNumber != null        -> repo.getByCard(f.cardNumber)
                    f.category != null && start != null && end != null ->
                        repo.getByDateRange(start, end).map { list -> list.filter { it.category == f.category } }
                    f.category != null          -> repo.getByCategory(f.category)
                    start != null && end != null -> repo.getByDateRange(start, end)
                    else                        -> repo.getAll()
                }
            }
            .onEach { list -> _state.update { it.copy(transactions = list) } }
            .launchIn(viewModelScope)

        repo.getDistinctCards()
            .onEach { cards -> _state.update { it.copy(availableCards = cards) } }
            .launchIn(viewModelScope)
    }

    private fun resolveDates(f: TransactionFilter, msd: Int): Pair<LocalDateTime?, LocalDateTime?> {
        val today    = LocalDate.now()
        val startDay = msd.coerceIn(1, 28)
        return when (f.datePreset) {
            DateRangePreset.ALL        -> null to null
            DateRangePreset.TODAY      -> today.atStartOfDay() to today.plusDays(1).atStartOfDay()
            DateRangePreset.THIS_WEEK  -> today.minusDays(today.dayOfWeek.value.toLong() - 1).atStartOfDay() to today.plusDays(1).atStartOfDay()
            DateRangePreset.THIS_MONTH -> {
                val ms = if (today.dayOfMonth >= startDay) today.withDayOfMonth(startDay) else today.minusMonths(1).withDayOfMonth(startDay)
                ms.atStartOfDay() to today.plusDays(1).atStartOfDay()
            }
            DateRangePreset.LAST_MONTH -> {
                val thisMs = if (today.dayOfMonth >= startDay) today.withDayOfMonth(startDay) else today.minusMonths(1).withDayOfMonth(startDay)
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

    fun toggleSelectionMode() = _state.update { it.copy(isSelectionMode = !it.isSelectionMode, selectedIds = emptySet()) }
    fun toggleSelect(id: Long) = _state.update { s ->
        val ids = if (id in s.selectedIds) s.selectedIds - id else s.selectedIds + id
        s.copy(selectedIds = ids)
    }
    fun selectAll()      = _state.update { it.copy(selectedIds = it.transactions.map { t -> t.id }.toSet()) }
    fun clearSelection() = _state.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }

    fun showAddDialog()                = _state.update { it.copy(showAddDialog = true, editingTransaction = null) }
    fun showEditDialog(t: Transaction) = _state.update { it.copy(showAddDialog = true, editingTransaction = t) }
    fun showDetail(t: Transaction)     = _state.update { it.copy(detailTransaction = t) }
    fun hideDetail()                   = _state.update { it.copy(detailTransaction = null) }
    fun hideDialog()                   = _state.update { it.copy(showAddDialog = false, editingTransaction = null) }
    fun clearSnackbar()                = _state.update { it.copy(snackbarMessage = null) }
    fun dismissRecategorize()          = _state.update { it.copy(pendingRecategorize = null) }

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

    fun requestRecategorize(t: Transaction, cat: ExpenseCategory) =
        _state.update { it.copy(pendingRecategorize = t to cat) }

    fun applyRecategorize(t: Transaction, newCat: ExpenseCategory, scope: RecategorizeScope) {
        viewModelScope.launch {
            when (scope) {
                RecategorizeScope.ONCE -> repo.update(t.copy(category = newCat, userEdited = true))
                RecategorizeScope.ALL_SIMILAR -> {
                    repo.applyMerchantRule(normalizeMerchant(t.merchant), t.merchant, newCat, applyToPast = true)
                    _state.update { it.copy(snackbarMessage = "Updated all ${t.merchant} transactions") }
                }
                RecategorizeScope.FUTURE_ONLY -> {
                    repo.applyMerchantRule(normalizeMerchant(t.merchant), t.merchant, newCat, applyToPast = false)
                    repo.update(t.copy(category = newCat, userEdited = true))
                    _state.update { it.copy(snackbarMessage = "Rule saved for ${t.merchant}") }
                }
            }
            _state.update { it.copy(detailTransaction = t.copy(category = newCat), pendingRecategorize = null) }
        }
    }

    fun exportSelected(context: Context) {
        viewModelScope.launch {
            val list = _state.value.transactions
            val toExport = if (_state.value.selectedIds.isEmpty()) list else list.filter { it.id in _state.value.selectedIds }
            CsvExporter.shareFile(context, CsvExporter.exportToFile(context, toExport))
        }
    }

    fun getSmsMatchDetails(t: Transaction): List<Pair<String, Boolean>> {
        val sms = t.rawSms ?: return emptyList()
        val combined = (t.merchant + " " + sms).lowercase()
        return SmsParser.getCategoryKeywords(t.category).map { kw -> kw to combined.contains(kw) }
    }
}
