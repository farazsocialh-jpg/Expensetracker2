package com.expensetracker.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings
    val progress = state.scanProgress

    if (progress.done) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissScanResult() },
            title = { Text(if (progress.error != null) "Scan Failed" else "Scan Complete ✅") },
            text = {
                if (progress.error != null) {
                    Text("Error: ${progress.error}")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Scanned ${progress.scanned} bank messages")
                        Text("Imported ${progress.imported} new transactions",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        if (progress.imported == 0) {
                            Spacer(Modifier.height(4.dp))
                            Text("No new transactions found. Try adjusting your sender list or keywords.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissScanResult() }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Scan Inbox ────────────────────────────────────────────────
            item {
                SectionCard("📥 Scan Existing SMS") {
                    Text(
                        "Scan your SMS inbox to import past bank transactions. " +
                        "Only messages from your trusted senders are read. " +
                        "All data stays on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    if (progress.isScanning) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LinearProgressIndicator(
                                progress = {
                                    if (progress.total > 0) progress.scanned.toFloat() / progress.total else 0f
                                },
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Scanning ${progress.scanned} / ${progress.total} messages…",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.scanInbox() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Search, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Scan SMS Inbox Now")
                        }
                    }
                }
            }

            // ── Auto Scan Toggle ─────────────────────────────────────────
            item {
                SectionCard("🔄 Auto-Import New SMS") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Auto-import incoming SMS",
                                style = MaterialTheme.typography.bodyMedium)
                            Text("Automatically detect bank SMS as they arrive",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.autoScanEnabled,
                            onCheckedChange = { viewModel.toggleAutoScan(it) }
                        )
                    }
                }
            }

            // ── Currency ─────────────────────────────────────────────────
            item {
                SectionCard("💱 Currency") {
                    Text("Select your currency:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    val currencies = listOf("QAR","AED","SAR","KWD","BHD","OMR","USD","EUR","GBP","INR")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        currencies.forEach { c ->
                            FilterChip(
                                selected = settings.currencySymbol == c,
                                onClick = { viewModel.setCurrency(c) },
                                label = { Text(c) }
                            )
                        }
                    }
                }
            }

            // ── Trusted Senders ──────────────────────────────────────────
            item {
                SectionCard("🏦 Trusted Senders") {
                    Text(
                        "Only SMS from these senders will be scanned. " +
                        "Add your bank's name or number (e.g. QNB, DOHA BANK).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    ChipGroup(items = settings.trustedSenders, onRemove = { viewModel.removeSender(it) })
                    Spacer(Modifier.height(8.dp))
                    AddItemRow(
                        value = state.newSenderInput,
                        onValueChange = { viewModel.onNewSenderInput(it) },
                        placeholder = "e.g. QNB or +974xxxxxxxx",
                        onAdd = { viewModel.addSender() }
                    )
                }
            }

            // ── Debit Keywords ───────────────────────────────────────────
            item {
                SectionCard("🔍 Debit Detection Keywords") {
                    Text("SMS containing any of these words will be treated as a debit transaction.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    ChipGroup(
                        items = settings.debitKeywords,
                        onRemove = { viewModel.removeDebitKeyword(it) },
                        chipColor = MaterialTheme.colorScheme.errorContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    AddItemRow(
                        value = state.newDebitKeyword,
                        onValueChange = { viewModel.onNewDebitKeyword(it) },
                        placeholder = "e.g. debited",
                        onAdd = { viewModel.addDebitKeyword() }
                    )
                }
            }

            // ── Credit Keywords ──────────────────────────────────────────
            item {
                SectionCard("🚫 Ignore (Credit) Keywords") {
                    Text("SMS containing these words will be ignored.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    ChipGroup(
                        items = settings.creditKeywords,
                        onRemove = { viewModel.removeCreditKeyword(it) },
                        chipColor = MaterialTheme.colorScheme.primaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    AddItemRow(
                        value = state.newCreditKeyword,
                        onValueChange = { viewModel.onNewCreditKeyword(it) },
                        placeholder = "e.g. credited",
                        onAdd = { viewModel.addCreditKeyword() }
                    )
                }
            }

            // ── Privacy Notice ───────────────────────────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🔒 Privacy & Security", fontWeight = FontWeight.SemiBold)
                        listOf(
                            "SMS is read locally on your device only",
                            "No data is sent to any server or third party",
                            "Only messages from your trusted senders are processed",
                            "You can revoke SMS permission anytime in Android Settings"
                        ).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChipGroup(
    items: List<String>,
    onRemove: (String) -> Unit,
    chipColor: Color = MaterialTheme.colorScheme.secondaryContainer
) {
    if (items.isEmpty()) {
        Text("None added yet",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { item ->
            InputChip(
                selected = false,
                onClick = {},
                label = { Text(item, fontSize = 12.sp) },
                trailingIcon = {
                    IconButton(onClick = { onRemove(item) }, modifier = Modifier.size(18.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                    }
                },
                colors = InputChipDefaults.inputChipColors(containerColor = chipColor)
            )
        }
    }
}

@Composable
fun AddItemRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onAdd: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 13.sp) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAdd() })
        )
        FilledIconButton(onClick = onAdd, enabled = value.isNotBlank()) {
            Icon(Icons.Default.Add, "Add")
        }
    }
}
