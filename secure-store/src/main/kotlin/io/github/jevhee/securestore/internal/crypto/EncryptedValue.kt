package io.github.jevhee.securestore.internal.crypto

/** AES-GCM output consisting of a public nonce and authenticated ciphertext. */
internal data class EncryptedValue(
    /** Fresh nonce generated for this encryption operation. */
    val nonce: ByteArray,

    /** Encrypted bytes including the authentication tag. */
    val ciphertext: ByteArray,
)
