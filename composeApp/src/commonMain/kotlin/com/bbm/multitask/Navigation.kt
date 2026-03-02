package com.bbm.multitask

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataExploration
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bbm.multitask.ui.lPGraphicalMethod.LPGraphicalMethod
import com.bbm.multitask.ui.main.MainScreen
import kotlinx.serialization.Serializable

@Serializable
object LsbMethod

@Serializable
object MainScr

@Serializable
object LPGraphicalMethod

sealed class TopLevelRoute<T : Any>(val name: String, val route: T, val icon: ImageVector) {
    object MainScreen : TopLevelRoute<MainScr>("Головна", MainScr, icon = Icons.Default.SelectAll)
    object LsbMethodScreen : TopLevelRoute<LsbMethod>("Метод LSB", LsbMethod, icon = Icons.Default.DataExploration)
    object LPGraphicalMethodScreen :
        TopLevelRoute<LPGraphicalMethod>("Графічний метод", LPGraphicalMethod, icon = Icons.Default.GraphicEq)
}

val navItems = listOf(
    TopLevelRoute.MainScreen,
    TopLevelRoute.LsbMethodScreen,
    TopLevelRoute.LPGraphicalMethodScreen,
)

@Composable
fun AppNavigation(desktopExtras: @Composable () -> Unit = {}) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination

    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail(
            /*header = {
                FloatingActionButton(onClick = { /* ... */ }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }*/
        ) {
            navItems.forEach { item ->
                val isSelected = currentRoute?.hierarchy?.any { it.hasRoute(item.route::class) } == true

                NavigationRailItem(
                    selected = isSelected,
                    onClick = { navController.navigate(item.route) },
                    icon = { Icon(item.icon, contentDescription = item.name) },
                    label = { Text(item.name) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            NavHost(navController, startDestination = MainScr) {
                composable<MainScr> { MainScreen() }
                composable<LsbMethod> { desktopExtras() }
                composable<LPGraphicalMethod> { LPGraphicalMethod() }
            }
        }
    }
}