package com.bbm.multitask.ui.leftMenu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import multitask.composeapp.generated.resources.Res
import multitask.composeapp.generated.resources.stenography
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeftMenuView(
    viewModel: LeftMenuViewModel = LeftMenuViewModel()
) {
    Column(
        modifier =
            Modifier
                .fillMaxHeight()
                .background(color = MaterialTheme.colorScheme.primaryContainer)
    ) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Right),
            tooltip = {
                PlainTooltip { Text("Завдання з Стеганографії") }
            },
            state = rememberTooltipState()
        ) {
            IconButton(
                onClick = {},
            ) {
                Icon(painter = painterResource(Res.drawable.stenography), "Stenography Icon")
            }
        }
    }
}