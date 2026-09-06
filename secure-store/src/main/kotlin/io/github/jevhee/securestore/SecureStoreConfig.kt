package io.github.jevhee.securestore

/** Immutable configuration for a [SecureStoreClient]. */
public data class SecureStoreConfig(
    /** Vetted cryptographic profile used for value encryption and authentication. */
    val securityProfile: SecurityProfile = SecurityProfile.Default,

    /** Policy controlling whether logical identifiers are visible in persistent metadata. */
    val identifierProtection: IdentifierProtection = IdentifierProtection.Obfuscated,

    /** Policy controlling when entries encrypted with older key versions are migrated. */
    val migrationPolicy: MigrationPolicy = MigrationPolicy.Lazy,

    /** Policy applied when persisted content cannot be decoded or authenticated safely. */
    val corruptionPolicy: CorruptionPolicy = CorruptionPolicy.ReturnFailure,
)
