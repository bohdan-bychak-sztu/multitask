package com.bbm.multitask.ui.lPGraphicalMethod

import androidx.compose.runtime.Composable

@Composable
actual fun ConstraintAddContextMenu(
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onDeleteAll: () -> Unit,
    content: @Composable (() -> Unit)
) {
    content()
}