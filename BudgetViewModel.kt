package com.expensetracker.presentation.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.SettingsRepository
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BudgetUiState(
    val budgets: List<Budget> = emptyList(),
    val summaries: List<CategorySummary> = emptyList(),
    val showDialog: Boolean = false,
    val editingBudget: Budget? = null,
    val monthStartDay: Int = 1
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val repo: TransactionRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BudgetUiState())
    val state: StateFlow<BudgetUiState> = _state.asStateFlow()

    init {
        val now = LocalDate.now()
        settingsRepo.settings.onEach { s ->
            _state.update { it.copy(monthStartDay = s.monthStartDay) }
        }.launchIn(viewModelScope)

        repo.getBudgetsForMonth(now.monthValue, now.year)
            .onEach { budgets -> _state.update { it.copy(budgets = budgets) }; loadSummaries() }
            .launchIn(viewModelScope)
    }

    private fun loadSummaries() {
        viewModelScope.launch {
            val stats = repo.getDashboardStats(monthStartDay = _state.value.monthStartDay)
            _state.update { it.copy(summaries = stats.categorySummaries) }
        }
    }

    fun showAddDialog(category: ExpenseCategory? = null) {
        val now = LocalDate.now()
        val existing = _state.value.budgets.find { it.category == category }
        _state.update {
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

    fun hideDialog() = _state.update { it.copy(showDialog = false, editingBudget = null) }

    fun saveBudget(b: Budget) {
        viewModelScope.launch { repo.saveBudget(b); hideDialog() }
    }

    fun deleteBudget(b: Budget) {
        viewModelScope.launch { repo.deleteBudget(b) }
    }
}
