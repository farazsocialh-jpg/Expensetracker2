package com.expensetracker.presentation.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.ExpenseCategory
import com.expensetracker.domain.model.Transaction
import com.expensetracker.presentation.dashboard.formatAmount
import com.expensetracker.presentation.ui.theme.CategoryColors
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(viewModel: TransactionViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Detail / recategorise sheet
    state.detailTransaction?.let { txn ->
        TransactionDetailSheet(
            transaction = txn,
            matchDetails = viewModel.getSmsMatchDetails(txn),
            onDismiss = { viewModel.hideDetail() },
            onRecategorize = { cat -> viewModel.recategorize(txn, cat) },
            onEdit = { viewModel.showEditDialog(txn); viewModel.hideDetail() },
            onDelete = { viewModel.deleteTransaction(txn.id); viewModel.hideDetail() }
        )
    }

    if (state.showAddDialog) {
        AddEditTransactionDialog(
            transaction = state.editingTransaction,
            onDismiss = { viewModel.hideDialog() },
            onSave = { viewModel.saveTransaction(it) }
        )
    }

    Scaffold(
        topBar = {
            if (state.isSelectionMode) {
                TopAppBar(
                    title = { Text("${state.selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, "Cancel")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.SelectAll, "Select all")
                        }
                        IconButton(onClick = { viewModel.exportSelected(context) }) {
                            Icon(Icons.Default.Share, "Export")
                        }
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Transactions", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                            Icon(Icons.Default.Checklist, "Select")
                        }
                        IconButton(onClick = { viewModel.exportSelected(context) }) {
                            Icon(Icons.Default.Share, "Export CSV")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!state.isSelectionMode) {
                FloatingActionButton(
                    onClick = { viewModel.showAddDialog() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) { Icon(Icons.Default.Add, "Add") }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Search
            OutlinedTextField(
                value = state.filter.searchQuery,
                onValueChange = { viewModel.setFilter(state.filter.copy(searchQuery = it)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search merchant, note, sender…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (state.filter.searchQuery.isNotEmpty())
                        IconButton(onClick = { viewModel.setFilter(state.filter.copy(searchQuery = "")) }) {
                            Icon(Icons.Default.Clear, null)
                        }
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // Date preset row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DateRangePreset.values()) { preset ->
                    FilterChip(
                        selected = state.filter.datePreset == preset,
                        onClick = { viewModel.setDatePreset(preset) },
                        label = { Text(preset.label()) }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Category + Card filter row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.filter.category == null && state.filter.cardNumber == null,
                        onClick = { viewModel.setFilter(state.filter.copy(category = null, cardNumber = null)) },
                        label = { Text("All") }
                    )
                }
                items(ExpenseCategory.values()) { cat ->
                    FilterChip(
                        selected = state.filter.category == cat,
                        onClick = {
                            val newCat = if (state.filter.category == cat) null else cat
                            viewModel.setFilter(state.filter.copy(category = newCat, cardNumber = null))
                        },
                        label = { Text("${cat.emoji} ${cat.displayName}") }
                    )
                }
                if (state.availableCards.isNotEmpty()) {
                    item {
                        HorizontalDivider(modifier = Modifier.height(24.dp).width(1.dp))
                    }
                    items(state.availableCards) { card ->
                        FilterChip(
                            selected = state.filter.cardNumber == card,
                            onClick = {
                                val newCard = if (state.filter.cardNumber == card) null else card
                                viewModel.setFilter(state.filter.copy(cardNumber = newCard, category = null))
                            },
                            label = { Text("💳 ••••$card") }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Summary bar
            if (state.transactions.isNotEmpty()) {
                val total = state.transactions.sumOf { it.amount }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${state.transactions.size} transactions",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(formatAmount(total),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            if (state.transactions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No transactions found", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.transactions, key = { it.id }) { t ->
                        val isSelected = t.id in state.selectedIds
                        SelectableTransactionCard(
                            transaction = t,
                            isSelected = isSelected,
                            isSelectionMode = state.isSelectionMode,
                            onClick = {
                                if (state.isSelectionMode) viewModel.toggleSelect(t.id)
                                else viewModel.showDetail(t)
                            },
                            onLongClick = {
                                if (!state.isSelectionMode) viewModel.toggleSelectionMode()
                                viewModel.toggleSelect(t.id)
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableTransactionCard(
    transaction: Transaction,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val color = CategoryColors[transaction.category.name] ?: Color.Gray
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onClick() })
                Spacer(Modifier.width(4.dp))
            }
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).let { m ->
                    if (isSelected) m else m
                },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Text(transaction.category.emoji, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(transaction.merchant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                val subtitle = buildString {
                    append(transaction.category.displayName)
                    append(" • ")
                    append(transaction.dateTime.format(DateTimeFormatter.ofPattern("MMM d, h:mm a")))
                    if (transaction.cardNumber.isNotBlank()) append(" • ••••${transaction.cardNumber}")
                }
                Text(subtitle, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("-${formatAmount(transaction.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error)
                if (transaction.balance != null) {
                    Text("Bal: ${formatAmount(transaction.balance)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    transaction: Transaction,
    matchDetails: List<Pair<String, Boolean>>,
    onDismiss: () -> Unit,
    onRecategorize: (ExpenseCategory) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showRecategorize by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(transaction.category.emoji, fontSize = 24.sp)
                Spacer(Modifier.width(8.dp))
                Text(transaction.merchant, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Amount + date
                DetailRow("Amount", formatAmount(transaction.amount))
                DetailRow("Date", transaction.dateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")))
                DetailRow("Category", "${transaction.category.emoji} ${transaction.category.displayName}")
                if (transaction.balance != null) DetailRow("Balance", formatAmount(transaction.balance))
                if (transaction.cardNumber.isNotBlank()) DetailRow("Card", "••••${transaction.cardNumber}")
                if (transaction.accountLabel.isNotBlank()) DetailRow("Account", transaction.accountLabel)
                if (transaction.sender.isNotBlank()) DetailRow("From", transaction.sender)
                if (transaction.note.isNotBlank()) DetailRow("Note", transaction.note)

                // Raw SMS
                if (transaction.rawSms != null) {
                    HorizontalDivider()
                    Text("Raw SMS", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Text(transaction.rawSms,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Category matching debug
                if (matchDetails.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Category match — ${transaction.category.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    matchDetails.filter { it.second }.take(5).forEach { (kw, _) ->
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Check, null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Text("\"$kw\" matched",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Recategorize
                if (showRecategorize) {
                    HorizontalDivider()
                    Text("Change category:", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ExpenseCategory.values().forEach { cat ->
                        TextButton(
                            onClick = { onRecategorize(cat); showRecategorize = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${cat.emoji} ${cat.displayName}",
                                color = if (cat == transaction.category)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { showRecategorize = !showRecategorize }) {
                    Text("Recategorize")
                }
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionDialog(
    transaction: Transaction?,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var amount by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var merchant by remember { mutableStateOf(transaction?.merchant ?: "") }
    var note by remember { mutableStateOf(transaction?.note ?: "") }
    var cardNumber by remember { mutableStateOf(transaction?.cardNumber ?: "") }
    var accountLabel by remember { mutableStateOf(transaction?.accountLabel ?: "") }
    var selectedCategory by remember { mutableStateOf(transaction?.category ?: ExpenseCategory.OTHER) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (transaction == null) "Add Transaction" else "Edit Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("Amount (QAR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = merchant, onValueChange = { merchant = it },
                    label = { Text("Merchant") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedCategory.emoji} ${selectedCategory.displayName}",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showCategoryDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        ExpenseCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.emoji} ${cat.displayName}") },
                                onClick = { selectedCategory = cat; showCategoryDropdown = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = cardNumber, onValueChange = { cardNumber = it.take(4) },
                    label = { Text("Card last 4 digits (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = accountLabel, onValueChange = { accountLabel = it },
                    label = { Text("Account name (e.g. QNB Visa)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: return@Button
                onSave(Transaction(
                    id = transaction?.id ?: 0L,
                    amount = amt,
                    merchant = merchant.ifBlank { "Unknown" },
                    category = selectedCategory,
                    dateTime = transaction?.dateTime ?: LocalDateTime.now(),
                    isManual = true,
                    note = note,
                    cardNumber = cardNumber.trim(),
                    accountLabel = accountLabel.trim()
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun DateRangePreset.label() = when (this) {
    DateRangePreset.ALL -> "All"
    DateRangePreset.TODAY -> "Today"
    DateRangePreset.THIS_WEEK -> "This Week"
    DateRangePreset.THIS_MONTH -> "This Month"
    DateRangePreset.LAST_MONTH -> "Last Month"
    DateRangePreset.CUSTOM -> "Custom"
}
