package io.github.jevhee.securestore.internal.crypto

import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AesGcmCryptoEngineTest {
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val engine = AesGcmCryptoEngine()

    @Test fun `encrypt and decrypt round trip`() {
        val plaintext = "classified".toByteArray()
        val aad = "context".toByteArray()

        val encrypted = engine.encrypt(key, plaintext, aad)
        val decrypted = engine.decrypt(key, encrypted, aad)

        assertArrayEquals(plaintext, decrypted)
        assertFalse(plaintext.contentEquals(encrypted.ciphertext))
    }

    @Test(expected = AEADBadTagException::class)
    fun `wrong associated data fails authentication`() {
        val encrypted = engine.encrypt(key, byteArrayOf(1, 2, 3), "right".toByteArray())
        engine.decrypt(key, encrypted, "wrong".toByteArray())
    }

    @Test(expected = AEADBadTagException::class)
    fun `modified ciphertext fails authentication`() {
        val aad = "context".toByteArray()
        val encrypted = engine.encrypt(key, byteArrayOf(1, 2, 3), aad)
        encrypted.ciphertext[0] = (encrypted.ciphertext[0].toInt() xor 1).toByte()
        engine.decrypt(key, encrypted, aad)
    }
}
