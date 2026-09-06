package io.github.jevhee.securestore

import android.content.Context
import io.github.jevhee.securestore.SecureStore.open
import io.github.jevhee.securestore.internal.storage.SecureStoreFactory

/** Entry point for opening encrypted key-value stores. */
public object SecureStore {
    /** Namespace used when [open] is called without an explicit namespace. */
    public const val DEFAULT_NAMESPACE: String = "default"

    /**
     * Opens a process-local store. Instances sharing a namespace also share storage coordination.
     * Multi-process access is not supported.
     *
     * @param context Android context used to access DataStore and Android Keystore. Only its
     * application context is retained.
     * @param namespace Stable, isolated storage namespace containing 1–64 supported characters.
     * @param config Immutable security and migration configuration for this namespace.
     * @return A coroutine-friendly client for the selected namespace.
     * @throws IllegalArgumentException if [namespace] is invalid or the namespace was already
     * opened in this process with a different [config].
     */
    @JvmStatic
    @JvmOverloads
    public fun open(
        context: Context,
        namespace: String = DEFAULT_NAMESPACE,
        config: SecureStoreConfig = SecureStoreConfig(),
    ): SecureStoreClient = SecureStoreFactory.open(
        context = context.applicationContext,
        namespace = namespace,
        config = config,
    )
}
