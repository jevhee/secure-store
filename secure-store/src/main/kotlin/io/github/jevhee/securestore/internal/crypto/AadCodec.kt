package io.github.jevhee.securestore.internal.crypto

import io.github.jevhee.securestore.internal.codec.ValueType
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/** Builds deterministic, length-prefixed associated data for AES-GCM operations. */
internal object AadCodec {
    /**
     * Binds ciphertext to the application, namespace, logical key, and expected value type.
     *
     * @return Versioned associated data that is authenticated but not encrypted.
     */
    fun encode(packageName: String, namespace: String, logicalKey: String, type: ValueType): ByteArray {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { data ->
            data.writeField("securestore".toByteArray(Charsets.UTF_8))
            data.writeByte(1)
            data.writeField(packageName.toByteArray(Charsets.UTF_8))
            data.writeField(namespace.toByteArray(Charsets.UTF_8))
            data.writeField(logicalKey.toByteArray(Charsets.UTF_8))
            data.writeByte(type.tag.toInt())
        }
        return output.toByteArray()
    }

    /** Writes a byte field with an unambiguous four-byte length prefix. */
    private fun DataOutputStream.writeField(value: ByteArray) {
        writeInt(value.size)
        write(value)
    }
}
