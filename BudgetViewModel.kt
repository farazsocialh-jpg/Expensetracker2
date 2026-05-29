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
data class BudgetUiState(val budgets:List<Budget>=emptyList(),val summaries:List<CategorySummary>=emptyList(),val showDialog:Boolean=false,val editingBudget:Budget?=null,val currency:String="QAR")
@HiltViewModel class BudgetViewModel @Inject constructor(private val repo:TransactionRepository,private val settingsRepo:SettingsRepository):ViewModel(){
    private val _state=MutableStateFlow(BudgetUiState());val state:StateFlow<BudgetUiState>=_state.asStateFlow();private var msd=1
    init{val now=LocalDate.now();settingsRepo.settings.onEach{s->_state.update{it.copy(currency=s.currencySymbol)};msd=s.monthStartDay}.launchIn(viewModelScope);repo.getBudgetsForMonth(now.monthValue,now.year).onEach{b->_state.update{it.copy(budgets=b)};refresh()}.launchIn(viewModelScope)}
    private fun refresh(){viewModelScope.launch{try{val s=repo.getDashboardStats(monthStartDay=msd);_state.update{it.copy(summaries=s.categorySummaries)}}catch(_:Exception){}}}
    fun showAddDialog(cat:ExpenseCategory?=null){val now=LocalDate.now();val ex=_state.value.budgets.find{it.category==cat};_state.update{it.copy(showDialog=true,editingBudget=ex?:Budget(category=cat?:ExpenseCategory.OTHER,monthlyLimit=0.0,month=now.monthValue,year=now.year))}}
    fun hideDialog()=_state.update{it.copy(showDialog=false,editingBudget=null)}
    fun saveBudget(b:Budget){viewModelScope.launch{repo.saveBudget(b);hideDialog()}}
    fun deleteBudget(b:Budget){viewModelScope.launch{repo.deleteBudget(b)}}
}
