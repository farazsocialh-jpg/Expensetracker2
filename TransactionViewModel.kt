package com.expensetracker.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.domain.model.ExpenseCategory
import com.expensetracker.domain.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class TransactionFilter(
    val category: ExpenseCategory? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val searchQuery: String = ""
)

data class TransactionUiState(
    val transactions: List<Transaction> = emptyList(),
    val filter: TransactionFilter = TransactionFilter(),
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val editingTransaction: Transaction? = null
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
        _filter.flatMapLatest { filter ->
            when {
                filter.searchQuery.isNotBlank() -> repository.searchTransactions(filter.searchQuery)
                filter.category != null -> repository.getTransactionsByCategory(filter.category)
                filter.startDate != null && filter.endDate != null ->
                    repository.getTransactionsByDateRange(filter.startDate, filter.endDate)
                else -> repository.getAllTransactions()
            }
        }.onEach { transactions ->
            _uiState.update { it.copy(transactions = transactions, isLoading = false) }
        }.launchIn(viewModelScope)
    }

    fun setFilter(filter: TransactionFilter) {
        _filter.value = filter
        _uiState.update { it.copy(filter = filter) }
    }

    fun showAddDialog() = _uiState.update { it.copy(showAddDialog = true, editingTransaction = null) }
    fun showEditDialog(t: Transaction) = _uiState.update { it.copy(showAddDialog = true, editingTransaction = t) }
    fun hideDialog() = _uiState.update { it.copy(showAddDialog = false, editingTransaction = null) }

    fun saveTransaction(transaction: Transaction) {
        viewModelScope.launch {
            if (transaction.id == 0L) repository.insertTransaction(transaction)
            else repository.updateTransaction(transaction)
            hideDialog()
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { repository.deleteTransaction(id) }
    }

    fun exportTransactions(context: android.content.Context) {
        viewModelScope.launch {
            val transactions = _uiState.value.transactions
            val file = com.expensetracker.utils.CsvExporter.exportToFile(context, transactions)
            com.expensetracker.utils.CsvExporter.shareFile(context, file)
        }
    }
}
