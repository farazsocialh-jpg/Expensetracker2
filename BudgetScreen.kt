package com.expensetracker.presentation.budget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.*
import com.expensetracker.presentation.dashboard.formatAmount
import java.time.LocalDate
import kotlin.math.min
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BudgetScreen(viewModel:BudgetViewModel=hiltViewModel()){
    val state by viewModel.state.collectAsState();val now=LocalDate.now()
    Scaffold(topBar={TopAppBar(title={Column{Text("Budgets",fontWeight=FontWeight.Bold);Text("${now.month.name.lowercase().replaceFirstChar{it.uppercase()}} ${now.year}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}})},floatingActionButton={FloatingActionButton(onClick={viewModel.showAddDialog()},containerColor=MaterialTheme.colorScheme.primary){Icon(Icons.Default.Add,null)}}){padding->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            item{val tb=state.budgets.sumOf{it.monthlyLimit};val ts=state.summaries.sumOf{it.totalAmount};val p=if(tb>0)min((ts/tb).toFloat(),1f) else 0f
                Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){Column(Modifier.padding(20.dp)){Text("Monthly Overview",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.7f));Spacer(Modifier.height(4.dp));Row(verticalAlignment=Alignment.Bottom){Text(formatAmount(ts,state.currency),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onPrimaryContainer);Text(" / ${formatAmount(tb,state.currency)}",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.6f))};Spacer(Modifier.height(10.dp));LinearProgressIndicator(progress={p},modifier=Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),color=if(p>=1f)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,trackColor=MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.1f));Spacer(Modifier.height(4.dp));Text("${(p*100).toInt()}% used",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.7f))}}}
            items(ExpenseCategory.values().toList()){cat->val budget=state.budgets.find{it.category==cat};val spent=state.summaries.find{it.category==cat}?.totalAmount?:0.0;val limit=budget?.monthlyLimit?:0.0;val p=if(budget!=null&&limit>0)min((spent/limit).toFloat(),1f) else 0f;val isOver=budget!=null&&spent>limit;val color=Color(cat.color)
                Card(shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=if(isOver)MaterialTheme.colorScheme.error.copy(alpha=0.08f) else MaterialTheme.colorScheme.surfaceVariant)){
                    Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Text(cat.emoji,fontSize=22.sp);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(cat.displayName,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold);if(budget!=null){Spacer(Modifier.height(5.dp));LinearProgressIndicator(progress={p},modifier=Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),color=if(isOver)MaterialTheme.colorScheme.error else color,trackColor=color.copy(alpha=0.2f));Spacer(Modifier.height(3.dp));Text("${formatAmount(spent,state.currency)} / ${formatAmount(limit,state.currency)}",style=MaterialTheme.typography.labelSmall,color=if(isOver)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)}else Text(if(spent>0)"${formatAmount(spent,state.currency)} spent · no limit" else "No budget set",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton(onClick={viewModel.showAddDialog(cat)}){Icon(if(budget!=null)Icons.Default.Edit else Icons.Default.Add,null,tint=MaterialTheme.colorScheme.primary)};if(budget!=null)IconButton(onClick={viewModel.deleteBudget(budget)}){Icon(Icons.Default.Delete,null,tint=MaterialTheme.colorScheme.error)}}
                }
            }
            item{Spacer(Modifier.height(80.dp))}
        }
        if(state.showDialog&&state.editingBudget!=null){var amount by remember{mutableStateOf(if(state.editingBudget!!.monthlyLimit>0)state.editingBudget!!.monthlyLimit.toString() else "")}
            AlertDialog(onDismissRequest={viewModel.hideDialog()},title={Text("${state.editingBudget!!.category.emoji} ${state.editingBudget!!.category.displayName}")},text={OutlinedTextField(value=amount,onValueChange={amount=it},label={Text("Monthly limit (${state.currency})")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth(),singleLine=true)},confirmButton={Button(onClick={val l=amount.toDoubleOrNull()?:return@Button;viewModel.saveBudget(state.editingBudget!!.copy(monthlyLimit=l))}){Text("Save")}},dismissButton={TextButton(onClick={viewModel.hideDialog()}){Text("Cancel")}})
        }
    }
}
