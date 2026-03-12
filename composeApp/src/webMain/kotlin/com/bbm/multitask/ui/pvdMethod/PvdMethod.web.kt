package com.bbm.multitask.ui.pvdMethod

import com.bbm.multitask.utils.platformImage.PlatformImage
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker

actual suspend fun saveFile(file: PlatformImage?): String? {
    try {
        val filePath = FileKit.openFilePicker(
            mode = FileKitMode.Single,
            type = FileKitType.File(listOf("png"))
        )

        file?.save(filePath.toString())

        return filePath.toString()

    } catch (e: Exception) {
        println("Помилка збереження: ${e.message}")
    }
    return null
}

actual fun openFileWithDefaultApplication(path: String) {

}