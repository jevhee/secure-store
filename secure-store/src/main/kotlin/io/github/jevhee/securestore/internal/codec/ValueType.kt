package io.github.jevhee.securestore.internal.codec

/** Stable plaintext type identifiers persisted in encrypted envelopes. */
internal enum class ValueType(val tag: Byte) {
    StringValue(1),
    IntValue(2),
    LongValue(3),
    FloatValue(4),
    DoubleValue(5),
    BooleanValue(6),
    BytesValue(7),
    ;

    companion object {
        /** Resolves a persisted [tag], or returns `null` when the tag is unknown. */
        fun fromTag(tag: Byte): ValueType? = entries.firstOrNull { it.tag == tag }
    }
}
