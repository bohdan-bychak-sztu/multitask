package com.bbm.multitask.ui.lPGraphicalMethod

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.runtime.Composable

@Composable
actual fun ConstraintInputContextMenu(
    onDelete: () -> Unit,
    content: @Composable (() -> Unit),
) {
    ContextMenuArea(
        items = {
            listOf(
                ContextMenuItem("Copy") {

                },
                ContextMenuItem("Cut") {
                    onDelete()
                }
            )
        }
    ) {
        content()
    }
}