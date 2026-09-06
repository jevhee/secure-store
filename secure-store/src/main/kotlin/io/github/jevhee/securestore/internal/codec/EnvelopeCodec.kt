package io.github.jevhee.securestore.internal.codec

import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Encodes and defensively decodes the bounded SecureStore binary envelope format. */
internal object EnvelopeCodec {
    /** Maximum supported plaintext size before encryption. */
    const val MAX_VALUE_BYTES: Int = 1024 * 1024
    private val magic =
        byteArrayOf('S'.code.toByte(), 'S'.code.toByte(), 'T'.code.toByte(), '1'.code.toByte())
    private const val FORMAT_VERSION: Byte = 1
    private const val AES_256_GCM: Byte = 1
    private const val HEADER_SIZE = 16
    private const val MAX_CIPHERTEXT_BYTES = MAX_VALUE_BYTES + 16

    /**
     * Serializes a validated [envelope] into the deterministic version-1 binary format.
     *
     * @throws IllegalArgumentException if key version, nonce, or ciphertext size is invalid.
     */
    fun encode(envelope: Envelope): ByteArray {
        require(envelope.keyVersion > 0)
        require(envelope.nonce.size == 12)
        require(envelope.ciphertext.size in 16..MAX_CIPHERTEXT_BYTES)
        return ByteBuffer.allocate(HEADER_SIZE + envelope.nonce.size + envelope.ciphertext.size)
            .order(ByteOrder.BIG_ENDIAN)
            .put(magic)
            .put(FORMAT_VERSION)
            .put(AES_256_GCM)
            .putInt(envelope.keyVersion)
            .put(envelope.type.tag)
            .put(envelope.nonce.size.toByte())
            .putInt(envelope.ciphertext.size)
            .put(envelope.nonce)
            .put(envelope.ciphertext)
            .array()
    }

    /**
     * Parses untrusted persistent [bytes] without allocating beyond documented bounds.
     *
     * @return A structured outcome that distinguishes malformed content from unsupported metadata.
     */
    fun decode(bytes: ByteArray): EnvelopeDecodeResult {
        if (bytes.size < HEADER_SIZE) return EnvelopeDecodeResult.Malformed
        return try {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            val actualMagic = ByteArray(magic.size).also(buffer::get)
            if (!actualMagic.contentEquals(magic)) return EnvelopeDecodeResult.UnsupportedFormat
            if (buffer.get() != FORMAT_VERSION) return EnvelopeDecodeResult.UnsupportedFormat
            if (buffer.get() != AES_256_GCM) return EnvelopeDecodeResult.UnsupportedCipherSuite
            val keyVersion = buffer.int
            val type = ValueType.fromTag(buffer.get()) ?: return EnvelopeDecodeResult.Malformed
            val nonceLength = buffer.get().toInt() and 0xff
            val ciphertextLength = buffer.int
            if (keyVersion <= 0 || nonceLength != 12) return EnvelopeDecodeResult.Malformed
            if (ciphertextLength !in 16..MAX_CIPHERTEXT_BYTES) return EnvelopeDecodeResult.Malformed
            if (buffer.remaining() != nonceLength + ciphertextLength) return EnvelopeDecodeResult.Malformed
            val nonce = ByteArray(nonceLength).also(buffer::get)
            val ciphertext = ByteArray(ciphertextLength).also(buffer::get)
            EnvelopeDecodeResult.Success(Envelope(keyVersion, type, nonce, ciphertext))
        } catch (_: BufferUnderflowException) {
            EnvelopeDecodeResult.Malformed
        } catch (_: IllegalArgumentException) {
            EnvelopeDecodeResult.Malformed
        }
    }
}
