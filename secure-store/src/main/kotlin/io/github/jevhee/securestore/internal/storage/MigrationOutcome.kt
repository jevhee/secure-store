package io.github.jevhee.securestore.internal.storage

/** Internal outcome for migrating one encrypted entry. */
internal enum class MigrationOutcome {
    /** Entry was authenticated and rewritten with the active key. */
    Migrated,

    /** Entry was absent or already used the active key. */
    Skipped,

    /** Entry could not be decoded, decrypted, or rewritten safely. */
    Failed,
}
