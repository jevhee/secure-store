package io.github.jevhee.securestore.internal.storage

import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.jevhee.securestore.IdentifierProtection
import io.github.jevhee.securestore.KeyRotationReport
import io.github.jevhee.securestore.MigrationPolicy
import io.github.jevhee.securestore.MigrationReport
import io.github.jevhee.securestore.SecureStoreConfig
import io.github.jevhee.securestore.SecureStoreError
import io.github.jevhee.securestore.SecureStoreResult
import io.github.jevhee.securestore.SecureStoreClient
import io.github.jevhee.securestore.internal.codec.Envelope
import io.github.jevhee.securestore.internal.codec.EnvelopeCodec
import io.github.jevhee.securestore.internal.codec.EnvelopeDecodeResult
import io.github.jevhee.securestore.internal.codec.ValueCodec
import io.github.jevhee.securestore.internal.codec.ValueType
import io.github.jevhee.securestore.internal.crypto.AadCodec
import io.github.jevhee.securestore.internal.crypto.AesGcmCryptoEngine
import io.github.jevhee.securestore.internal.crypto.AndroidKeyManager
import io.github.jevhee.securestore.internal.crypto.EncryptedValue
import io.github.jevhee.securestore.internal.crypto.KeyInvalidatedException
import io.github.jevhee.securestore.internal.validation.InputValidator
import java.io.IOException
import java.security.GeneralSecurityException
import javax.crypto.AEADBadTagException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Default namespace-scoped implementation coordinating validation, encryption, and DataStore I/O.
 */
internal class DefaultSecureStoreClient(
    private val packageName: String,
    private val namespace: String,
    private val config: SecureStoreConfig,
    private val dataStore: DataStore<Preferences>,
    private val keyManager: AndroidKeyManager,
    private val cryptoEngine: AesGcmCryptoEngine = AesGcmCryptoEngine(),
) : SecureStoreClient {
    private val mutex = Mutex()

    /** Encrypts and stores a UTF-8 string. */
    override suspend fun put(key: String, value: String): SecureStoreResult<Unit> =
        putEncoded(key, ValueType.StringValue, ValueCodec.encode(value))

    /** Encrypts and stores a fixed-width integer. */
    override suspend fun put(key: String, value: Int): SecureStoreResult<Unit> =
        putEncoded(key, ValueType.IntValue, ValueCodec.encode(value))

    /** Encrypts and stores a fixed-width long. */
    override suspend fun put(key: String, value: Long): SecureStoreResult<Unit> =
        putEncoded(key, ValueType.LongValue, ValueCodec.encode(value))

    /** Encrypts and stores a fixed-width float. */
    override suspend fun put(key: String, value: Float): SecureStoreResult<Unit> =
        putEncoded(key, ValueType.FloatValue, ValueCodec.encode(value))

    /** Encrypts and stores a fixed-width double. */
    override suspend fun put(key: String, value: Double): SecureStoreResult<Unit> =
        putEncoded(key, ValueType.DoubleValue, ValueCodec.encode(value))

    /** Encrypts and stores a canonical Boolean. */
    override suspend fun put(key: String, value: Boolean): SecureStoreResult<Unit> =
        putEncoded(key, ValueType.BooleanValue, ValueCodec.encode(value))

    /** Copies, encrypts, and stores binary input. */
    override suspend fun put(key: String, value: ByteArray): SecureStoreResult<Unit> =
        putEncoded(key, ValueType.BytesValue, ValueCodec.encode(value))

    /** Reads and decodes a string entry. */
    override suspend fun getString(key: String): SecureStoreResult<String?> =
        getDecoded(key, ValueType.StringValue, ValueCodec::decodeString)

    /** Reads and decodes an integer entry. */
    override suspend fun getInt(key: String): SecureStoreResult<Int?> =
        getDecoded(key, ValueType.IntValue, ValueCodec::decodeInt)

    /** Reads and decodes a long entry. */
    override suspend fun getLong(key: String): SecureStoreResult<Long?> =
        getDecoded(key, ValueType.LongValue, ValueCodec::decodeLong)

    /** Reads and decodes a float entry. */
    override suspend fun getFloat(key: String): SecureStoreResult<Float?> =
        getDecoded(key, ValueType.FloatValue, ValueCodec::decodeFloat)

    /** Reads and decodes a double entry. */
    override suspend fun getDouble(key: String): SecureStoreResult<Double?> =
        getDecoded(key, ValueType.DoubleValue, ValueCodec::decodeDouble)

    /** Reads and decodes a Boolean entry. */
    override suspend fun getBoolean(key: String): SecureStoreResult<Boolean?> =
        getDecoded(key, ValueType.BooleanValue, ValueCodec::decodeBoolean)

    /** Reads binary data and returns a caller-owned array. */
    override suspend fun getBytes(key: String): SecureStoreResult<ByteArray?> =
        getDecoded(key, ValueType.BytesValue) { it.copyOf() }

    /** Checks for an entry after applying the configured identifier protection. */
    override suspend fun contains(key: String): SecureStoreResult<Boolean> = runSafely {
        mutex.withLock {
            InputValidator.validateKey(key)
            dataStore.data.first().contains(preferenceKey(key))
        }
    }

    /** Removes one entry while preserving namespace metadata. */
    override suspend fun remove(key: String): SecureStoreResult<Unit> = runSafely {
        mutex.withLock {
            InputValidator.validateKey(key)
            dataStore.edit { preferences -> preferences.remove(preferenceKey(key)) }
        }
    }

    /** Removes all user entries while preserving namespace metadata and keys. */
    override suspend fun clear(): SecureStoreResult<Unit> = runSafely {
        mutex.withLock {
            dataStore.edit { preferences -> preferences.removeEntryValues() }
        }
    }

    /** Generates and activates the next versioned AES value key. */
    override suspend fun rotateKey(): SecureStoreResult<KeyRotationReport> = runSafely {
        mutex.withLock {
            val currentVersion = activeKeyVersion()
            check(currentVersion < Int.MAX_VALUE) { "Key version exhausted." }
            val newVersion = currentVersion + 1
            keyManager.getOrCreateValueKey(newVersion)
            dataStore.edit { preferences -> preferences[ACTIVE_KEY_VERSION] = newVersion }
            KeyRotationReport(previousVersion = currentVersion, activeVersion = newVersion)
        }
    }

    /** Migrates enumerable entries to the active AES value key. */
    override suspend fun migrate(): SecureStoreResult<MigrationReport> = runSafely {
        mutex.withLock {
            val preferences = dataStore.data.first()
            val entryNames = preferences.asMap().keys.map { it.name }.filter(::isEntryName)
            if (config.identifierProtection == IdentifierProtection.Obfuscated) {
                return@withLock MigrationReport(
                    totalEntries = entryNames.size,
                    migratedEntries = 0,
                    skippedEntries = entryNames.size,
                    failedEntries = 0,
                )
            }

            var migrated = 0
            var skipped = 0
            var failed = 0
            for (entryName in entryNames) {
                val logicalKey = entryName.removePrefix(ENTRY_PREFIX)
                when (migrateEntry(logicalKey)) {
                    MigrationOutcome.Migrated -> migrated++
                    MigrationOutcome.Skipped -> skipped++
                    MigrationOutcome.Failed -> failed++
                }
            }
            MigrationReport(entryNames.size, migrated, skipped, failed)
        }
    }

    /**
     * Validates, encrypts, and persists encoded [plaintext], then clears the temporary buffer.
     */
    private suspend fun putEncoded(
        key: String,
        type: ValueType,
        plaintext: ByteArray,
    ): SecureStoreResult<Unit> = runSafely {
        try {
            mutex.withLock {
                InputValidator.validateKey(key)
                InputValidator.validateValue(plaintext)
                putEncodedLocked(key, type, plaintext, activeKeyVersion())
            }
        } finally {
            plaintext.fill(0)
        }
    }

    /** Encrypts and persists [plaintext]; the namespace mutex must already be held. */
    private suspend fun putEncodedLocked(
        key: String,
        type: ValueType,
        plaintext: ByteArray,
        keyVersion: Int,
    ) {
        val encryptionKey = keyManager.getOrCreateValueKey(keyVersion)
        val aad = AadCodec.encode(packageName, namespace, key, type)
        val encrypted = cryptoEngine.encrypt(encryptionKey, plaintext, aad)
        val envelope = EnvelopeCodec.encode(
            Envelope(
                keyVersion = keyVersion,
                type = type,
                nonce = encrypted.nonce,
                ciphertext = encrypted.ciphertext,
            ),
        )
        val storedValue = Base64.encodeToString(envelope, Base64.NO_WRAP)
        dataStore.edit { preferences -> preferences[preferenceKey(key)] = storedValue }
    }

    /**
     * Reads, authenticates, decrypts, type-checks, and decodes one entry.
     *
     * A successfully read outdated entry is lazily rewritten when configured.
     */
    private suspend fun <T> getDecoded(
        key: String,
        expectedType: ValueType,
        decode: (ByteArray) -> T,
    ): SecureStoreResult<T?> = runSafely {
        mutex.withLock {
            InputValidator.validateKey(key)
            val storedValue = dataStore.data.first()[preferenceKey(key)] ?: return@withLock null
            val envelopeBytes = try {
                Base64.decode(storedValue, Base64.NO_WRAP)
            } catch (_: IllegalArgumentException) {
                throw SecureStoreOperationException(SecureStoreError.CorruptedData)
            }
            val envelope = when (val result = EnvelopeCodec.decode(envelopeBytes)) {
                is EnvelopeDecodeResult.Success -> result.envelope
                EnvelopeDecodeResult.Malformed -> throw SecureStoreOperationException(SecureStoreError.CorruptedData)
                EnvelopeDecodeResult.UnsupportedFormat ->
                    throw SecureStoreOperationException(SecureStoreError.UnsupportedFormat)
                EnvelopeDecodeResult.UnsupportedCipherSuite ->
                    throw SecureStoreOperationException(SecureStoreError.UnsupportedCipherSuite)
            }
            if (envelope.type != expectedType) {
                throw SecureStoreOperationException(SecureStoreError.TypeMismatch)
            }
            val encryptionKey = keyManager.getValueKey(envelope.keyVersion)
                ?: throw SecureStoreOperationException(SecureStoreError.KeyNotFound)
            val aad = AadCodec.encode(packageName, namespace, key, expectedType)
            val plaintext = cryptoEngine.decrypt(
                encryptionKey,
                EncryptedValue(envelope.nonce, envelope.ciphertext),
                aad,
            )
            try {
                val decoded = try {
                    decode(plaintext)
                } catch (_: IllegalArgumentException) {
                    throw SecureStoreOperationException(SecureStoreError.CorruptedData)
                }
                if (
                    config.migrationPolicy == MigrationPolicy.Lazy &&
                    envelope.keyVersion != activeKeyVersion()
                ) {
                    putEncodedLocked(key, expectedType, plaintext, activeKeyVersion())
                }
                decoded
            } finally {
                plaintext.fill(0)
            }
        }
    }

    /** Attempts to migrate one plain-identifier entry while preserving data on failure. */
    private suspend fun migrateEntry(logicalKey: String): MigrationOutcome {
        val storedValue = dataStore.data.first()[preferenceKey(logicalKey)]
            ?: return MigrationOutcome.Skipped
        val envelopeBytes = try {
            Base64.decode(storedValue, Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            return MigrationOutcome.Failed
        }
        val envelope = (EnvelopeCodec.decode(envelopeBytes) as? EnvelopeDecodeResult.Success)?.envelope
            ?: return MigrationOutcome.Failed
        val currentVersion = activeKeyVersion()
        if (envelope.keyVersion == currentVersion) return MigrationOutcome.Skipped
        val oldKey = keyManager.getValueKey(envelope.keyVersion) ?: return MigrationOutcome.Failed
        val aad = AadCodec.encode(packageName, namespace, logicalKey, envelope.type)
        val plaintext = try {
            cryptoEngine.decrypt(oldKey, EncryptedValue(envelope.nonce, envelope.ciphertext), aad)
        } catch (_: GeneralSecurityException) {
            return MigrationOutcome.Failed
        }
        return try {
            putEncodedLocked(logicalKey, envelope.type, plaintext, currentVersion)
            MigrationOutcome.Migrated
        } finally {
            plaintext.fill(0)
        }
    }

    /** Returns the persisted active key version, initializing version one when absent. */
    private suspend fun activeKeyVersion(): Int {
        val preferences = dataStore.data.first()
        val storedVersion = preferences[ACTIVE_KEY_VERSION]
        if (storedVersion != null) return storedVersion
        keyManager.getOrCreateValueKey(INITIAL_KEY_VERSION)
        dataStore.edit { mutable ->
            if (mutable[ACTIVE_KEY_VERSION] == null) {
                mutable[ACTIVE_KEY_VERSION] = INITIAL_KEY_VERSION
            }
        }
        return dataStore.data.first()[ACTIVE_KEY_VERSION] ?: INITIAL_KEY_VERSION
    }

    /** Derives the DataStore key according to the configured identifier-protection policy. */
    private fun preferenceKey(logicalKey: String): Preferences.Key<String> {
        val suffix = when (config.identifierProtection) {
            IdentifierProtection.Obfuscated -> {
                val input = identifierInput(namespace, logicalKey)
                Base64.encodeToString(
                    keyManager.obfuscateIdentifier(input),
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
                )
            }
            IdentifierProtection.Plain -> logicalKey
        }
        return stringPreferencesKey(ENTRY_PREFIX + suffix)
    }

    /** Encodes namespace and logical key with length prefixes for identifier HMAC input. */
    private fun identifierInput(namespace: String, logicalKey: String): ByteArray {
        val namespaceBytes = namespace.toByteArray(Charsets.UTF_8)
        val keyBytes = logicalKey.toByteArray(Charsets.UTF_8)
        return byteArrayOf(
            (namespaceBytes.size ushr 8).toByte(),
            namespaceBytes.size.toByte(),
        ) + namespaceBytes + byteArrayOf(
            (keyBytes.size ushr 8).toByte(),
            keyBytes.size.toByte(),
        ) + keyBytes
    }

    /** Removes only keys belonging to user entries from these mutable preferences. */
    private fun MutablePreferences.removeEntryValues() {
        asMap().keys.filter { isEntryName(it.name) }.forEach { key -> remove(key) }
    }

    /** Returns whether [name] belongs to a user entry rather than internal metadata. */
    private fun isEntryName(name: String): Boolean = name.startsWith(ENTRY_PREFIX)

    /** Maps expected internal and platform failures to the stable public result model. */
    private suspend fun <T> runSafely(block: suspend () -> T): SecureStoreResult<T> = try {
        SecureStoreResult.Success(block())
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: SecureStoreOperationException) {
        SecureStoreResult.Failure(exception.error)
    } catch (_: AEADBadTagException) {
        SecureStoreResult.Failure(SecureStoreError.IntegrityCheckFailed)
    } catch (_: KeyInvalidatedException) {
        SecureStoreResult.Failure(SecureStoreError.KeyInvalidated)
    } catch (_: KeyPermanentlyInvalidatedException) {
        SecureStoreResult.Failure(SecureStoreError.KeyInvalidated)
    } catch (_: IOException) {
        SecureStoreResult.Failure(SecureStoreError.StorageUnavailable)
    } catch (exception: IllegalArgumentException) {
        SecureStoreResult.Failure(SecureStoreError.InvalidInput(exception.message ?: "Invalid input."))
    } catch (_: GeneralSecurityException) {
        SecureStoreResult.Failure(SecureStoreError.Unknown)
    } catch (_: IllegalStateException) {
        SecureStoreResult.Failure(SecureStoreError.Unknown)
    }

    private companion object {
        const val ENTRY_PREFIX = "entry."
        const val INITIAL_KEY_VERSION = 1
        val ACTIVE_KEY_VERSION: Preferences.Key<Int> = intPreferencesKey("__securestore_active_value_key_version")
    }
}
