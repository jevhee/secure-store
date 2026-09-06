package io.github.jevhee.securestore

/** Summary returned after explicitly migrating stored entries. */
public data class MigrationReport(
    /** Number of stored entries considered for migration. */
    val totalEntries: Int,

    /** Number of entries successfully re-encrypted with the active key. */
    val migratedEntries: Int,

    /** Number of entries that did not require or could not support migration. */
    val skippedEntries: Int,

    /** Number of entries that could not be authenticated, decoded, or rewritten. */
    val failedEntries: Int,
)
