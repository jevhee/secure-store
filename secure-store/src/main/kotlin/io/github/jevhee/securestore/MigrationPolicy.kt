package io.github.jevhee.securestore

/** Controls when entries written with an older key version are migrated. */
public enum class MigrationPolicy {
    /** Re-encrypts an outdated entry after it has been read and authenticated successfully. */
    Lazy,

    /** Migrates entries only when [SecureStoreClient.migrate] is invoked explicitly. */
    Manual,
}
