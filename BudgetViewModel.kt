package com.expensetracker.presentation.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.domain.model.Budget
import com.expensetracker.domain.model.CategorySummary
import com.expensetracker.domain.model.ExpenseCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BudgetUiState(
    val budgets: List<Budget> = emptyList(),
    val summaries: List<CategorySummary> = emptyList(),
    val showDialog: Boolean = false,
    val editingBudget: Budget? = null
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        val now = LocalDate.now()
        repository.getBudgetsForMonth(now.monthValue, now.year)
            .onEach { budgets ->
                _uiState.update { it.copy(budgets = budgets) }
                loadSummaries()
            }
            .launchIn(viewModelScope)
    }

    private fun loadSummaries() {
        viewModelScope.launch {
            val stats = repository.getDashboardStats()
            _uiState.update { it.copy(summaries = stats.categorySummaries) }
        }
    }

    fun showAddDialog(category: ExpenseCategory? = null) {
        val now = LocalDate.now()
        val existing = _uiState.value.budgets.find { it.category == category }
        _uiState.update {
            it.copy(
                showDialog = true,
                editingBudget = existing ?: Budget(
                    category = category ?: ExpenseCategory.OTHER,
                    monthlyLimit = 0.0,
                    month = now.monthValue,
                    year = now.year
                )
            )
        }
    }

    fun hideDialog() = _uiState.update { it.copy(showDialog = false, editingBudget = null) }

    fun saveBudget(budget: Budget) {
        viewModelScope.launch {
            repository.saveBudget(budget)
            hideDialog()
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch { repository.deleteBudget(budget) }
    }
}
