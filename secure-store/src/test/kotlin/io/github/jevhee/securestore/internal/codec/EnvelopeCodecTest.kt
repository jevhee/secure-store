package io.github.jevhee.securestore.internal.codec

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnvelopeCodecTest {
    @Test fun `envelope round trips`() {
        val original = Envelope(
            keyVersion = 7,
            type = ValueType.BytesValue,
            nonce = ByteArray(12) { it.toByte() },
            ciphertext = ByteArray(32) { (it * 3).toByte() },
        )

        val decoded = EnvelopeCodec.decode(EnvelopeCodec.encode(original)) as EnvelopeDecodeResult.Success

        assertEquals(original.keyVersion, decoded.envelope.keyVersion)
        assertEquals(original.type, decoded.envelope.type)
        assertArrayEquals(original.nonce, decoded.envelope.nonce)
        assertArrayEquals(original.ciphertext, decoded.envelope.ciphertext)
    }

    @Test fun `encoded envelope uses SecureStore magic`() {
        val encoded = EnvelopeCodec.encode(
            Envelope(1, ValueType.StringValue, ByteArray(12), ByteArray(16)),
        )

        assertArrayEquals("SST1".toByteArray(Charsets.US_ASCII), encoded.copyOfRange(0, 4))
    }

    @Test fun `truncated envelope is rejected`() {
        val encoded = EnvelopeCodec.encode(
            Envelope(1, ValueType.StringValue, ByteArray(12), ByteArray(16)),
        )

        assertTrue(EnvelopeCodec.decode(encoded.copyOf(encoded.size - 1)) is EnvelopeDecodeResult.Malformed)
    }

    @Test fun `trailing bytes are rejected`() {
        val encoded = EnvelopeCodec.encode(
            Envelope(1, ValueType.StringValue, ByteArray(12), ByteArray(16)),
        )

        assertTrue(EnvelopeCodec.decode(encoded + 0) is EnvelopeDecodeResult.Malformed)
    }

    @Test fun `unknown format is rejected distinctly`() {
        val encoded = EnvelopeCodec.encode(
            Envelope(1, ValueType.StringValue, ByteArray(12), ByteArray(16)),
        )
        encoded[4] = 99

        assertTrue(EnvelopeCodec.decode(encoded) is EnvelopeDecodeResult.UnsupportedFormat)
    }
}
