package com.expensetracker.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.domain.model.MerchantRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MerchantRulesUiState(val rules: List<MerchantRule> = emptyList())

@HiltViewModel
class MerchantRulesViewModel @Inject constructor(private val repo: TransactionRepository) : ViewModel() {
    private val _state = MutableStateFlow(MerchantRulesUiState())
    val state: StateFlow<MerchantRulesUiState> = _state.asStateFlow()

    init {
        repo.getMerchantRules().onEach { rules -> _state.update { it.copy(rules = rules) } }.launchIn(viewModelScope)
    }

    fun deleteRule(rule: MerchantRule) { viewModelScope.launch { repo.deleteMerchantRule(rule) } }
}
