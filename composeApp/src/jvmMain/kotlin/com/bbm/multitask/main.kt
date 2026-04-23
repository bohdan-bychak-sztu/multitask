package com.bbm.multitask

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.bbm.multitask.ui.lsbMethod.LsbMethod
import com.bbm.multitask.ui.md5Algo.MD5Algo
import com.bbm.multitask.ui.rsaAlgo.RsaAlgo
import java.awt.Dimension

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "multitask",
    ) {
        window.minimumSize = Dimension(800, 600)
        App(
            lsb = {
                LsbMethod()
            },
            rsa = {
                RsaAlgo()
            },
            md5 = {
                MD5Algo()
            }
        )
    }
}