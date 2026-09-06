package io.github.jevhee.securestore.internal.crypto

/** Internal signal that Android Keystore permanently invalidated a required key. */
internal class KeyInvalidatedException(cause: Throwable) : Exception(cause)
