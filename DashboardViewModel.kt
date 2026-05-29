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
data class DashboardUiState(val stats:DashboardStats=DashboardStats(),val recentTransactions:List<Transaction>=emptyList(),val wallets:List<Wallet>=emptyList(),val goals:List<SavingsGoal>=emptyList(),val hideBalances:Boolean=false,val isLoading:Boolean=true,val monthStartDay:Int=1,val currency:String="QAR")
@HiltViewModel class DashboardViewModel @Inject constructor(private val repo:TransactionRepository,private val settingsRepo:SettingsRepository):ViewModel(){
    private val _state=MutableStateFlow(DashboardUiState());val state:StateFlow<DashboardUiState>=_state.asStateFlow()
    init{settingsRepo.settings.onEach{s->_state.update{it.copy(hideBalances=s.hideBalances,monthStartDay=s.monthStartDay,currency=s.currencySymbol)};loadStats(s.monthStartDay)}.launchIn(viewModelScope);repo.getRecent(8).onEach{list->_state.update{it.copy(recentTransactions=list)}}.launchIn(viewModelScope);repo.getWallets().onEach{list->_state.update{it.copy(wallets=list.filter{w->!w.isHidden})}}.launchIn(viewModelScope);repo.getGoals().onEach{list->_state.update{it.copy(goals=list.filter{g->g.status==GoalStatus.ACTIVE}.take(3))}}.launchIn(viewModelScope)}
    fun refresh()=loadStats(_state.value.monthStartDay)
    private fun loadStats(msd:Int){viewModelScope.launch{_state.update{it.copy(isLoading=true)};try{val stats=repo.getDashboardStats(LocalDate.now(),msd);_state.update{it.copy(stats=stats,isLoading=false)}}catch(_:Exception){_state.update{it.copy(isLoading=false)}}}}
}
