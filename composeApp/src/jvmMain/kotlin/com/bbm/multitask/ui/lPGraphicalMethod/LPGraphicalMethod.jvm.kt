package com.bbm.multitask.ui.lPGraphicalMethod

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.runtime.Composable

@Composable
actual fun ConstraintAddContextMenu(
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onDeleteAll: () -> Unit,
    content: @Composable (() -> Unit)
) {
    ContextMenuArea(
        items = {
            listOf(
                ContextMenuItem("Copy") {
                    onCopy()
                },
                ContextMenuItem("Paste") {
                    onPaste()
                },
                ContextMenuItem("Delete All") {
                    onDeleteAll()
                }
            )
        }
    ) {
        content()
    }
}