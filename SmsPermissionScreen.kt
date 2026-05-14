package com.expensetracker.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SmsPermissionScreen(onGrantPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💸", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text("ExpenseTracker", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Your private, offline expense manager",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        listOf(
            "📱" to "Reads bank SMS to auto-track expenses",
            "🔒" to "100% offline — data never leaves your phone",
            "🏦" to "Only reads messages from your trusted banks",
            "🚫" to "OTPs and promotions are always ignored",
            "📵" to "No internet connection required — ever"
        ).forEach { (icon, text) ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 18.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Button(onClick = onGrantPermission, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Default.Message, null); Spacer(Modifier.width(8.dp))
            Text("Grant SMS Permission", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onGrantPermission) { Text("Skip — manual entry only") }
    }
}
