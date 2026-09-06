package io.github.jevhee.securestore.internal.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * Creates and resolves non-exportable AES and HMAC keys scoped to one application namespace.
 */
internal class AndroidKeyManager(
    packageName: String,
    namespace: String,
) {
    private val aliasPrefix = "securestore.${shortHash(packageName)}.${shortHash(namespace)}"
    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    /** Returns the AES key for [version], creating it atomically when absent. */
    @Synchronized
    fun getOrCreateValueKey(version: Int): SecretKey {
        require(version > 0)
        val alias = "$aliasPrefix.value.v$version"
        return getSecretKey(alias) ?: generateAesKey(alias)
    }

    /** Returns the existing AES key for [version], or `null` without recreating a missing key. */
    fun getValueKey(version: Int): SecretKey? {
        require(version > 0)
        return getSecretKey("$aliasPrefix.value.v$version")
    }

    /** Returns the namespace HMAC identifier key, creating it atomically when absent. */
    @Synchronized
    fun getOrCreateIdentifierKey(): SecretKey {
        val alias = "$aliasPrefix.identifier.v1"
        return getSecretKey(alias) ?: generateHmacKey(alias)
    }

    /** Computes the deterministic HMAC-SHA-256 storage identifier for [input]. */
    fun obfuscateIdentifier(input: ByteArray): ByteArray {
        val mac = Mac.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256)
        mac.init(getOrCreateIdentifierKey())
        return mac.doFinal(input)
    }

    /** Loads a key by [alias] while translating permanent invalidation to an internal signal. */
    private fun getSecretKey(alias: String): SecretKey? = try {
        keyStore.getKey(alias, null) as? SecretKey
    } catch (exception: KeyPermanentlyInvalidatedException) {
        throw KeyInvalidatedException(exception)
    }

    /** Generates a non-exportable 256-bit AES-GCM key under [alias]. */
    private fun generateAesKey(alias: String): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    /** Generates a non-exportable 256-bit HMAC-SHA-256 key under [alias]. */
    private fun generateHmacKey(alias: String): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"

        /** Produces a compact deterministic hash used in aliases without exposing raw names. */
        fun shortHash(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .take(12)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
