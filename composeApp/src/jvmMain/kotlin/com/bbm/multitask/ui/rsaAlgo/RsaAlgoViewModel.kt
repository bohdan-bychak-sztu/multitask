package com.bbm.multitask.ui.rsaAlgo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.security.SecureRandom
import java.util.Base64


data class RsaAlgoState(
    val message: String = "",
    val encryptedMessage: String = "",
    val decryptedMessage: String = "",
    val keyPair: RsaKeyPair? = null,
)

sealed interface RsaAlgoEvent {
    data class UpdateMessage(val message: String) : RsaAlgoEvent
    object GenerateKeyPair : RsaAlgoEvent
    object EncryptMessage : RsaAlgoEvent
    object DecryptMessage : RsaAlgoEvent
}

class RsaAlgoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RsaAlgoState())
    val uiState: StateFlow<RsaAlgoState> = _uiState.asStateFlow()

    fun onEvent(event: RsaAlgoEvent) {
        when (event) {
            is RsaAlgoEvent.UpdateMessage -> {
                _uiState.update { it.copy(message = event.message) }
            }
            is RsaAlgoEvent.GenerateKeyPair -> {
                viewModelScope.launch(Dispatchers.Default) {
                    val keyPair = RSA.generateKeyPair()
                    _uiState.update { it.copy(keyPair = keyPair) }
                }
            }
            is RsaAlgoEvent.EncryptMessage -> {
                val keyPair = _uiState.value.keyPair ?: return

                viewModelScope.launch(Dispatchers.Default) {
                    try {
                        val encrypted = RSA.encrypt(_uiState.value.message, keyPair.publicKey)
                        _uiState.update { it.copy(encryptedMessage = encrypted) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _uiState.update { it.copy(encryptedMessage = "Помилка шифрування") }
                    }
                }
            }
            is RsaAlgoEvent.DecryptMessage -> {
                val keyPair = _uiState.value.keyPair ?: return

                viewModelScope.launch(Dispatchers.Default) {
                    try {
                        val decrypted = RSA.decrypt(_uiState.value.message, keyPair.privateKey)
                        _uiState.update { it.copy(decryptedMessage = decrypted) }
                    } catch (e: IllegalArgumentException) {
                        e.printStackTrace()
                        _uiState.update { it.copy(decryptedMessage = "Помилка: введено некоректний зашифрований текст (не Base64).") }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _uiState.update { it.copy(decryptedMessage = "Помилка розшифрування: дані пошкоджено або використано не той ключ.") }
                    }
                }
            }
        }

    }
}

data class RsaPublicKey(val e: BigInteger, val n: BigInteger)
data class RsaPrivateKey(val d: BigInteger, val n: BigInteger)
data class RsaKeyPair(val publicKey: RsaPublicKey, val privateKey: RsaPrivateKey)

object RSA {
    fun generateKeyPair(bitLength: Int = 2048): RsaKeyPair {
        val random = SecureRandom()

        val p = BigInteger.probablePrime(bitLength / 2, random)
        val q = BigInteger.probablePrime(bitLength / 2, random)

        val n = p.multiply(q)

        val phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE))

        var e: BigInteger
        do {
            e = BigInteger(phi.bitLength(), random)
        } while (e <= BigInteger.ONE || e >= phi || e.gcd(phi) != BigInteger.ONE)

        val d = e.modInverse(phi)

        return RsaKeyPair(
            publicKey = RsaPublicKey(e, n),
            privateKey = RsaPrivateKey(d, n)
        )
    }

    fun encrypt(message: String, publicKey: RsaPublicKey): String {

        return Base64.getEncoder().encodeToString(encrypt(message.toByteArray(Charsets.UTF_8), publicKey))
    }

    fun encrypt(message: ByteArray, publicKey: RsaPublicKey): ByteArray {
        val m = BigInteger(1, message)

        require(m < publicKey.n) { "Повідомлення занадто велике для цього розміру ключа." }

        val c = m.modPow(publicKey.e, publicKey.n)

        return c.toByteArray()
    }

    fun decrypt(cipherText: String, privateKey: RsaPrivateKey): String {
        return String(decrypt(Base64.getDecoder().decode(cipherText), privateKey), Charsets.UTF_8)
    }

    fun decrypt(cipherText: ByteArray, privateKey: RsaPrivateKey): ByteArray {
        val c = BigInteger(1, cipherText)

        val m = c.modPow(privateKey.d, privateKey.n)

        var decryptedBytes = m.toByteArray()
        if (decryptedBytes.isNotEmpty() && decryptedBytes[0] == 0.toByte()) {
            decryptedBytes = decryptedBytes.copyOfRange(1, decryptedBytes.size)
        }

        return decryptedBytes
    }
}
