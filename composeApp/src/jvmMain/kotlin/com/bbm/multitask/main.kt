package com.bbm.multitask

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Task
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.bbm.multitask.ui.lsbMethod.LsbMethod
import java.awt.Dimension

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "multitask",
    ) {
        window.minimumSize = Dimension(800, 600)
        App(
            desktopExtras = {
                LsbMethod()
            }
        )
    }
}