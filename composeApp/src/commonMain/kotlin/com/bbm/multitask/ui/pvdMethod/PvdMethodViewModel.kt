package com.bbm.multitask.ui.pvdMethod

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbm.multitask.utils.platformImage.ColorChannel
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
    val action: String = "Embed",
    val result: String = "",
    val text: String = "",
    val outputPath: String? = null,
    val outputImage: PlatformImage? = null,
    val selectedColorChannels: Set<Int> = setOf(0, 1, 2),

    val p1: String = "",
    val p2: String = "",
    val charToEmbed: String = "",
    val pvdCalcResult: String = ""
)

sealed interface PvdMethodEvent {
    data class UpdateText(val text: String) : PvdMethodEvent
    data class UpdatePathToImage(val path: String) : PvdMethodEvent
    data class UpdateAction(val action: String) : PvdMethodEvent
    data class UpdateOutputPath(val path: String?) : PvdMethodEvent
    data class UpdateImage(val platformFile: PlatformFile) : PvdMethodEvent
    data class UpdateSelectedColorChannels(val channels: Set<Int>) : PvdMethodEvent
    object ProcessImage : PvdMethodEvent
    data class CalculatePvd(val p1: String, val p2: String, val char: String) : PvdMethodEvent
    data class ExtractPvd(val p1: String, val p2: String) : PvdMethodEvent
}

class PvdMethodViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PvdMethodState())
    val uiState: StateFlow<PvdMethodState> = _uiState.asStateFlow()

    fun onEvent(event: PvdMethodEvent) {
        when (event) {
            is PvdMethodEvent.UpdatePathToImage -> {
                _uiState.update {
                    it.copy(pathToImage = event.path, outputImage = null, result = "", outputPath = "")
                }
            }

            is PvdMethodEvent.UpdateImage -> {
                _uiState.update {
                    it.copy(image = event.platformFile)
                }
            }

            is PvdMethodEvent.UpdateSelectedColorChannels -> {
                _uiState.update { it.copy(selectedColorChannels = event.channels) }
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

            is PvdMethodEvent.CalculatePvd -> {
                _uiState.update { it.copy(p1 = event.p1, p2 = event.p2, charToEmbed = event.char) }
                viewModelScope.launch(Dispatchers.Default) {
                    val p1Int = event.p1.ifEmpty { "0" }.toIntOrNull()
                    val p2Int = event.p2.ifEmpty { "0" }.toIntOrNull()
                    val char = event.char.firstOrNull()

                    if (p1Int == null || p2Int == null) {
                        _uiState.update { it.copy(pvdCalcResult = "Invalid pixel value.") }
                        return@launch
                    }
                    if (char == null) {
                        _uiState.update { it.copy(pvdCalcResult = "Please enter a character to embed.") }
                        return@launch
                    }
                    if (p1Int !in 0..255 || p2Int !in 0..255) {
                        _uiState.update { it.copy(pvdCalcResult = "Pixel values must be between 0 and 255.") }
                        return@launch
                    }

                    val dummyImage = object : PlatformImage {
                        override val width: Int = 0
                        override val height: Int = 0
                        override fun getPixel(x: Int, y: Int): Int = 0
                        override fun setPixel(x: Int, y: Int, color: Int) {}
                        override fun save(path: String) {}
                        override fun getBytes(): ByteArray {
                            return byteArrayOf()
                        }

                        override fun toImageBitmap(): ImageBitmap {
                            return ImageBitmap(1, 1)
                        }
                    }
                    val processor = PvdProcessor(dummyImage)
                    val result = processor.calculatePvdForPair(p1Int, p2Int, char)
                    _uiState.update { it.copy(pvdCalcResult = result) }
                }
            }

            is PvdMethodEvent.ExtractPvd -> {
                _uiState.update { it.copy(p1 = event.p1, p2 = event.p2) }
                viewModelScope.launch(Dispatchers.Default) {
                    val p1Int = event.p1.ifEmpty { "0" }.toIntOrNull()
                    val p2Int = event.p2.ifEmpty { "0" }.toIntOrNull()

                    if (p1Int == null || p2Int == null) {
                        _uiState.update { it.copy(pvdCalcResult = "Invalid pixel value.") }
                        return@launch
                    }
                    if (p1Int !in 0..255 || p2Int !in 0..255) {
                        _uiState.update { it.copy(pvdCalcResult = "Pixel values must be between 0 and 255.") }
                        return@launch
                    }

                    val dummyImage = object : PlatformImage {
                        override val width: Int = 0
                        override val height: Int = 0
                        override fun getPixel(x: Int, y: Int): Int = 0
                        override fun setPixel(x: Int, y: Int, color: Int) {}
                        override fun save(path: String) {}
                        override fun getBytes(): ByteArray {
                            return byteArrayOf()
                        }

                        override fun toImageBitmap(): ImageBitmap {
                            return ImageBitmap(1, 1)
                        }
                    }
                    val processor = PvdProcessor(dummyImage)
                    val result = processor.extractPvdForPair(p1Int, p2Int)
                    _uiState.update { it.copy(pvdCalcResult = result) }
                }
            }

            is PvdMethodEvent.ProcessImage -> {
                val currentState = _uiState.value
                val imageFile = currentState.image
                val text = currentState.text
                val action = currentState.action
                val outputPath = currentState.outputPath

                if (imageFile != null && (text.isNotEmpty() || action == "Extract")) {
                    viewModelScope.launch {
                        try {
                            _uiState.update { it.copy(result = "Processing...") }
                            val selectedColorChannels = currentState.selectedColorChannels
                                .sorted()
                                .map { ColorChannel.entries[it] }

                            withContext(Dispatchers.Default) {
                                val image = loadPlatformImage(imageFile)
                                val pvdProcessor = PvdProcessor(image)

                                if (action == "Embed") {
                                    pvdProcessor.embedData(text.encodeToByteArray(), selectedColorChannels)
                                    _uiState.update { it.copy(outputImage = pvdProcessor.image, result = "") }
                                } else if (action == "Extract") {
                                    val extractedData = pvdProcessor.extractData(selectedColorChannels)
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
