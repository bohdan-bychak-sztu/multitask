package com.bbm.multitask.ui.pvdMethod

import com.bbm.multitask.utils.platformImage.PlatformImage
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write

actual suspend fun saveFile(file: PlatformImage?): String? {
    try {
        val fileSaver =
            FileKit.openFileSaver(suggestedName = "result", extension = "png")
        fileSaver?.write(file!!.getBytes())
        return fileSaver?.file?.path

    } catch (e: Exception) {
        println("Помилка збереження: ${e.message}")
    }
    return null
}

actual fun openFileWithDefaultApplication(path: String) {
    try {
        java.awt.Desktop.getDesktop().open(java.io.File(path))
    } catch (e: Exception) {
        println("Помилка відкриття файлу: ${e.message}")
    }
}