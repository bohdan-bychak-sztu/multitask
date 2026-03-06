package com.bbm.multitask.ui.lsbMethod

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


data class LsbMethodState(
    val pathToImage: String = "",
    val numberOfBitsToHide: Int = 1,
    val action: String = "Encrypt",
    val result: String = "",
    val text: String = "",
    val outputPath: String = "",
    val outputImage: PlatformImage? = null,
    val secretWord: String? = null,
)

sealed interface LsbMethodEvent {
    data class UpdateText(val text: String) : LsbMethodEvent
    data class UpdatePathToImage(val path: String) : LsbMethodEvent
    data class UpdateNumberOfBitsToHide(val numberOfBitsToHide: String) : LsbMethodEvent
    data class UpdateAction(val action: String) : LsbMethodEvent
    data class UpdateOutputPath(val path: String) : LsbMethodEvent
    data class UpdateSecretWord(val secretWord: String?) : LsbMethodEvent
    object ProcessImage : LsbMethodEvent
}

class LsbMethodViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LsbMethodState())
    val uiState: StateFlow<LsbMethodState> = _uiState.asStateFlow()

    fun onEvent(event: LsbMethodEvent) {
        when (event) {
            is LsbMethodEvent.UpdatePathToImage -> {
                _uiState.update { it.copy(pathToImage = event.path, outputImage = null, result = "", outputPath = "") }
            }

            is LsbMethodEvent.UpdateNumberOfBitsToHide -> {
                _uiState.update { it.copy(numberOfBitsToHide = event.numberOfBitsToHide.toInt()) }
            }

            is LsbMethodEvent.UpdateAction -> {
                _uiState.update { it.copy(action = event.action) }
            }

            is LsbMethodEvent.UpdateText -> {
                _uiState.update { it.copy(text = event.text) }
            }

            is LsbMethodEvent.UpdateOutputPath -> {
                _uiState.update { it.copy(outputPath = event.path) }
            }
            is LsbMethodEvent.UpdateSecretWord -> {
                _uiState.update { it.copy(secretWord = event.secretWord) }
            }

            is LsbMethodEvent.ProcessImage -> {
                _uiState.update { it.copy(result = "", outputPath = "", outputImage = null) }
                try {
                    if (_uiState.value.action == "Encrypt")
                        _uiState.update {
                            it.copy(
                                outputImage =
                                    encodeImage(
                                        _uiState.value.pathToImage,
                                        _uiState.value.text,
                                        _uiState.value.numberOfBitsToHide,
                                        _uiState.value.secretWord
                                    )
                            )
                        }
                    else if (_uiState.value.action == "Decrypt")
                        _uiState.update {
                            it.copy(
                                result = decodeImage(
                                    _uiState.value.pathToImage,
                                    _uiState.value.numberOfBitsToHide,
                                    _uiState.value.secretWord
                                )
                            )
                        }
                } catch (e: Exception) {
                    _uiState.update { it.copy(result = "Error: ${e.message}") }
                }
            }


            else -> {}
        }

    }
}
