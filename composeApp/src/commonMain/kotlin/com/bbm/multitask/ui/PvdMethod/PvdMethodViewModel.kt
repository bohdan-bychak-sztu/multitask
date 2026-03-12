package com.bbm.multitask.ui.PvdMethod

import androidx.lifecycle.ViewModel
import com.bbm.multitask.utils.platformImage.PlatformImage
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


data class PvdMethodState(
    val pathToImage: String? = null,
    val image: PlatformFile? = null,
    val action: String = "Encrypt",
    val result: String = "",
    val text: String = "",
    val outputPath: String = "",
    val outputImage: PlatformImage? = null,
)

sealed interface PvdMethodEvent {
    data class UpdateText(val text: String) : PvdMethodEvent
    data class UpdatePathToImage(val path: String) : PvdMethodEvent
    data class UpdateAction(val action: String) : PvdMethodEvent
    data class UpdateOutputPath(val path: String) : PvdMethodEvent
    data class UpdateImage(val platformFile: PlatformFile) : PvdMethodEvent
    object ProcessImage : PvdMethodEvent
}

class PvdMethodViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PvdMethodState())
    val uiState: StateFlow<PvdMethodState> = _uiState.asStateFlow()

    fun onEvent(event: PvdMethodEvent) {
        when (event) {
            is PvdMethodEvent.UpdatePathToImage -> {
                _uiState.update { it.copy(pathToImage = event.path, outputImage = null, result = "", outputPath = "")
                }
            }

            is PvdMethodEvent.UpdateImage -> {
                _uiState.update { it.copy(image = event.platformFile)
                }
            }

            is PvdMethodEvent.UpdateAction -> {
                _uiState.update { it.copy(action = event.action) }
            }

            is PvdMethodEvent.UpdateText -> {
                _uiState.update { it.copy(text = event.text) }
            }

            is PvdMethodEvent.UpdateOutputPath -> {
                _uiState.update { it.copy(outputPath = event.path) }
            }

            else -> {}
        }

    }
}
