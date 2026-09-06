package io.github.jevhee.securestore.internal.storage

/** Uniquely identifies a SecureStore namespace within an Android application process. */
internal data class RegistryKey(
    /** Host application's package name. */
    val packageName: String,

    /** Validated and normalized logical namespace. */
    val namespace: String,
)
