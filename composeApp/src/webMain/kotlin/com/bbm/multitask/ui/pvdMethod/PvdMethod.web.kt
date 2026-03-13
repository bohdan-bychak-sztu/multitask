package com.bbm.multitask.ui.pvdMethod

import com.bbm.multitask.utils.platformImage.PlatformImage
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.download

actual suspend fun saveFile(file: PlatformImage?): String? {
    try {
        println(file.toString())
        FileKit.download(file!!.getBytes(), "pvd_method_image.png")

        return null

    } catch (e: Exception) {
        println("Помилка збереження: ${e.message}")
    }
    return null
}

actual fun openFileWithDefaultApplication(path: String) {

}