package io.github.jevhee.securestore

/** Controls whether logical keys remain visible in persistent storage. */
public enum class IdentifierProtection {
    /** Replaces logical keys with a deterministic HMAC-SHA-256 value. */
    Obfuscated,

    /** Stores logical keys as metadata. This can leak sensitive identifiers. */
    Plain,
}
