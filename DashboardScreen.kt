package com.expensetracker.presentation.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.expensetracker.presentation.ui.theme.CategoryColors
import java.time.format.DateTimeFormatter
import kotlin.math.min

// ─── Shared currency formatter ────────────────────────────────────────────────
fun formatAmount(amount: Double, currency: String = "QAR"): String =
    "$currency ${"%,.2f".format(amount)}"

@Composable
fun DashboardScreen(
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudget: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val stats = state.stats

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Hero card
        item {
            HeroSpendCard(
                monthly = stats?.monthlyTotal ?: 0.0,
                daily = stats?.dailyTotal ?: 0.0,
                weekly = stats?.weeklyTotal ?: 0.0,
                projected = stats?.projectedMonthly ?: 0.0,
                dailyAvg = stats?.dailyAverage ?: 0.0,
                hideBalances = state.hideBalances,
                isLoading = state.isLoading
            )
        }

        // Alerts
        if ((stats?.alerts?.size ?: 0) > 0) {
            item { AlertsSection(alerts = stats!!.alerts) }
        }

        // Donut chart
        if ((stats?.categorySummaries?.size ?: 0) > 0) {
            item {
                SpendingDonutChart(
                    summaries = stats!!.categorySummaries,
                    total = stats.monthlyTotal,
                    hideBalances = state.hideBalances,
                    onSeeAll = onNavigateToBudget
                )
            }
        }

        // Top merchants
        if ((stats?.topMerchants?.size ?: 0) > 0) {
            item { TopMerchantsSection(merchants = stats!!.topMerchants, hideBalances = state.hideBalances) }
        }

        // Recent transactions
        item {
            RecentSection(
                transactions = state.recentTransactions,
                hideBalances = state.hideBalances,
                onSeeAll = onNavigateToTransactions
            )
        }

        if (state.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun HeroSpendCard(
    monthly: Double, daily: Double, weekly: Double,
    projected: Double, dailyAvg: Double,
    hideBalances: Boolean, isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column {
            Text("This Month",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            if (isLoading) {
                Box(Modifier.height(48.dp).width(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant))
            } else {
                Text(
                    if (hideBalances) "••••••" else formatAmount(monthly),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (projected > 0 && !hideBalances) {
                Text("Projected: ${formatAmount(projected)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniStatCard("Today",    if (hideBalances) "••••" else formatAmount(daily),   Modifier.weight(1f))
                MiniStatCard("This Week",if (hideBalances) "••••" else formatAmount(weekly),  Modifier.weight(1f))
                MiniStatCard("Daily Avg",if (hideBalances) "••••" else formatAmount(dailyAvg),Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun MiniStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun AlertsSection(alerts: List<SpendingAlert>) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        alerts.take(3).forEach { alert ->
            val color = when (alert.type) {
                AlertType.BUDGET_EXCEEDED -> MaterialTheme.colorScheme.error
                AlertType.BUDGET_WARNING  -> Color(0xFFFF9800)
                else -> MaterialTheme.colorScheme.primary
            }
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (alert.type == AlertType.BUDGET_EXCEEDED) Icons.Default.Warning else Icons.Default.Info,
                        null, tint = color, modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(alert.message, style = MaterialTheme.typography.bodySmall, color = color)
                }
            }
        }
    }
}

@Composable
fun SpendingDonutChart(
    summaries: List<CategorySummary>,
    total: Double,
    hideBalances: Boolean,
    onSeeAll: () -> Unit
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(summaries) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(900, easing = EaseOutCubic))
    }

    Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Spending Breakdown", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onSeeAll) { Text("Budgets") }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                // Donut
                DonutChart(summaries, total, anim.value, hideBalances, 150.dp)
                // Legend
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    summaries.take(6).forEach { s ->
                        val color = Color(s.category.color)
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                            Text(s.category.displayName, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f), maxLines = 1)
                            if (!hideBalances) {
                                Text(formatAmount(s.totalAmount), style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(
    summaries: List<CategorySummary>,
    total: Double,
    animProgress: Float,
    hideBalances: Boolean,
    size: Dp
) {
    val sweeps = if (total > 0)
        summaries.map { ((it.totalAmount / total) * 360f * animProgress).toFloat() }
    else summaries.map { 0f }

    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            var startAngle = -90f
            sweeps.forEachIndexed { i, sweep ->
                drawArc(
                    color = Color(summaries[i].category.color),
                    startAngle = startAngle,
                    sweepAngle = (sweep - 2f).coerceAtLeast(0f),
                    useCenter = false,
                    style = Stroke(width = 30f, cap = StrokeCap.Round)
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (hideBalances) "••••" else formatAmount(total * animProgress),
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("total", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TopMerchantsSection(merchants: List<MerchantSummary>, hideBalances: Boolean) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Top Merchants", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        merchants.forEach { m ->
            val color = Color(m.category.color)
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center) {
                    Text(m.category.emoji, fontSize = 16.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(m.name, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium, maxLines = 1)
                    Text("${m.count} transactions", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!hideBalances) {
                    Text(formatAmount(m.totalAmount), style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold, color = color)
                }
            }
        }
    }
}

@Composable
fun RecentSection(
    transactions: List<Transaction>,
    hideBalances: Boolean,
    onSeeAll: () -> Unit
) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("Recent Transactions", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onSeeAll) { Text("See All") }
        }
        if (transactions.isEmpty()) {
            EmptyState("📭", "No transactions yet", "SMS auto-import is active")
        } else {
            transactions.forEach { t ->
                TransactionRow(t, hideBalances)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
fun TransactionRow(
    transaction: Transaction,
    hideBalances: Boolean,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val color = Color(transaction.category.color)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
            .let { if (onClick != null) it.clickable { onClick() } else it },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center) {
            Text(transaction.category.emoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(transaction.merchant, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(transaction.category.displayName, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("•", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(transaction.dateTime.format(DateTimeFormatter.ofPattern("MMM d, h:mm a")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (transaction.accountLast4.isNotBlank()) {
                    Text("• ••••${transaction.accountLast4}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (hideBalances) "••••"
                else "-${formatAmount(transaction.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            if (transaction.isRecurring) {
                Text("recurring", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
        if (onEdit != null || onDelete != null) {
            Spacer(Modifier.width(4.dp))
            Column {
                onEdit?.let {
                    IconButton(onClick = it, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    }
                }
                onDelete?.let {
                    IconButton(onClick = it, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(emoji: String, title: String, subtitle: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Keep CategoryColors for compatibility
val CategoryColors = ExpenseCategory.values().associate { it.name to Color(it.color) }
