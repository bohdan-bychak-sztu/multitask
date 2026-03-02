package com.bbm.multitask.ui.lPGraphicalMethod

import androidx.compose.runtime.Composable

@Composable
actual fun ConstraintInputContextMenu(
    onDelete: () -> Unit,
    content: @Composable (() -> Unit),
){
    content()
}