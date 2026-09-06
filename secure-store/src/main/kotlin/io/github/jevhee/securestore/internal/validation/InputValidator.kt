package io.github.jevhee.securestore.internal.validation

import io.github.jevhee.securestore.internal.codec.EnvelopeCodec

/** Centralizes bounded validation for all caller-controlled names and values. */
internal object InputValidator {
    private val namespacePattern = Regex("[A-Za-z0-9._-]{1,64}")

    /** Trims and validates [namespace], returning its canonical representation. */
    fun validateNamespace(namespace: String): String {
        val normalized = namespace.trim()
        require(namespacePattern.matches(normalized)) {
            "Namespace must contain 1-64 characters from A-Z, a-z, 0-9, dot, underscore, or hyphen."
        }
        return normalized
    }

    /** Validates a logical [key] without altering its identity. */
    fun validateKey(key: String) {
        require(key.isNotBlank()) { "Key must not be blank." }
        require(key.toByteArray(Charsets.UTF_8).size <= 256) { "Key must not exceed 256 UTF-8 bytes." }
    }

    /** Enforces the maximum supported plaintext [value] size. */
    fun validateValue(value: ByteArray) {
        require(value.size <= EnvelopeCodec.MAX_VALUE_BYTES) { "Value must not exceed 1 MiB." }
    }
}
