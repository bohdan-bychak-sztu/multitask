package com.bbm.multitask

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.bbm.multitask.ui.lsbMethod.LsbMethod

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "multitask",
    ) {
        App(
            desktopExtras = {
                LsbMethod()
            }
        )
    }
}