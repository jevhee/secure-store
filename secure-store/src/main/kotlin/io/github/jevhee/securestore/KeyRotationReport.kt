package io.github.jevhee.securestore

/** Summary returned after activating a new value-encryption key. */
public data class KeyRotationReport(
    /** Key version that was active before rotation. */
    val previousVersion: Int,

    /** Key version activated by the completed rotation. */
    val activeVersion: Int,
)
