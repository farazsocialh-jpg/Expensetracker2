package com.expensetracker

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.expensetracker.data.repository.SettingsRepository
import com.expensetracker.presentation.SmsPermissionScreen
import com.expensetracker.presentation.budget.BudgetScreen
import com.expensetracker.presentation.dashboard.DashboardScreen
import com.expensetracker.presentation.settings.MerchantRulesScreen
import com.expensetracker.presentation.settings.SettingsScreen
import com.expensetracker.presentation.transactions.TransactionsScreen
import com.expensetracker.presentation.ui.theme.ExpenseTrackerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard    : Screen("dashboard",    "Home",         Icons.Default.Home)
    object Transactions : Screen("transactions", "Transactions", Icons.Default.List)
    object Budget       : Screen("budget",       "Budget",       Icons.Default.AccountBalance)
    object Settings     : Screen("settings",     "Settings",     Icons.Default.Settings)
    object Rules        : Screen("rules",        "Rules",        Icons.Default.AutoFixHigh)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = com.expensetracker.domain.model.AppSettings())

            ExpenseTrackerTheme(darkTheme = settings.darkTheme, amoledTheme = settings.amoledTheme) {
                val smsPerms = rememberMultiplePermissionsState(
                    listOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                )
                if (!smsPerms.allPermissionsGranted) {
                    SmsPermissionScreen(onGrantPermission = { smsPerms.launchMultiplePermissionRequest() })
                } else {
                    MainApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    val bottomScreens = listOf(Screen.Dashboard, Screen.Transactions, Screen.Budget, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val entry by navController.currentBackStackEntryAsState()
                val current = entry?.destination
                bottomScreens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, screen.label) },
                        label = { Text(screen.label) },
                        selected = current?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = Screen.Dashboard.route, Modifier.padding(padding)) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                    onNavigateToBudget = { navController.navigate(Screen.Budget.route) }
                )
            }
            composable(Screen.Transactions.route) { TransactionsScreen() }
            composable(Screen.Budget.route) { BudgetScreen() }
            composable(Screen.Settings.route) {
                SettingsScreen(onNavigateToRules = { navController.navigate(Screen.Rules.route) })
            }
            composable(Screen.Rules.route) { MerchantRulesScreen() }
        }
    }
}
