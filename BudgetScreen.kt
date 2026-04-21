package com.expensetracker.presentation.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.Budget
import com.expensetracker.domain.model.ExpenseCategory
import com.expensetracker.presentation.ui.theme.CategoryColors
import java.time.LocalDate
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: BudgetViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val now = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Manager", fontWeight = FontWeight.Bold) },
                subtitle = { Text("${now.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${now.year}") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Set Budget")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BudgetSummaryHeader(state)
            }
            items(ExpenseCategory.values().toList()) { cat ->
                val budget = state.budgets.find { it.category == cat }
                val summary = state.summaries.find { it.category == cat }
                BudgetCategoryCard(
                    category = cat,
                    budget = budget,
                    spent = summary?.totalAmount ?: 0.0,
                    onEdit = { viewModel.showAddDialog(cat) },
                    onDelete = { budget?.let { viewModel.deleteBudget(it) } }
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }

        if (state.showDialog && state.editingBudget != null) {
            BudgetDialog(
                budget = state.editingBudget!!,
                onDismiss = { viewModel.hideDialog() },
                onSave = { viewModel.saveBudget(it) }
            )
        }
    }
}

@Composable
fun BudgetSummaryHeader(state: BudgetUiState) {
    val totalBudget = state.budgets.sumOf { it.monthlyLimit }
    val totalSpent = state.summaries.sumOf { it.totalAmount }
    val progress = if (totalBudget > 0) min((totalSpent / totalBudget).toFloat(), 1f) else 0f

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Total Budget", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "₹${"%,.0f".format(totalSpent)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    " / ₹${"%,.0f".format(totalBudget)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = if (progress >= 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${(progress * 100).toInt()}% of monthly budget used",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun BudgetCategoryCard(
    category: ExpenseCategory,
    budget: Budget?,
    spent: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val hasBudget = budget != null
    val progress = if (hasBudget && budget!!.monthlyLimit > 0)
        min((spent / budget.monthlyLimit).toFloat(), 1f) else 0f
    val isOver = hasBudget && spent > (budget?.monthlyLimit ?: 0.0)
    val color = CategoryColors[category.name] ?: MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOver)
                MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(category.emoji, fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(category.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (hasBudget) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color = if (isOver) MaterialTheme.colorScheme.error else color,
                        trackColor = color.copy(alpha = 0.2f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "₹${"%,.0f".format(spent)} / ₹${"%,.0f".format(budget!!.monthlyLimit)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        if (spent > 0) "₹${"%,.0f".format(spent)} spent · no budget set"
                        else "No budget set",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(
                    if (hasBudget) Icons.Default.Edit else Icons.Default.Add,
                    contentDescription = "Edit budget",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (hasBudget) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete budget", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDialog(budget: Budget, onDismiss: () -> Unit, onSave: (Budget) -> Unit) {
    var amount by remember { mutableStateOf(if (budget.monthlyLimit > 0) budget.monthlyLimit.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Budget — ${budget.category.emoji} ${budget.category.displayName}") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Monthly limit (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = {
                val limit = amount.toDoubleOrNull() ?: return@Button
                onSave(budget.copy(monthlyLimit = limit))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
