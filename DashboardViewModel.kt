package com.expensetracker.presentation.dashboard

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

data class DashboardUiState(
    val stats: DashboardStats? = null,
    val recentTransactions: List<Transaction> = emptyList(),
    val hideBalances: Boolean = false,
    val isLoading: Boolean = true,
    val monthStartDay: Int = 1
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: TransactionRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        settingsRepo.settings.onEach { s ->
            _state.update { it.copy(hideBalances = s.hideBalances, monthStartDay = s.monthStartDay) }
            loadStats(s.monthStartDay)
        }.launchIn(viewModelScope)

        repo.getRecent(8).onEach { txns ->
            _state.update { it.copy(recentTransactions = txns) }
        }.launchIn(viewModelScope)
    }

    fun refresh() {
        loadStats(_state.value.monthStartDay)
    }

    private fun loadStats(monthStartDay: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val stats = repo.getDashboardStats(LocalDate.now(), monthStartDay)
                _state.update { it.copy(stats = stats, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
