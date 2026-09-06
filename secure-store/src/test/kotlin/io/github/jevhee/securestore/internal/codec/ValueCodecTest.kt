package io.github.jevhee.securestore.internal.codec

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ValueCodecTest {
    @Test
    fun `primitive values round trip`() {
        assertEquals("hello", ValueCodec.decodeString(ValueCodec.encode("hello")))
        assertEquals(Int.MIN_VALUE, ValueCodec.decodeInt(ValueCodec.encode(Int.MIN_VALUE)))
        assertEquals(Long.MAX_VALUE, ValueCodec.decodeLong(ValueCodec.encode(Long.MAX_VALUE)))
        assertEquals(1.25f, ValueCodec.decodeFloat(ValueCodec.encode(1.25f)))
        assertEquals(-5.75, ValueCodec.decodeDouble(ValueCodec.encode(-5.75)), 0.0)
        assertEquals(true, ValueCodec.decodeBoolean(ValueCodec.encode(true)))
        assertArrayEquals(byteArrayOf(0, 1, -1), ValueCodec.encode(byteArrayOf(0, 1, -1)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid boolean is rejected`() {
        ValueCodec.decodeBoolean(byteArrayOf(2))
    }
}
