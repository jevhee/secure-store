package io.github.jevhee.securestore.internal.codec

/** Exhaustive result of decoding untrusted persistent envelope bytes. */
internal sealed interface EnvelopeDecodeResult {
    /** Contains a fully validated structural envelope. */
    data class Success(val envelope: Envelope) : EnvelopeDecodeResult

    /** Indicates invalid lengths, fields, type tags, or truncated/trailing bytes. */
    data object Malformed : EnvelopeDecodeResult

    /** Indicates an unknown magic marker or envelope format version. */
    data object UnsupportedFormat : EnvelopeDecodeResult

    /** Indicates a recognized envelope whose cipher suite is not supported. */
    data object UnsupportedCipherSuite : EnvelopeDecodeResult
}
