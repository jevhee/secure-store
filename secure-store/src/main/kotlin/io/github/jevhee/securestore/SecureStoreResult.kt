package io.github.jevhee.securestore

/** Result of a SecureStore operation. */
public sealed interface SecureStoreResult<out T> {
    /**
     * A successful operation.
     *
     * @property value Produced value. For nullable read operations, `null` means no entry exists.
     */
    public data class Success<T>(val value: T) : SecureStoreResult<T>

    /**
     * An operation that failed without exposing sensitive implementation details.
     *
     * @property error Stable category describing the failure.
     */
    public data class Failure(val error: SecureStoreError) : SecureStoreResult<Nothing>
}
