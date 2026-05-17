package com.expensetracker.presentation.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.*
import java.time.format.DateTimeFormatter

fun formatAmount(amount: Double, currency: String = "QAR"): String =
    "$currency ${"%,.2f".format(amount)}"

@Composable
fun DashboardScreen(
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudget: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            HeroCard(
                monthly  = state.stats.monthlyTotal,
                daily    = state.stats.dailyTotal,
                weekly   = state.stats.weeklyTotal,
                projected = state.stats.projectedMonthly,
                dailyAvg  = state.stats.dailyAverage,
                currency = state.currency,
                hide     = state.hideBalances,
                loading  = state.isLoading
            )
        }

        if (state.stats.alerts.isNotEmpty()) {
            item { AlertsRow(state.stats.alerts) }
        }

        if (state.stats.categorySummaries.isNotEmpty()) {
            item {
                DonutSection(
                    summaries = state.stats.categorySummaries,
                    total     = state.stats.monthlyTotal,
                    currency  = state.currency,
                    hide      = state.hideBalances,
                    onBudgets = onNavigateToBudget
                )
            }
        }

        item {
            RecentSection(
                transactions = state.recentTransactions,
                currency     = state.currency,
                hide         = state.hideBalances,
                onSeeAll     = onNavigateToTransactions
            )
        }
    }
}

@Composable
fun HeroCard(
    monthly: Double, daily: Double, weekly: Double,
    projected: Double, dailyAvg: Double,
    currency: String, hide: Boolean, loading: Boolean
) {
    Box(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                MaterialTheme.colorScheme.background
            )))
            .padding(20.dp)
    ) {
        Column {
            Text("This Month", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            if (loading) {
                Box(Modifier.height(48.dp).width(180.dp).clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant))
            } else {
                Text(
                    if (hide) "••••••" else formatAmount(monthly, currency),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            if (!hide && projected > 0) {
                Text("Projected: ${formatAmount(projected, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniCard("Today",     if (hide) "••••" else formatAmount(daily, currency),    Modifier.weight(1f))
                MiniCard("This Week", if (hide) "••••" else formatAmount(weekly, currency),   Modifier.weight(1f))
                MiniCard("Daily Avg", if (hide) "••••" else formatAmount(dailyAvg, currency), Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun MiniCard(label: String, value: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun AlertsRow(alerts: List<SpendingAlert>) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        alerts.take(3).forEach { alert ->
            val color = when (alert.type) {
                AlertType.BUDGET_EXCEEDED -> MaterialTheme.colorScheme.error
                AlertType.BUDGET_WARNING  -> Color(0xFFFF9800)
                else                      -> MaterialTheme.colorScheme.primary
            }
            Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (alert.type == AlertType.BUDGET_EXCEEDED) Icons.Default.Warning else Icons.Default.Info,
                        null, tint = color, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(alert.message, style = MaterialTheme.typography.bodySmall, color = color)
                }
            }
        }
    }
}

@Composable
fun DonutSection(
    summaries: List<CategorySummary>, total: Double,
    currency: String, hide: Boolean, onBudgets: () -> Unit
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(summaries) { anim.snapTo(0f); anim.animateTo(1f, tween(900, easing = EaseOutCubic)) }

    Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Spending Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onBudgets) { Text("Budgets") }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DonutChart(summaries, total, anim.value, currency, hide, 140.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    summaries.take(6).forEach { s ->
                        val color = Color(s.category.color)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                            Text(s.category.displayName, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f), maxLines = 1)
                            if (!hide) Text(formatAmount(s.totalAmount, currency),
                                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(
    summaries: List<CategorySummary>, total: Double,
    animProgress: Float, currency: String, hide: Boolean, size: Dp
) {
    val sweeps = if (total > 0) summaries.map { ((it.totalAmount / total) * 360f * animProgress).toFloat() }
                 else summaries.map { 0f }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            var start = -90f
            sweeps.forEachIndexed { i, sweep ->
                drawArc(Color(summaries[i].category.color), start,
                    (sweep - 2f).coerceAtLeast(0f), false,
                    style = Stroke(28f, cap = StrokeCap.Round))
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (hide) "••••" else formatAmount(total * animProgress),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text("total", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RecentSection(
    transactions: List<Transaction>, currency: String,
    hide: Boolean, onSeeAll: () -> Unit
) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("Recent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onSeeAll) { Text("See All") }
        }
        if (transactions.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📭", fontSize = 40.sp)
                Spacer(Modifier.height(8.dp))
                Text("No transactions yet", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("SMS auto-import is active", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            transactions.forEach { t ->
                TransactionRow(t, currency, hide)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        }
    }
}

@Composable
fun TransactionRow(
    t: Transaction, currency: String = "QAR", hide: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val color = Color(t.category.color)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp)
            .let { if (onClick != null) it.clickable { onClick() } else it },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center) {
            Text(t.category.emoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.merchant, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(t.category.displayName, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("•", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(t.dateTime.format(DateTimeFormatter.ofPattern("MMM d, h:mm a")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (t.accountLast4.isNotBlank()) {
                    Text("• ••••${t.accountLast4}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(if (hide) "••••" else "-${formatAmount(t.amount, currency)}",
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error)
            if (t.isRecurring) Text("recurring", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}
