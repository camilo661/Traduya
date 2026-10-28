package com.traduya.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.traduya.feature.dialer.DialerRoute
import com.traduya.feature.history.detail.HistoryDetailRoute
import com.traduya.feature.history.detail.HistoryDetailViewModel
import com.traduya.feature.history.list.HistoryRoute
import com.traduya.feature.settings.SettingsRoute

@Composable
fun TraduyaMainScaffold(dialNumber: String?) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopLevelDestination.entries.any { it.route == currentDestination?.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTopLevel(destination) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DIALER,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.DIALER) {
                DialerRoute(
                    initialNumber = dialNumber,
                    onOpenSettings = { navController.navigateToTopLevel(TopLevelDestination.SETTINGS) },
                )
            }
            composable(Routes.HISTORY) {
                HistoryRoute(onOpenCall = { navController.navigate(Routes.callDetail(it)) })
            }
            composable(Routes.SETTINGS) { SettingsRoute() }
            composable(
                route = Routes.CALL_DETAIL,
                arguments = listOf(navArgument(HistoryDetailViewModel.CALL_ID_ARG) { type = NavType.LongType }),
            ) {
                HistoryDetailRoute(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
