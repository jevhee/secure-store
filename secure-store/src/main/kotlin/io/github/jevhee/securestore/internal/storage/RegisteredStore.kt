package io.github.jevhee.securestore.internal.storage

import io.github.jevhee.securestore.SecureStoreConfig
import io.github.jevhee.securestore.SecureStoreClient

/** Process registry value pairing a namespace configuration with its shared client. */
internal data class RegisteredStore(
    /** Configuration that must match on every subsequent open request. */
    val config: SecureStoreConfig,

    /** Long-lived process-local client for the namespace. */
    val store: SecureStoreClient,
)
