package io.github.jevhee.securestore

/** Coroutine-friendly encrypted key-value storage. */
public interface SecureStoreClient {
    /** Encrypts and stores a [String] under the logical [key]. */
    public suspend fun put(key: String, value: String): SecureStoreResult<Unit>

    /** Encrypts and stores an [Int] under the logical [key]. */
    public suspend fun put(key: String, value: Int): SecureStoreResult<Unit>

    /** Encrypts and stores a [Long] under the logical [key]. */
    public suspend fun put(key: String, value: Long): SecureStoreResult<Unit>

    /** Encrypts and stores a [Float] under the logical [key]. */
    public suspend fun put(key: String, value: Float): SecureStoreResult<Unit>

    /** Encrypts and stores a [Double] under the logical [key]. */
    public suspend fun put(key: String, value: Double): SecureStoreResult<Unit>

    /** Encrypts and stores a [Boolean] under the logical [key]. */
    public suspend fun put(key: String, value: Boolean): SecureStoreResult<Unit>

    /**
     * Encrypts and stores binary [value] under the logical [key].
     *
     * The input is copied before ownership can cross a suspension boundary.
     */
    public suspend fun put(key: String, value: ByteArray): SecureStoreResult<Unit>

    /** Reads and decrypts a [String], returning success with `null` when [key] is absent. */
    public suspend fun getString(key: String): SecureStoreResult<String?>

    /** Reads and decrypts an [Int], returning success with `null` when [key] is absent. */
    public suspend fun getInt(key: String): SecureStoreResult<Int?>

    /** Reads and decrypts a [Long], returning success with `null` when [key] is absent. */
    public suspend fun getLong(key: String): SecureStoreResult<Long?>

    /** Reads and decrypts a [Float], returning success with `null` when [key] is absent. */
    public suspend fun getFloat(key: String): SecureStoreResult<Float?>

    /** Reads and decrypts a [Double], returning success with `null` when [key] is absent. */
    public suspend fun getDouble(key: String): SecureStoreResult<Double?>

    /** Reads and decrypts a [Boolean], returning success with `null` when [key] is absent. */
    public suspend fun getBoolean(key: String): SecureStoreResult<Boolean?>

    /**
     * Reads and decrypts binary data, returning success with `null` when [key] is absent.
     *
     * Each successful read returns a new array owned by the caller.
     */
    public suspend fun getBytes(key: String): SecureStoreResult<ByteArray?>

    /** Returns whether an encrypted entry exists for the logical [key]. */
    public suspend fun contains(key: String): SecureStoreResult<Boolean>

    /** Removes the entry associated with [key] without promising secure physical deletion. */
    public suspend fun remove(key: String): SecureStoreResult<Unit>

    /** Removes every user entry in this namespace while retaining internal key metadata. */
    public suspend fun clear(): SecureStoreResult<Unit>

    /** Creates and activates the next versioned value-encryption key. */
    public suspend fun rotateKey(): SecureStoreResult<KeyRotationReport>

    /**
     * Attempts to re-encrypt eligible entries with the active value-encryption key.
     *
     * Obfuscated identifiers cannot be reversed for enumeration and are reported as skipped.
     */
    public suspend fun migrate(): SecureStoreResult<MigrationReport>
}
