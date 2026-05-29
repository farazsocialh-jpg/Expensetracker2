package com.expensetracker.presentation.dashboard
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.*
import java.time.format.DateTimeFormatter

fun formatAmount(amount:Double,currency:String="QAR"):String="$currency ${"%,.2f".format(amount)}"

@Composable fun DashboardScreen(onNavigateToTransactions:()->Unit,onNavigateToBudget:()->Unit,onNavigateToAnalytics:()->Unit,viewModel:DashboardViewModel=hiltViewModel()){
    val state by viewModel.state.collectAsState()
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),contentPadding=PaddingValues(bottom=100.dp)){
        item{HeroCard(state)}
        if(state.stats.alerts.isNotEmpty())item{AlertsSection(state.stats.alerts)}
        if(state.wallets.isNotEmpty())item{WalletsRow(state.wallets,state.currency,state.hideBalances)}
        if(state.stats.categorySummaries.isNotEmpty())item{SpendingBreakdown(state,onNavigateToBudget)}
        if(state.stats.weeklySpends.isNotEmpty())item{WeeklyChart(state.stats.weeklySpends,state.hideBalances)}
        if(state.goals.isNotEmpty())item{GoalsSection(state.goals)}
        item{RecentSection(state.recentTransactions,state.currency,state.hideBalances,onNavigateToTransactions)}
    }
}

@Composable fun HeroCard(state:DashboardUiState){
    Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha=0.18f),MaterialTheme.colorScheme.background))).padding(20.dp)){
        Column{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                Column{
                    Text("This Month",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    if(state.isLoading)Box(Modifier.height(44.dp).width(160.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                    else Text(if(state.hideBalances)"•••••••" else formatAmount(state.stats.monthlyTotal,state.currency),style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold)
                    if(!state.hideBalances&&state.stats.projectedMonthly>0)Text("Projected: ${formatAmount(state.stats.projectedMonthly,state.currency)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if(state.stats.financialHealthScore>0){
                    val sc=state.stats.financialHealthScore
                    val c=when{sc>=75->Color(0xFF43A047);sc>=50->Color(0xFFFF9800);else->Color(0xFFF44336)}
                    Box(Modifier.size(60.dp).clip(CircleShape).background(c.copy(alpha=0.15f)),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Text("$sc",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=c);Text("health",style=MaterialTheme.typography.labelSmall,color=c)}}
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                MiniCard("Today",if(state.hideBalances)"•••" else formatAmount(state.stats.dailyTotal,state.currency),Modifier.weight(1f))
                MiniCard("This Week",if(state.hideBalances)"•••" else formatAmount(state.stats.weeklyTotal,state.currency),Modifier.weight(1f))
                MiniCard("Daily Avg",if(state.hideBalances)"•••" else formatAmount(state.stats.dailyAverage,state.currency),Modifier.weight(1f))
            }
            if(!state.hideBalances&&state.stats.monthlyIncome>0){
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    MiniCard("Income",formatAmount(state.stats.monthlyIncome,state.currency),Modifier.weight(1f),Color(0xFF43A047))
                    MiniCard("Saved",formatAmount((state.stats.monthlyIncome-state.stats.monthlyTotal).coerceAtLeast(0.0),state.currency),Modifier.weight(1f),Color(0xFF1565C0))
                    MiniCard("Save %","${state.stats.savingsRate.toInt()}%",Modifier.weight(1f),MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable fun MiniCard(label:String,value:String,modifier:Modifier,color:Color=MaterialTheme.colorScheme.primary){
    Card(modifier,shape=RoundedCornerShape(12.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){
        Column(Modifier.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(value,style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Bold,color=color,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
    }
}

@Composable fun AlertsSection(alerts:List<SpendingAlert>){
    Column(Modifier.padding(horizontal=20.dp,vertical=4.dp)){
        alerts.take(3).forEach{alert->
            val color=when(alert.type){AlertType.BUDGET_EXCEEDED->MaterialTheme.colorScheme.error;AlertType.BUDGET_WARNING->Color(0xFFFF9800);else->MaterialTheme.colorScheme.primary}
            Card(Modifier.fillMaxWidth().padding(vertical=3.dp),shape=RoundedCornerShape(10.dp),colors=CardDefaults.cardColors(containerColor=color.copy(alpha=0.1f))){
                Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                    Icon(if(alert.type==AlertType.BUDGET_EXCEEDED)Icons.Default.Warning else Icons.Default.Info,null,tint=color,modifier=Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(alert.message,style=MaterialTheme.typography.bodySmall,color=color)
                }
            }
        }
    }
}

@Composable fun WalletsRow(wallets:List<Wallet>,currency:String,hide:Boolean){
    Column(Modifier.padding(horizontal=20.dp,vertical=8.dp)){
        Text("Accounts",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)){
            items(wallets){w->val color=Color(w.color)
                Card(Modifier.width(150.dp),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=color.copy(alpha=0.15f))){
                    Column(Modifier.padding(14.dp)){Text(w.emoji,fontSize=22.sp);Spacer(Modifier.height(6.dp));Text(w.name,style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.SemiBold,maxLines=1);Text(if(hide)"•••••" else formatAmount(w.balance,w.currency),style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Bold,color=color)}
                }
            }
        }
    }
}

@Composable fun SpendingBreakdown(state:DashboardUiState,onBudgets:()->Unit){
    val anim=remember{Animatable(0f)};LaunchedEffect(state.stats.categorySummaries){anim.snapTo(0f);anim.animateTo(1f,tween(900,easing=EaseOutCubic))}
    Card(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=8.dp),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){
        Column(Modifier.padding(20.dp)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Spending Breakdown",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);TextButton(onClick=onBudgets){Text("Budgets")}}
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)){
                DonutChart(state.stats.categorySummaries,state.stats.monthlyTotal,anim.value,140.dp)
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){
                    state.stats.categorySummaries.take(6).forEach{s->val color=Color(s.category.color)
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                            Text(s.category.displayName,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.weight(1f),maxLines=1)
                            if(!state.hideBalances)Text(formatAmount(s.totalAmount,state.currency),style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable fun DonutChart(summaries:List<CategorySummary>,total:Double,animProgress:Float,size:Dp){
    val sweeps=if(total>0)summaries.map{((it.totalAmount/total)*360f*animProgress).toFloat()} else summaries.map{0f}
    Box(Modifier.size(size),contentAlignment=Alignment.Center){
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()){var start=-90f;sweeps.forEachIndexed{i,sweep->drawArc(Color(summaries[i].category.color),start,(sweep-2f).coerceAtLeast(0f),false,style=Stroke(28f,cap=StrokeCap.Round));start+=sweep}}
        Column(horizontalAlignment=Alignment.CenterHorizontally){Text(formatAmount(total*animProgress),style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold);Text("total",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
}

@Composable fun WeeklyChart(spends:List<DailySpend>,hide:Boolean){
    if(spends.isEmpty())return
    val maxAmt=spends.maxOfOrNull{it.amount}?:1.0
    Card(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=8.dp),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){
        Column(Modifier.padding(20.dp)){
            Text("7-Day Trend",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().height(80.dp),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.SpaceEvenly){
                spends.takeLast(7).forEach{day->val frac=if(maxAmt>0)(day.amount/maxAmt).toFloat() else 0f
                    Column(horizontalAlignment=Alignment.CenterHorizontally){
                        Box(Modifier.width(28.dp).fillMaxHeight(),contentAlignment=Alignment.BottomCenter){Box(Modifier.width(20.dp).fillMaxHeight(frac.coerceAtLeast(0.05f)).clip(RoundedCornerShape(topStart=6.dp,topEnd=6.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha=0.8f)))}
                    }
                }
            }
            if(!hide){Spacer(Modifier.height(4.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){spends.takeLast(7).forEach{day->Text(day.date.takeLast(5).take(2),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
        }
    }
}

@Composable fun GoalsSection(goals:List<SavingsGoal>){
    Column(Modifier.padding(horizontal=20.dp,vertical=8.dp)){
        Text("Savings Goals",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(10.dp))
        goals.forEach{goal->val color=Color(goal.color)
            Card(Modifier.fillMaxWidth().padding(vertical=4.dp),shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){
                Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                    Text(goal.emoji,fontSize=24.sp);Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)){
                        Text(goal.name,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(progress={goal.progress},modifier=Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),color=color,trackColor=color.copy(alpha=0.2f))
                        Spacer(Modifier.height(2.dp))
                        Text("${(goal.progress*100).toInt()}% · ${formatAmount(goal.remaining)} remaining",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable fun RecentSection(transactions:List<Transaction>,currency:String,hide:Boolean,onSeeAll:()->Unit){
    Column(Modifier.padding(horizontal=20.dp,vertical=8.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Recent",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);TextButton(onClick=onSeeAll){Text("See All")}}
        if(transactions.isEmpty()){Column(Modifier.fillMaxWidth().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("📭",fontSize=40.sp);Spacer(Modifier.height(8.dp));Text("No transactions yet",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("SMS auto-import is active",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
        else{transactions.forEach{t->TransactionRow(t,currency,hide);HorizontalDivider(color=MaterialTheme.colorScheme.outline.copy(alpha=0.2f))}}
    }
}

@Composable fun TransactionRow(t:Transaction,currency:String="QAR",hide:Boolean=false,onClick:(()->Unit)?=null){
    val color=Color(t.category.color);val isIncome=t.transactionType==TransactionType.INCOME
    Row(Modifier.fillMaxWidth().padding(vertical=10.dp).let{if(onClick!=null)it.clickable{onClick()}else it},verticalAlignment=Alignment.CenterVertically){
        Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha=0.15f)),contentAlignment=Alignment.Center){Text(t.category.emoji,fontSize=18.sp)}
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)){
            Text(t.merchant,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                Text(t.category.displayName,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text("•",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text(t.dateTime.format(DateTimeFormatter.ofPattern("MMM d, h:mm a")),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(t.accountLast4.isNotBlank())Text("• ••••${t.accountLast4}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(horizontalAlignment=Alignment.End){
            Text(if(hide)"•••" else "${if(isIncome)"+" else "-"}${formatAmount(t.amount,currency)}",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Bold,color=if(isIncome)Color(0xFF43A047) else MaterialTheme.colorScheme.error)
            if(t.isRecurring)Text("recurring",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
        }
    }
}
