package com.bbm.multitask.ui.md5Algo

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64


data class User(val login: String, val passwordHash: String, val salt: String = "")

data class MD5AlgoState(
    val loginInput: String = "",
    val passwordInput: String = "",
    val users: List<User> = emptyList(),
    val authResultMessage: String = ""
)

sealed interface MD5AlgoEvent {
    data class UpdateLogin(val login: String) : MD5AlgoEvent
    data class UpdatePassword(val password: String) : MD5AlgoEvent
    object Register : MD5AlgoEvent
    object Login : MD5AlgoEvent
    object ClearMessage : MD5AlgoEvent
}


class MD5AlgoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MD5AlgoState())
    val uiState: StateFlow<MD5AlgoState> = _uiState.asStateFlow()

    fun onEvent(event: MD5AlgoEvent) {
        when (event) {
            is MD5AlgoEvent.UpdateLogin -> {
                _uiState.update { it.copy(loginInput = event.login) }
            }
            is MD5AlgoEvent.UpdatePassword -> {
                _uiState.update { it.copy(passwordInput = event.password) }
            }
            is MD5AlgoEvent.ClearMessage -> {
                _uiState.update { it.copy(authResultMessage = "") }
            }
            is MD5AlgoEvent.Register -> {
                val currentState = _uiState.value
                if (currentState.loginInput.isBlank() || currentState.passwordInput.isBlank()) {
                    _uiState.update { it.copy(authResultMessage = "Логін та пароль не можуть бути порожніми.") }
                    return
                }

                if (currentState.users.any { it.login == currentState.loginInput }) {
                    _uiState.update { it.copy(authResultMessage = "Помилка: Користувач з таким логіном вже існує.") }
                    return
                }

                val salt = generateSalt()

                val hashedPassword = MD5.hash(currentState.passwordInput + salt)
                val newUser = User(currentState.loginInput, hashedPassword, salt)

                _uiState.update {
                    it.copy(
                        users = it.users + newUser,
                        authResultMessage = "Користувача [${newUser.login}] успішно зареєстровано!",
                        loginInput = "",
                        passwordInput = ""
                    )
                }
            }
            is MD5AlgoEvent.Login -> {
                val currentState = _uiState.value
                if (currentState.loginInput.isBlank() || currentState.passwordInput.isBlank()) {
                    _uiState.update { it.copy(authResultMessage = "Введіть логін та пароль для входу.") }
                    return
                }

                val user = currentState.users.find { it.login == currentState.loginInput }
                if (user == null) {
                    _uiState.update { it.copy(authResultMessage = "Помилка: Користувача не знайдено.") }
                    return
                }

                val hashedInputPassword = MD5.hash(currentState.passwordInput + user.salt)
                if (user.passwordHash == hashedInputPassword) {
                    _uiState.update {
                        it.copy(
                            authResultMessage = "Успішна аутентифікація! Вітаємо, ${user.login}.",
                            passwordInput = ""
                        )
                    }
                } else {
                    _uiState.update { it.copy(authResultMessage = "Помилка: Невірний пароль!") }
                }
            }
        }
    }

    private fun generateSalt(lengthBytes: Int = 16): String {
        val secureRandom = SecureRandom()
        val salt = ByteArray(lengthBytes)
        secureRandom.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }
}

object MD5 {
    private val TABLE_T = intArrayOf(
        0xd76aa478.toInt(), 0xe8c7b756.toInt(), 0x242070db.toInt(), 0xc1bdceee.toInt(),
        0xf57c0faf.toInt(), 0x4787c62a.toInt(), 0xa8304613.toInt(), 0xfd469501.toInt(),
        0x698098d8.toInt(), 0x8b44f7af.toInt(), 0xffff5bb1.toInt(), 0x895cd7be.toInt(),
        0x6b901122.toInt(), 0xfd987193.toInt(), 0xa679438e.toInt(), 0x49b40821.toInt(),
        0xf61e2562.toInt(), 0xc040b340.toInt(), 0x265e5a51.toInt(), 0xe9b6c7aa.toInt(),
        0xd62f105d.toInt(), 0x02441453.toInt(), 0xd8a1e681.toInt(), 0xe7d3fbc8.toInt(),
        0x21e1cde6.toInt(), 0xc33707d6.toInt(), 0xf4d50d87.toInt(), 0x455a14ed.toInt(),
        0xa9e3e905.toInt(), 0xfcefa3f8.toInt(), 0x676f02d9.toInt(), 0x8d2a4c8a.toInt(),
        0xfffa3942.toInt(), 0x8771f681.toInt(), 0x6d9d6122.toInt(), 0xfde5380c.toInt(),
        0xa4beea44.toInt(), 0x4bdecfa9.toInt(), 0xf6bb4b60.toInt(), 0xbebfbc70.toInt(),
        0x289b7ec6.toInt(), 0xeaa127fa.toInt(), 0xd4ef3085.toInt(), 0x04881d05.toInt(),
        0xd9d4d039.toInt(), 0xe6db99e5.toInt(), 0x1fa27cf8.toInt(), 0xc4ac5665.toInt(),
        0xf4292244.toInt(), 0x432aff97.toInt(), 0xab9423a7.toInt(), 0xfc93a039.toInt(),
        0x655b59c3.toInt(), 0x8f0ccc92.toInt(), 0xffeff47d.toInt(), 0x85845dd1.toInt(),
        0x6fa87e4f.toInt(), 0xfe2ce6e0.toInt(), 0xa3014314.toInt(), 0x4e0811a1.toInt(),
        0xf7537e82.toInt(), 0xbd3af235.toInt(), 0x2ad7d2bb.toInt(), 0xeb86d391.toInt()
    )

    private val SHIFT_AMTS = intArrayOf(
        7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
        5,  9, 14, 20, 5,  9, 14, 20, 5,  9, 14, 20, 5,  9, 14, 20,
        4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
        6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21
    )

    fun hash(message: String): String {
        val messageBytes = message.toByteArray(Charsets.UTF_8)
        val originalLenInBytes = messageBytes.size

        val paddingBytesCount = 56 - (originalLenInBytes % 64)
        val paddingLen = if (paddingBytesCount <= 0) paddingBytesCount + 64 else paddingBytesCount

        val totalLen = originalLenInBytes + paddingLen + 8
        val paddedBytes = ByteArray(totalLen)

        System.arraycopy(messageBytes, 0, paddedBytes, 0, originalLenInBytes)

        paddedBytes[originalLenInBytes] = 0x80.toByte()

        val bitLen = originalLenInBytes.toLong() * 8
        for (i in 0 until 8) {
            paddedBytes[totalLen - 8 + i] = ((bitLen ushr (8 * i)) and 0xFF).toByte()
        }

        var a0 = 0x67452301.toInt()
        var b0 = 0xefcdab89.toInt()
        var c0 = 0x98badcfe.toInt()
        var d0 = 0x10325476.toInt()

        for (i in 0 until totalLen step 64) {
            val m = IntArray(16)
            for (j in 0 until 16) {
                val index = i + j * 4
                m[j] = (paddedBytes[index].toInt() and 0xFF) or
                        ((paddedBytes[index + 1].toInt() and 0xFF) shl 8) or
                        ((paddedBytes[index + 2].toInt() and 0xFF) shl 16) or
                        ((paddedBytes[index + 3].toInt() and 0xFF) shl 24)
            }

            var a = a0
            var b = b0
            var c = c0
            var d = d0

            for (j in 0 until 64) {
                var f = 0
                var g = 0

                when (j) {
                    in 0..15 -> {
                        f = (b and c) or (b.inv() and d) // Функція F
                        g = j
                    }
                    in 16..31 -> {
                        f = (d and b) or (d.inv() and c) // Функція G
                        g = (5 * j + 1) % 16
                    }
                    in 32..47 -> {
                        f = b xor c xor d  // Функція H
                        g = (3 * j + 5) % 16
                    }
                    in 48..63 -> {
                        f = c xor (b or d.inv()) // Функція I
                        g = (7 * j) % 16
                    }
                }

                val temp = d
                d = c
                c = b

                val sum = a + f + TABLE_T[j] + m[g]
                b += (sum shl SHIFT_AMTS[j]) or (sum ushr (32 - SHIFT_AMTS[j]))
                a = temp
            }

            a0 += a
            b0 += b
            c0 += c
            d0 += d
        }

        return toHex(a0) + toHex(b0) + toHex(c0) + toHex(d0)
    }

    private fun toHex(value: Int): String {
        return String.format(
            "%02x%02x%02x%02x",
            value and 0xFF,
            (value ushr 8) and 0xFF,
            (value ushr 16) and 0xFF,
            (value ushr 24) and 0xFF
        )
    }
}