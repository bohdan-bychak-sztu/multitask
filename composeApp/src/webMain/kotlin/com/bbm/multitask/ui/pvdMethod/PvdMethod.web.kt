package com.bbm.multitask.ui.pvdMethod

import com.bbm.multitask.utils.platformImage.PlatformImage
import io.github.vinceglb.filekit.PlatformFile

actual suspend fun saveFile(file: PlatformImage?) : String?{
    /*try {
        val file =
            FileKit.openFileSaver(suggestedName = "result", extension = "png")
        file?.write(file!!.getBytes())
        viewModel.onEvent(
            PvdMethodEvent.UpdateOutputPath(
                file?.absolutePath() ?: ""
            )
        )
        scope.launch {
            snackbarHostState.showSnackbar(
                "Image saved successfully!",
                duration = SnackbarDuration.Short
            )
        }
    } catch (e: Exception) {
        println("Помилка збереження: ${e.message}")
    }*/
    return null
}

actual fun openFileWithDefaultApplication(path: String){

}