package io.github.jevhee.securestore

/** Controls behavior when persistent data cannot be decoded safely. */
public enum class CorruptionPolicy {
    /** Returns a structured failure while leaving the unreadable entry unchanged. */
    ReturnFailure,
}
