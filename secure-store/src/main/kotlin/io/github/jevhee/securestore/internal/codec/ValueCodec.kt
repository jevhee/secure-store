package io.github.jevhee.securestore.internal.codec

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Converts supported Kotlin values to and from deterministic plaintext byte representations. */
internal object ValueCodec {
    /** Encodes [value] as UTF-8. */
    fun encode(value: String): ByteArray = value.toByteArray(Charsets.UTF_8)

    /** Encodes [value] as a fixed-width big-endian integer. */
    fun encode(value: Int): ByteArray = buffer(Int.SIZE_BYTES).putInt(value).array()

    /** Encodes [value] as a fixed-width big-endian long. */
    fun encode(value: Long): ByteArray = buffer(Long.SIZE_BYTES).putLong(value).array()

    /** Encodes [value] as a fixed-width big-endian IEEE 754 float. */
    fun encode(value: Float): ByteArray = buffer(Float.SIZE_BYTES).putFloat(value).array()

    /** Encodes [value] as a fixed-width big-endian IEEE 754 double. */
    fun encode(value: Double): ByteArray = buffer(Double.SIZE_BYTES).putDouble(value).array()

    /** Encodes [value] as one byte containing zero or one. */
    fun encode(value: Boolean): ByteArray = byteArrayOf(if (value) 1 else 0)

    /** Returns a defensive copy of binary [value]. */
    fun encode(value: ByteArray): ByteArray = value.copyOf()

    /** Decodes UTF-8 [bytes] into a string. */
    fun decodeString(bytes: ByteArray): String = bytes.toString(Charsets.UTF_8)

    /** Decodes exactly four big-endian [bytes] into an integer. */
    fun decodeInt(bytes: ByteArray): Int = exactBuffer(bytes, Int.SIZE_BYTES).int

    /** Decodes exactly eight big-endian [bytes] into a long. */
    fun decodeLong(bytes: ByteArray): Long = exactBuffer(bytes, Long.SIZE_BYTES).long

    /** Decodes exactly four big-endian [bytes] into a float. */
    fun decodeFloat(bytes: ByteArray): Float = exactBuffer(bytes, Float.SIZE_BYTES).float

    /** Decodes exactly eight big-endian [bytes] into a double. */
    fun decodeDouble(bytes: ByteArray): Double = exactBuffer(bytes, Double.SIZE_BYTES).double

    /** Decodes a canonical one-byte Boolean representation. */
    fun decodeBoolean(bytes: ByteArray): Boolean {
        require(bytes.size == 1 && (bytes[0] == 0.toByte() || bytes[0] == 1.toByte()))
        return bytes[0] == 1.toByte()
    }

    /** Allocates a big-endian buffer with the requested [size]. */
    private fun buffer(size: Int): ByteBuffer =
        ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN)

    /** Wraps [bytes] as big-endian after enforcing the exact expected [size]. */
    private fun exactBuffer(bytes: ByteArray, size: Int): ByteBuffer {
        require(bytes.size == size)
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
    }
}
