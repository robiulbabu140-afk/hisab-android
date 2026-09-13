package com.hisab.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.compose.runtime.getValue
import com.hisab.app.ui.components.HisabBottomBar
import com.hisab.app.ui.screens.*

@Composable
fun HisabNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != null && currentRoute in BOTTOM_NAV_ROUTES) {
                HisabBottomBar(currentRoute = currentRoute) { route ->
                    if (route == Dest.ADD) {
                        navController.navigate(Dest.ADD)
                    } else {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.SPLASH,
            modifier = Modifier.padding(padding)
        ) {
            composable(Dest.SPLASH) {
                SplashScreen(onGetStarted = { navController.navigate(Dest.ONBOARDING) })
            }
            composable(Dest.ONBOARDING) {
                OnboardingScreen(onDone = {
                    navController.navigate(Dest.DASHBOARD) {
                        popUpTo(Dest.SPLASH) { inclusive = true }
                    }
                })
            }
            composable(Dest.DASHBOARD) { DashboardScreen(navController) }
            composable(Dest.TRANSACTIONS) { TransactionsScreen(navController) }
            composable(
                Dest.TRANSACTION_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                TransactionDetailScreen(navController, entry.arguments?.getLong("id") ?: 0L)
            }
            composable(Dest.ADD) { AddTransactionScreen(navController) }
            composable(Dest.SMS_REVIEW) { SmsReviewScreen(navController) }
            composable(
                Dest.SMS_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                SmsDetailScreen(navController, entry.arguments?.getLong("id") ?: 0L)
            }
            composable(Dest.ACCOUNTS) { AccountsScreen(navController) }
            composable(
                Dest.ACCOUNT_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                AccountDetailScreen(navController, entry.arguments?.getLong("id") ?: 0L)
            }
            composable(Dest.CATEGORIES) { CategoriesScreen(navController) }
            composable(Dest.TRANSFER) { TransferScreen(navController) }
            composable(Dest.NEUTRAL) { NeutralScreen(navController) }
            composable(Dest.REPORTS) { ReportsScreen() }
            composable(Dest.MORE) { MoreScreen(navController) }
            composable(Dest.SETTINGS) { SettingsScreen(navController) }
            composable(Dest.CLIENTS) { ClientsScreen(navController) }
            composable(Dest.DOLLAR_SALES) { DollarSalesScreen(navController) }
            composable(Dest.MANAGED) { ManagedScreen(navController) }
            composable(Dest.SUPPLIERS) { SuppliersScreen(navController) }
        }
    }
}
