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
import com.expensetracker.domain.model.*
import com.expensetracker.presentation.dashboard.TransactionRow
import com.expensetracker.presentation.dashboard.formatAmount
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(viewModel: TransactionViewModel = hiltViewModel()) {
    val state   by viewModel.state.collectAsState()
    val context  = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { snackbar.showSnackbar(it); viewModel.clearSnackbar() }
    }

    state.pendingRecategorize?.let { (txn, newCat) ->
        RecategorizeDialog(txn, newCat,
            onDismiss = { viewModel.dismissRecategorize() },
            onApply   = { scope -> viewModel.applyRecategorize(txn, newCat, scope) }
        )
    }
    state.detailTransaction?.let { txn ->
        DetailDialog(
            transaction  = txn,
            matchDetails = viewModel.getSmsMatchDetails(txn),
            currency     = state.currency,
            hide         = state.hideBalances,
            onDismiss    = { viewModel.hideDetail() },
            onRecategorize = { cat -> viewModel.requestRecategorize(txn, cat) },
            onEdit       = { viewModel.showEditDialog(txn); viewModel.hideDetail() },
            onDelete     = { viewModel.deleteTransaction(txn.id); viewModel.hideDetail() }
        )
    }
    if (state.showAddDialog) {
        AddEditDialog(state.editingTransaction,
            onDismiss = { viewModel.hideDialog() },
            onSave    = { viewModel.saveTransaction(it) }
        )
    }
    if (state.showCustomDatePicker) {
        CustomDateDialog(
            onDismiss = { viewModel.dismissCustomDatePicker() },
            onApply   = { s, e -> viewModel.applyCustomDates(s, e) }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (state.isSelectionMode) {
                TopAppBar(
                    title = { Text("${state.selectedIds.size} selected") },
                    navigationIcon = { IconButton(onClick = { viewModel.clearSelection() }) { Icon(Icons.Default.Close, null) } },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll() })          { Icon(Icons.Default.SelectAll, null) }
                        IconButton(onClick = { viewModel.exportSelected(context) }) { Icon(Icons.Default.Share, null) }
                        IconButton(onClick = { viewModel.deleteSelected() })     { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Transactions", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { viewModel.toggleSelectionMode() }) { Icon(Icons.Default.CheckBox, null) }
                        IconButton(onClick = { viewModel.exportSelected(context) }) { Icon(Icons.Default.Share, null) }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!state.isSelectionMode)
                FloatingActionButton(onClick = { viewModel.showAddDialog() },
                    containerColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Search
            OutlinedTextField(
                value = state.filter.searchQuery,
                onValueChange = { viewModel.setFilter(state.filter.copy(searchQuery = it)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search merchant, note, card…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (state.filter.searchQuery.isNotEmpty())
                        IconButton(onClick = { viewModel.setFilter(state.filter.copy(searchQuery = "")) }) { Icon(Icons.Default.Clear, null) }
                },
                shape = RoundedCornerShape(14.dp), singleLine = true
            )
            // Date chips
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DateRangePreset.values()) { preset ->
                    val localStart = state.filter.startDate
                    val localEnd   = state.filter.endDate
                    val isCustomActive = preset == DateRangePreset.CUSTOM &&
                        state.filter.datePreset == DateRangePreset.CUSTOM && localStart != null
                    val label = if (isCustomActive) {
                        val f = DateTimeFormatter.ofPattern("MMM d")
                        "${localStart!!.format(f)} – ${localEnd?.format(f) ?: "?"}"
                    } else preset.label()
                    FilterChip(
                        selected = state.filter.datePreset == preset,
                        onClick  = { viewModel.setDatePreset(preset) },
                        label    = { Text(label) },
                        trailingIcon = if (preset == DateRangePreset.CUSTOM) {
                            { Icon(Icons.Default.DateRange, null, Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            // Category + card chips
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.filter.category == null && state.filter.cardNumber == null && !state.filter.showRecurringOnly,
                        onClick  = { viewModel.setFilter(state.filter.copy(category = null, cardNumber = null, showRecurringOnly = false)) },
                        label    = { Text("All") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.filter.showRecurringOnly,
                        onClick  = { viewModel.setFilter(state.filter.copy(showRecurringOnly = !state.filter.showRecurringOnly)) },
                        label    = { Text("🔄 Recurring") }
                    )
                }
                items(ExpenseCategory.values()) { cat ->
                    FilterChip(
                        selected = state.filter.category == cat,
                        onClick  = {
                            val nc = if (state.filter.category == cat) null else cat
                            viewModel.setFilter(state.filter.copy(category = nc, cardNumber = null))
                        },
                        label = { Text("${cat.emoji} ${cat.displayName}") }
                    )
                }
                items(state.availableCards) { card ->
                    FilterChip(
                        selected = state.filter.cardNumber == card,
                        onClick  = {
                            val nc = if (state.filter.cardNumber == card) null else card
                            viewModel.setFilter(state.filter.copy(cardNumber = nc, category = null))
                        },
                        leadingIcon = { Text("💳", fontSize = 12.sp) },
                        label       = { Text("••••$card") }
                    )
                }
            }
            // Summary bar
            if (state.transactions.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${state.transactions.size} transactions",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        if (!state.hideBalances)
                            Text(formatAmount(state.transactions.sumOf { it.amount }, state.currency),
                                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            if (state.transactions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", fontSize = 48.sp); Spacer(Modifier.height(8.dp))
                        Text("No transactions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    items(state.transactions, key = { it.id }) { t ->
                        val isSelected = t.id in state.selectedIds
                        Surface(
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier.combinedClickable(
                                onClick = { if (state.isSelectionMode) viewModel.toggleSelect(t.id) else viewModel.showDetail(t) },
                                onLongClick = { if (!state.isSelectionMode) viewModel.toggleSelectionMode(); viewModel.toggleSelect(t.id) }
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (state.isSelectionMode) Checkbox(checked = isSelected, onCheckedChange = { viewModel.toggleSelect(t.id) })
                                Box(Modifier.weight(1f)) { TransactionRow(t, state.currency, state.hideBalances) }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun RecategorizeDialog(
    transaction: Transaction, newCategory: ExpenseCategory,
    onDismiss: () -> Unit, onApply: (RecategorizeScope) -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Change Category") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${transaction.merchant}  →  ${newCategory.emoji} ${newCategory.displayName}",
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("Apply to:", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = { onApply(RecategorizeScope.ALL_SIMILAR) }, Modifier.fillMaxWidth()) {
                    Text("All ${transaction.merchant} — past & future")
                }
                OutlinedButton(onClick = { onApply(RecategorizeScope.FUTURE_ONLY) }, Modifier.fillMaxWidth()) {
                    Text("Future transactions only")
                }
                TextButton(onClick = { onApply(RecategorizeScope.ONCE) }, Modifier.fillMaxWidth()) {
                    Text("Just this one")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailDialog(
    transaction: Transaction, matchDetails: List<Pair<String, Boolean>>,
    currency: String, hide: Boolean,
    onDismiss: () -> Unit, onRecategorize: (ExpenseCategory) -> Unit,
    onEdit: () -> Unit, onDelete: () -> Unit
) {
    var showRecategorize by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(transaction.category.emoji, fontSize = 22.sp)
                Text(transaction.merchant, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!hide) DetailRow("Amount", formatAmount(transaction.amount, currency))
                DetailRow("Date", transaction.dateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")))
                DetailRow("Category", "${transaction.category.emoji} ${transaction.category.displayName}")
                if (!hide && transaction.balance != null) DetailRow("Balance", formatAmount(transaction.balance, currency))
                if (transaction.accountLast4.isNotBlank()) DetailRow("Card", "••••${transaction.accountLast4}")
                if (transaction.accountLabel.isNotBlank()) DetailRow("Account", transaction.accountLabel)
                if (transaction.bankName.isNotBlank()) DetailRow("Bank", transaction.bankName)
                if (transaction.sender.isNotBlank()) DetailRow("Sender", transaction.sender)
                if (transaction.note.isNotBlank()) DetailRow("Note", transaction.note)
                if (transaction.confidenceScore < 0.7f)
                    Text("⚠️ Low confidence categorization",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                if (transaction.rawSms != null) {
                    HorizontalDivider()
                    Text("Raw SMS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.background) {
                        Text(transaction.rawSms, Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val matched = matchDetails.filter { it.second }
                if (matched.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Matched → ${transaction.category.displayName}",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    matched.take(4).forEach { (kw, _) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("\"$kw\"", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                if (showRecategorize) {
                    HorizontalDivider()
                    Text("Change to:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ExpenseCategory.values().forEach { cat ->
                        TextButton(onClick = { onRecategorize(cat); showRecategorize = false }, Modifier.fillMaxWidth()) {
                            Text("${cat.emoji} ${cat.displayName}",
                                color = if (cat == transaction.category) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { showRecategorize = !showRecategorize }) { Text("Recategorize") }
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDateDialog(onDismiss: () -> Unit, onApply: (LocalDateTime, LocalDateTime) -> Unit) {
    var startText by remember { mutableStateOf("") }
    var endText   by remember { mutableStateOf("") }
    var error     by remember { mutableStateOf("") }
    val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("📅 Custom Date Range") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Format: DD/MM/YYYY", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = startText, onValueChange = { startText = it; error = "" },
                    label = { Text("Start date") }, placeholder = { Text("01/04/2025") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = endText, onValueChange = { endText = it; error = "" },
                    label = { Text("End date") }, placeholder = { Text("30/04/2025") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (error.isNotBlank())
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                val today = LocalDate.now()
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { startText = today.withDayOfMonth(1).format(fmt); endText = today.format(fmt) }) { Text("This month") }
                    TextButton(onClick = { startText = today.minusDays(30).format(fmt); endText = today.format(fmt) }) { Text("30 days") }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                try {
                    val s = LocalDate.parse(startText.trim(), fmt).atStartOfDay()
                    val e = LocalDate.parse(endText.trim(), fmt).atStartOfDay()
                    if (e.isBefore(s)) { error = "End must be after start"; return@Button }
                    onApply(s, e)
                } catch (_: Exception) { error = "Use DD/MM/YYYY format" }
            }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDialog(transaction: Transaction?, onDismiss: () -> Unit, onSave: (Transaction) -> Unit) {
    var amount  by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var merchant by remember { mutableStateOf(transaction?.merchant ?: "") }
    var note    by remember { mutableStateOf(transaction?.note ?: "") }
    var card    by remember { mutableStateOf(transaction?.accountLast4 ?: "") }
    var label   by remember { mutableStateOf(transaction?.accountLabel ?: "") }
    var cat     by remember { mutableStateOf(transaction?.category ?: ExpenseCategory.OTHER) }
    var catOpen by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (transaction == null) "Add Transaction" else "Edit Transaction") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = amount, onValueChange = { amount = it },
                    label = { Text("Amount (QAR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = merchant, onValueChange = { merchant = it },
                    label = { Text("Merchant") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                ExposedDropdownMenuBox(expanded = catOpen, onExpandedChange = { catOpen = it }) {
                    OutlinedTextField(value = "${cat.emoji} ${cat.displayName}", onValueChange = {},
                        readOnly = true, label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catOpen) },
                        modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = catOpen, onDismissRequest = { catOpen = false }) {
                        ExpenseCategory.values().forEach { c ->
                            DropdownMenuItem(text = { Text("${c.emoji} ${c.displayName}") },
                                onClick = { cat = c; catOpen = false })
                        }
                    }
                }
                OutlinedTextField(value = card, onValueChange = { if (it.length <= 4) card = it },
                    label = { Text("Card last 4 digits") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = label, onValueChange = { label = it },
                    label = { Text("Account label (e.g. QNB Visa)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = note, onValueChange = { note = it },
                    label = { Text("Note") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: return@Button
                onSave(Transaction(id = transaction?.id ?: 0L, amount = amt,
                    merchant = merchant.ifBlank { "Unknown" }, category = cat,
                    dateTime = transaction?.dateTime ?: LocalDateTime.now(),
                    isManual = true, note = note, accountLast4 = card.trim(), accountLabel = label.trim()))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun DateRangePreset.label() = when (this) {
    DateRangePreset.ALL        -> "All Time"
    DateRangePreset.TODAY      -> "Today"
    DateRangePreset.THIS_WEEK  -> "This Week"
    DateRangePreset.THIS_MONTH -> "This Month"
    DateRangePreset.LAST_MONTH -> "Last Month"
    DateRangePreset.CUSTOM     -> "Custom"
}
