package io.github.jevhee.securestore.internal.codec

/** Parsed representation of one authenticated, versioned encrypted value. */
internal data class Envelope(
    /** Android Keystore key version needed to decrypt [ciphertext]. */
    val keyVersion: Int,

    /** Stable type tag describing the encoded plaintext. */
    val type: ValueType,

    /** Unique 96-bit AES-GCM nonce stored alongside the ciphertext. */
    val nonce: ByteArray,

    /** Ciphertext including the AES-GCM authentication tag. */
    val ciphertext: ByteArray,
)
