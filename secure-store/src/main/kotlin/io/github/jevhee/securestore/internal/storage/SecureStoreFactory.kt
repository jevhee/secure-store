package io.github.jevhee.securestore.internal.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.jevhee.securestore.SecureStoreClient
import io.github.jevhee.securestore.SecureStoreConfig
import io.github.jevhee.securestore.internal.crypto.AndroidKeyManager
import io.github.jevhee.securestore.internal.validation.InputValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.security.MessageDigest

/** Maintains one coordinated DataStore-backed client per application namespace and process. */
internal object SecureStoreFactory {
    /**
     * Process-lifetime scope required by DataStore's single-instance-per-file contract.
     *
     * It is intentionally isolated from UI lifecycles.
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Registry preventing multiple DataStore instances from opening the same backing file. */
    private val stores = mutableMapOf<RegistryKey, RegisteredStore>()

    /**
     * Returns the shared client for [namespace], creating its DataStore and key manager if needed.
     *
     * @throws IllegalArgumentException for an invalid namespace or conflicting configuration.
     */
    @Synchronized
    fun open(context: Context, namespace: String, config: SecureStoreConfig): SecureStoreClient {
        val normalizedNamespace = InputValidator.validateNamespace(namespace)
        val registryKey = RegistryKey(context.packageName, normalizedNamespace)
        val existing = stores[registryKey]
        if (existing != null) {
            require(existing.config == config) {
                "A SecureStore namespace cannot be opened with conflicting configurations."
            }
            return existing.store
        }

        val dataStore = createDataStore(context, normalizedNamespace)
        val registered = RegisteredStore(
            config = config,
            store = DefaultSecureStoreClient(
                packageName = context.packageName,
                namespace = normalizedNamespace,
                config = config,
                dataStore = dataStore,
                keyManager = AndroidKeyManager(context.packageName, normalizedNamespace),
            ),
        )
        stores[registryKey] = registered
        return registered.store
    }

    /** Creates the single DataStore instance for a normalized [namespace]. */
    private fun createDataStore(context: Context, namespace: String): DataStore<Preferences> {
        val namespaceHash = MessageDigest.getInstance("SHA-256")
            .digest(namespace.toByteArray(Charsets.UTF_8))
            .take(12)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
        return PreferenceDataStoreFactory.create(
            scope = applicationScope,
            produceFile = { context.preferencesDataStoreFile("securestore_$namespaceHash") },
        )
    }
}
