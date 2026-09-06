package io.github.jevhee.securestore

/** Stable error categories exposed by SecureStore. */
public sealed interface SecureStoreError {
    /** The cryptographic operation requires user authentication before it can continue. */
    public data object AuthenticationRequired : SecureStoreError

    /** Android Keystore permanently invalidated a key required by the entry. */
    public data object KeyInvalidated : SecureStoreError

    /** The encrypted entry references a key version that is no longer available. */
    public data object KeyNotFound : SecureStoreError

    /** Persisted bytes are malformed or cannot be decoded safely. */
    public data object CorruptedData : SecureStoreError

    /** AES-GCM authentication failed because ciphertext, nonce, or associated data differs. */
    public data object IntegrityCheckFailed : SecureStoreError

    /** The entry uses an envelope format version unsupported by this library version. */
    public data object UnsupportedFormat : SecureStoreError

    /** The entry requests a cipher suite unsupported by this library version. */
    public data object UnsupportedCipherSuite : SecureStoreError

    /** The requested getter does not match the type recorded in the encrypted envelope. */
    public data object TypeMismatch : SecureStoreError

    /** The underlying persistent storage could not be read or updated. */
    public data object StorageUnavailable : SecureStoreError

    /** One or more entries could not complete an explicitly requested migration. */
    public data object MigrationFailed : SecureStoreError

    /**
     * A caller-supplied namespace, logical key, or value violates an input constraint.
     *
     * @property reason Sanitized explanation that does not contain the rejected value.
     */
    public data class InvalidInput(val reason: String) : SecureStoreError

    /** An unexpected internal or platform failure that has no more specific public category. */
    public data object Unknown : SecureStoreError
}
