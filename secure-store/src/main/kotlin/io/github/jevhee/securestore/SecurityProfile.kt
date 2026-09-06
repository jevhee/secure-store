package io.github.jevhee.securestore

/** A vetted collection of cryptographic settings. */
public sealed interface SecurityProfile {
    /** AES-256-GCM with keys managed by Android Keystore. */
    public data object Default : SecurityProfile
}
