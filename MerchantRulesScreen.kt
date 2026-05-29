package com.expensetracker.presentation.settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.MerchantRule
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MerchantRulesScreen(viewModel:MerchantRulesViewModel=hiltViewModel()){
    val state by viewModel.state.collectAsState()
    Scaffold(topBar={TopAppBar(title={Text("Merchant Rules",fontWeight=FontWeight.Bold)})}){padding->
        if(state.rules.isEmpty())Box(Modifier.fillMaxSize().padding(padding),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Text("🏪",fontSize=48.sp);Spacer(Modifier.height(8.dp));Text("No rules yet",style=MaterialTheme.typography.titleSmall);Spacer(Modifier.height(4.dp));Text("Rules are created when you recategorize a transaction.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
        else LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            item{Text("${state.rules.size} rules",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            items(state.rules,key={it.id}){rule->val color=Color(rule.category.color)
                Card(shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){
                    Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Text(rule.category.emoji,fontSize=22.sp);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(rule.displayName,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold);Text("→ ${rule.category.displayName}",style=MaterialTheme.typography.labelSmall,color=color);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){if(rule.applyToFuture)AssistChip(onClick={},label={Text("Future",fontSize=10.sp)},modifier=Modifier.height(22.dp));if(rule.applyToPast)AssistChip(onClick={},label={Text("Past",fontSize=10.sp)},modifier=Modifier.height(22.dp))}};IconButton(onClick={viewModel.deleteRule(rule)}){Icon(Icons.Default.Delete,null,tint=MaterialTheme.colorScheme.error)}}
                }
            }
            item{Spacer(Modifier.height(40.dp))}
        }
    }
}
