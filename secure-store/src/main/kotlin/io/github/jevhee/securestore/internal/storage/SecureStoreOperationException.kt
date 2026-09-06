package io.github.jevhee.securestore.internal.storage

import io.github.jevhee.securestore.SecureStoreError

/** Carries a sanitized domain [error] through internal control flow. */
internal class SecureStoreOperationException(val error: SecureStoreError) : Exception()
