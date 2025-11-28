package com.aark.sfuscavenger.crypto

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import com.aark.sfuscavenger.BuildConfig

/**
 * Helper to encrypt/decrypt chat messages before they go to Firestore
 * - Uses AES/GCM/NoPadding with a static key derived from a hidden key
 */
object ChatCrypto {
    private val keySpec: SecretKeySpec by lazy {
        val passphrase = BuildConfig.CHAT_SECRET
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(passphrase.toByteArray(Charsets.UTF_8))
        SecretKeySpec(keyBytes, "AES")
    }

    // Used to generate a fresh IV for each encryption call
    private val secureRandom = SecureRandom()

    /**
     * Encrypt plain text into a Base64 string: base64(IV(12 bytes) || ciphertext)
     */
    fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty()) return plaintext

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        val iv = ByteArray(12)
        secureRandom.nextBytes(iv)

        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec)

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size,
            ciphertext.size)

        // Storing as a Base64 string
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypt a Base64 string produced by encrypt
     */
    fun decrypt(encoded: String): String {
        if (encoded.isEmpty()) return encoded

        return try {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)

            // If it doesn't look like our format, just treat as plaintext
            if (combined.size < 13) {
                return encoded
            }

            val iv = combined.copyOfRange(0, 12)
            val ciphertext = combined.copyOfRange(12, combined.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec)

            val plaintext = cipher.doFinal(ciphertext)
            String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            // Fallback for old/plaintext messages or decode issues
            encoded
        }
    }
}
