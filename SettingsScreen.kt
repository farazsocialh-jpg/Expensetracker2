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
fun SettingsScreen(onNavigateToRules: () -> Unit = {}, viewModel: SettingsViewModel = hiltViewModel()) {
    val state    by viewModel.state.collectAsState()
    val settings = state.settings
    val progress = state.scanProgress

    if (progress.done) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissScanResult() },
            title = { Text(if (progress.error != null) "Scan Failed" else "✅ Scan Complete") },
            text = {
                if (progress.error != null) Text("Error: ${progress.error}")
                else Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Scanned ${progress.scanned} messages")
                    Text("Imported ${progress.imported} new transactions",
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (progress.imported == 0)
                        Text("Try adding your bank name to Trusted Senders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { Button(onClick = { viewModel.dismissScanResult() }) { Text("OK") } }
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Scan ────────────────────────────────────────────────────
            item {
                SectionCard("📥 Scan Existing SMS") {
                    Text("Import past bank transactions from your inbox. Only trusted senders are read. 100% offline.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    if (progress.isScanning) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LinearProgressIndicator(
                                progress = { if (progress.total > 0) progress.scanned.toFloat() / progress.total else 0f },
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
                            Spacer(Modifier.height(6.dp))
                            Text("Scanning ${progress.scanned} / ${progress.total}…", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Button(onClick = { viewModel.scanInbox() }, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.Search, null); Spacer(Modifier.width(8.dp)); Text("Scan SMS Inbox Now")
                        }
                    }
                }
            }

            // ── Merchant Rules ──────────────────────────────────────────
            item {
                Card(shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    onClick = onNavigateToRules) {
                    Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("🏪", fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Merchant Rules", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Auto-categorization rules", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

                        // ── Appearance ──────────────────────────────────────────────
            item {
                SectionCard("🎨 Appearance") {
                    ToggleRow("Dark Mode", settings.darkTheme) { viewModel.toggleDarkTheme(it) }
                    ToggleRow("AMOLED Black", settings.amoledTheme) { viewModel.toggleAmoled(it) }
                    ToggleRow("Hide Balances", settings.hideBalances) { viewModel.toggleHideBalances(it) }
                }
            }

            // ── Auto Import ─────────────────────────────────────────────
            item {
                SectionCard("🔄 Auto-Import New SMS") {
                    ToggleRow("Auto-import incoming bank SMS", settings.autoScanEnabled) { viewModel.toggleAutoScan(it) }
                }
            }

            // ── Month Start Day ─────────────────────────────────────────
            item {
                SectionCard("📅 Month Start Day") {
                    Text("'This Month' filter starts from day ${settings.monthStartDay} of each month.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Day ${settings.monthStartDay}", style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Slider(value = settings.monthStartDay.toFloat(),
                            onValueChange = { viewModel.setMonthStartDay(it.toInt()) },
                            valueRange = 1f..28f, steps = 26, modifier = Modifier.weight(1f))
                    }
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(1,5,10,15,20,25,28).forEach { day ->
                            FilterChip(selected = settings.monthStartDay == day,
                                onClick = { viewModel.setMonthStartDay(day) },
                                label = { Text("$day") })
                        }
                    }
                }
            }

            // ── Currency ────────────────────────────────────────────────
            item {
                SectionCard("💱 Currency") {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("QAR","AED","SAR","KWD","BHD","OMR","USD","EUR","GBP","INR").forEach { c ->
                            FilterChip(selected = settings.currencySymbol == c,
                                onClick = { viewModel.setCurrency(c) }, label = { Text(c) })
                        }
                    }
                }
            }

            // ── Trusted Senders ─────────────────────────────────────────
            item {
                SectionCard("🏦 Trusted Senders") {
                    Text("Only SMS from these senders are scanned. Add your bank name or short code.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    ChipGroup(settings.trustedSenders, onRemove = { viewModel.removeSender(it) })
                    Spacer(Modifier.height(8.dp))
                    AddItemRow(state.newSenderInput, { viewModel.onNewSenderInput(it) }, "e.g. QNB or +97444xxxxxx") { viewModel.addSender() }
                }
            }

            // ── Debit Keywords ──────────────────────────────────────────
            item {
                SectionCard("🔍 Debit Keywords") {
                    Text("SMS with these words are treated as expenses.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    ChipGroup(settings.debitKeywords, onRemove = { viewModel.removeDebitKeyword(it) },
                        chipColor = MaterialTheme.colorScheme.errorContainer)
                    Spacer(Modifier.height(8.dp))
                    AddItemRow(state.newDebitKeyword, { viewModel.onNewDebitKeyword(it) }, "e.g. debited") { viewModel.addDebitKeyword() }
                }
            }

            // ── Ignore Keywords ─────────────────────────────────────────
            item {
                SectionCard("🚫 Ignore Keywords") {
                    Text("SMS with these words are ignored (credits, refunds).",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    ChipGroup(settings.creditKeywords, onRemove = { viewModel.removeCreditKeyword(it) },
                        chipColor = MaterialTheme.colorScheme.primaryContainer)
                    Spacer(Modifier.height(8.dp))
                    AddItemRow(state.newCreditKeyword, { viewModel.onNewCreditKeyword(it) }, "e.g. credited") { viewModel.addCreditKeyword() }
                }
            }

            // ── Privacy ─────────────────────────────────────────────────
            item {
                Card(shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🔒 Privacy", fontWeight = FontWeight.SemiBold)
                        listOf("All data stored locally on your device only",
                            "No internet connection used — ever",
                            "SMS read locally, never transmitted",
                            "Revoke SMS permission anytime in Android Settings"
                        ).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChipGroup(items: List<String>, onRemove: (String) -> Unit,
              chipColor: Color = MaterialTheme.colorScheme.secondaryContainer) {
    if (items.isEmpty()) {
        Text("None added", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant); return
    }
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { item ->
            InputChip(selected = false, onClick = {}, label = { Text(item, fontSize = 12.sp) },
                trailingIcon = {
                    IconButton(onClick = { onRemove(item) }, Modifier.size(18.dp)) {
                        Icon(Icons.Default.Close, null, Modifier.size(14.dp))
                    }
                },
                colors = InputChipDefaults.inputChipColors(containerColor = chipColor))
        }
    }
}

@Composable
fun AddItemRow(value: String, onValueChange: (String) -> Unit, placeholder: String, onAdd: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = value, onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 13.sp) },
            modifier = Modifier.weight(1f), singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAdd() }))
        FilledIconButton(onClick = onAdd, enabled = value.isNotBlank()) { Icon(Icons.Default.Add, null) }
    }
}
