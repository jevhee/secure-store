package io.github.jevhee.securestore.internal.crypto

import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Performs authenticated AES-256-GCM encryption with a fresh nonce for every write. */
internal class AesGcmCryptoEngine(
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    /**
     * Encrypts [plaintext] and authenticates [aad] with [key].
     *
     * @return Ciphertext with its newly generated nonce.
     */
    fun encrypt(key: SecretKey, plaintext: ByteArray, aad: ByteArray): EncryptedValue {
        val nonce = ByteArray(NONCE_BYTES).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad)
        return EncryptedValue(nonce, cipher.doFinal(plaintext))
    }

    /**
     * Authenticates and decrypts [encrypted] using the exact [aad] supplied during encryption.
     *
     * @throws AEADBadTagException if ciphertext, nonce, key, or associated data does not match.
     */
    @Throws(AEADBadTagException::class)
    fun decrypt(key: SecretKey, encrypted: EncryptedValue, aad: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, encrypted.nonce))
        cipher.updateAAD(aad)
        return cipher.doFinal(encrypted.ciphertext)
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val NONCE_BYTES = 12
        const val TAG_BITS = 128
    }
}
