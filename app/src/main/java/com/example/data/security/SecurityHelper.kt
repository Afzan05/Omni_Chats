package com.example.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

object SecurityHelper {
    private val secureRandom = SecureRandom()

    /**
     * Generates a cryptographically strong 16-byte random salt.
     */
    fun generateSalt(): String {
        val saltBytes = ByteArray(16)
        secureRandom.nextBytes(saltBytes)
        return Base64.encodeToString(saltBytes, Base64.NO_WRAP)
    }

    /**
     * Generates a 6-digit numeric OTP.
     */
    fun generateOtp(): String {
        val number = secureRandom.nextInt(900000) + 100000
        return number.toString()
    }

    /**
     * Generates a secure session token.
     */
    fun generateSessionToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    /**
     * Hashes input (password, PIN, or OTP) with salt using SHA-256.
     * Prevents plain-text credential leaks.
     */
    fun hashWithSalt(input: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(Base64.decode(salt, Base64.NO_WRAP))
        val hashed = md.digest(input.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashed, Base64.NO_WRAP)
    }

    /**
     * Constant-time comparison to protect against timing attacks.
     */
    fun verifyHash(input: String, salt: String, expectedHash: String): Boolean {
        val computed = hashWithSalt(input, salt)
        return MessageDigest.isEqual(
            computed.toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
    }
}
