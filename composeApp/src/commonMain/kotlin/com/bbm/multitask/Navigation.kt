package com.bbm.multitask

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bbm.multitask.ui.pvdMethod.PvdMethod
import com.bbm.multitask.ui.l3ib.L3ib
import com.bbm.multitask.ui.lPGraphicalMethod.LPGraphicalMethod
import com.bbm.multitask.ui.main.MainScreen
import kotlinx.serialization.Serializable

@Serializable
object LsbMethod

@Serializable
object MainScr

@Serializable
object LPGraphicalMethod

@Serializable
object L3ibMethod

@Serializable
object PvdMethod

sealed class TopLevelRoute<T : Any>(
    val name: String,
    val route: T,
    val icon: ImageVector,
    val contentDescription: String = ""
) {
    object MainScreen : TopLevelRoute<MainScr>(
        "Головна",
        MainScr,
        icon = Icons.Default.SelectAll,
        contentDescription = "Головна сторінка"
    )

    object LsbMethodScreen : TopLevelRoute<LsbMethod>(
        "Метод LSB",
        LsbMethod,
        icon = Icons.Default.ImageAspectRatio,
        contentDescription = "Метод LSB для приховування даних в зображеннях"
    )

    object L3ibScreen :
        TopLevelRoute<L3ibMethod>(
            "Методи шифрування",
            L3ibMethod,
            icon = Icons.Default.EnhancedEncryption,
            contentDescription = "Методи шифрування: заміна, перестановка, гамування"
        )

    object LPGraphicalMethodScreen :
        TopLevelRoute<LPGraphicalMethod>(
            "Графічний метод",
            LPGraphicalMethod,
            icon = Icons.Default.GraphicEq,
            contentDescription = "Графічний метод розв'язання задач лінійного програмування"
        )

    object PvdMethodScreen :
        TopLevelRoute<PvdMethod>(
            "Метод PVD",
            PvdMethod,
            icon = Icons.Default.HideImage,
            contentDescription = "Метод PVD для приховування даних в зображеннях"
        )
}

val navItems = listOf(
    TopLevelRoute.PvdMethodScreen,
    TopLevelRoute.LsbMethodScreen,
    TopLevelRoute.LPGraphicalMethodScreen,
    TopLevelRoute.L3ibScreen,
)

@Composable
fun AppNavigation(desktopExtras: @Composable () -> Unit = {}) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination

    Column(modifier = Modifier.fillMaxSize()) {
        /*NavigationRail(
            /*header = {
                FloatingActionButton(onClick = { /* ... */ }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }*/
            windowInsets = WindowInsets.statusBars
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
        }*/

        TopBar(navController)

        Box(modifier = Modifier.weight(1f)) {
            NavHost(navController, startDestination = MainScr) {
                composable<MainScr> { MainScreen(navItems, navController) }
                composable<LsbMethod> { desktopExtras() }
                composable<LPGraphicalMethod> { LPGraphicalMethod() }
                composable<L3ibMethod> { L3ib() }
                composable<PvdMethod> { PvdMethod() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TopBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentRouteName = navItems.find { item ->
        currentDestination?.hierarchy?.any { it.route == item.route::class.qualifiedName } == true
    }?.name ?: TopLevelRoute.MainScreen.name

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color(0xFF212121)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
            Icon(
                Icons.Default.TaskAlt,
                contentDescription = "Logo",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("MultiTask", color = Color.White, style = MaterialTheme.typography.titleSmallEmphasized)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                currentRouteName,
                color = Color.LightGray,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                navController.navigate(MainScr)
            }) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Home",
                    tint = Color.LightGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}