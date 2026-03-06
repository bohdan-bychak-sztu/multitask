package com.bbm.multitask.ui.main

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bbm.multitask.TopLevelRoute
import com.bbm.multitask.ui.components.fadingEdge
import kotlin.math.min

@Composable
fun MainScreen(
    navItems: List<TopLevelRoute<*>>,
    navController: NavHostController,
    viewModel: MainScreenViewModel = MainScreenViewModel()
) {
    val scrollState = rememberScrollState()

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        for (item in navItems) {
            ScreenCard(
                topLevelRoute = item,
                onClick = { route ->
                    navController.navigate(route)
                }
            )
        }
    }
}

@Composable
fun ScreenCard(
    topLevelRoute: TopLevelRoute<*>,
    onClick: (Any) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .size(180.dp)
            .clip(MaterialTheme.shapes.medium)
            .padding(4.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable {
                onClick(topLevelRoute.route)
            }
            .verticalScroll(scrollState)
            .fadingEdge(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = topLevelRoute.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(8.dp),
            textAlign = TextAlign.Center
        )
        Icon(topLevelRoute.icon, contentDescription = null, Modifier.size(48.dp))
        Text(
            text = topLevelRoute.contentDescription,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(8.dp),
            textAlign = TextAlign.Center
        )
    }
}

