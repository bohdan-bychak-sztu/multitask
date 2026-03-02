package com.bbm.multitask.ui.l3ib

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


data class L3ibMethodState(
    val language: String = "Ukrainian",
    val method: String = "Gamma cipher",
    val formula: String = "S = Г + О",
    val keyWord: String = "Ключ",
    val action: String = "Encrypt",
    val text: String = "",
    val result: String = "",
)

sealed interface L3ibMethodEvent {
    data class UpdateLanguage(val language: String) : L3ibMethodEvent
    data class UpdateMethod(val method: String) : L3ibMethodEvent
    data class UpdateKeyWord(val keyWord: String) : L3ibMethodEvent
    data class UpdateText(val text: String) : L3ibMethodEvent
    data class UpdateAction(val action: String) : L3ibMethodEvent
    data class UpdateFormula(val formula: String) : L3ibMethodEvent
}

class L3ibViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(L3ibMethodState())
    val uiState: StateFlow<L3ibMethodState> = _uiState.asStateFlow()

    fun onEvent(event: L3ibMethodEvent) {
        when (event) {
            is L3ibMethodEvent.UpdateLanguage -> {
                _uiState.update { it.copy(language = event.language) }
            }
            is L3ibMethodEvent.UpdateMethod -> {
                _uiState.update { it.copy(method = event.method) }
            }
            is L3ibMethodEvent.UpdateAction -> {
                _uiState.update { it.copy(action = event.action) }
            }
            is L3ibMethodEvent.UpdateKeyWord -> {
                _uiState.update { it.copy(keyWord = event.keyWord) }
            }
            is L3ibMethodEvent.UpdateText -> {
                _uiState.update { it.copy(text = event.text) }
            }
            is L3ibMethodEvent.UpdateFormula -> {
                _uiState.update { it.copy(formula = event.formula) }
            }


            else -> {}
        }
        processCipher()

    }

    private fun processCipher() {
        val currentState = _uiState.value
        if (currentState.text.isEmpty() || currentState.keyWord.isEmpty()) {
            _uiState.update { it.copy(result = "") }
            return
        }

        val result = when (currentState.method) {
            "Gamma cipher" -> performGammaArithmetic(
                text = currentState.text,
                key = currentState.keyWord,
                language = currentState.language,
                formula = currentState.formula,
                isEncrypt = currentState.action == "Encrypt"
            )
            else -> "Coming soon..."
        }

        _uiState.update { it.copy(result = result) }
    }
}

private object AlphabetUtils {
    const val UKRAINIAN = "абвгґдеєжзиіїйклмнопрстуфхцчшщьюя"
    const val ENGLISH = "abcdefghijklmnopqrstuvwxyz"

    fun getAlphabet(language: String) = if (language == "Ukrainian") UKRAINIAN else ENGLISH
}

private fun performGammaArithmetic(
    text: String,
    key: String,
    language: String,
    formula: String, // "S = Г + О", "S = Г - О", "S = О - Г"
    isEncrypt: Boolean
): String {
    val alphabet = AlphabetUtils.getAlphabet(language)
    val n = alphabet.length
    val cleanKey = key.lowercase()

    return text.mapIndexed { index, char ->
        val isUpper = char.isUpperCase()
        val lowerChar = char.lowercaseChar()
        val charIdx = alphabet.indexOf(lowerChar)

        if (charIdx == -1) return@mapIndexed char

        val gammaIdx = alphabet.indexOf(cleanKey[index % cleanKey.length])
        if (gammaIdx == -1) return@mapIndexed char

        val resultIdx = when (formula) {
            "S = Г + О" -> if (isEncrypt) (gammaIdx + charIdx) % n else (charIdx - gammaIdx + n) % n
            "S = Г - О" -> if (isEncrypt) (gammaIdx - charIdx + n) % n else (gammaIdx - charIdx + n) % n
            "S = О - Г" -> if (isEncrypt) (charIdx - gammaIdx + n) % n else (charIdx + gammaIdx) % n
            else -> charIdx
        }

        val resultChar = alphabet[resultIdx]
        if (isUpper) resultChar.uppercaseChar() else resultChar
    }.joinToString("")
}