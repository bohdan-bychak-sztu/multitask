package com.bbm.multitask.ui.pvdMethod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbm.multitask.utils.platformImage.PlatformImage
import com.bbm.multitask.utils.platformImage.PvdProcessor
import com.bbm.multitask.utils.platformImage.loadPlatformImage
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


data class PvdMethodState(
    val pathToImage: String? = null,
    val image: PlatformFile? = null,
    val action: String = "Encrypt",
    val result: String = "",
    val text: String = "",
    val outputPath: String? = null,
    val outputImage: PlatformImage? = null,
)

sealed interface PvdMethodEvent {
    data class UpdateText(val text: String) : PvdMethodEvent
    data class UpdatePathToImage(val path: String) : PvdMethodEvent
    data class UpdateAction(val action: String) : PvdMethodEvent
    data class UpdateOutputPath(val path: String?) : PvdMethodEvent
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

            is PvdMethodEvent.ProcessImage -> {
                val currentState = _uiState.value
                val imageFile = currentState.image
                val text = currentState.text
                val action = currentState.action
                val outputPath = currentState.outputPath

                if (imageFile != null && (text.isNotEmpty() || action == "Decrypt")) {
                    viewModelScope.launch {
                        try {
                            _uiState.update { it.copy(result = "Processing...") }

                            withContext(Dispatchers.Default) {
                                val image = loadPlatformImage(imageFile)
                                val pvdProcessor = PvdProcessor(image)

                                if (action == "Encrypt") {
                                    pvdProcessor.embedData(text.encodeToByteArray())
                                    _uiState.update { it.copy(outputImage = pvdProcessor.image, result = "") }
                                } else if (action == "Decrypt") {
                                    val extractedData = pvdProcessor.extractData()
                                    val extractedText = extractedData.decodeToString()
                                    _uiState.update { it.copy(result = extractedText) }
                                }
                            }
                        } catch (e: Exception) {
                            _uiState.update { it.copy(result = "Error: ${e.message}") }
                        }
                    }
                } else {
                    _uiState.update { it.copy(result = "Please fill all fields") }
                }
            }

            else -> {}
        }

    }
}
