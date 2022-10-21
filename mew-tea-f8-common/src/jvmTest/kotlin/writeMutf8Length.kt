package me.nullicorn.mewteaf8

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class WriteMutf8LengthTests {

    private val mutf8Lengths: Iterable<UShort> =
        (UShort.MIN_VALUE..UShort.MAX_VALUE step 17).map { it.toUShort() } + UShort.MAX_VALUE

    @Test
    fun `writeMutf8Length should throw an IOException if the OutputStream does`() {
        for (mutf8Length in mutf8Lengths) {
            val stream = object : OutputStream() {
                override fun write(b: Int) {
                    throw IOException()
                }
            }

            assertFailsWith<IOException> {
                stream.writeMutf8Length(mutf8Length)
            }
        }
    }

    @Test
    fun `writeMutf8Length should encode the mutf8Length using 2 bytes in big-endian order`() {
        for (mutf8Length in mutf8Lengths) {
            val encoded = ByteArrayOutputStream(/* size = */ 2)
            encoded.writeMutf8Length(mutf8Length)

            assertContentEquals(
                actual = encoded.toByteArray(),
                expected = byteArrayOf(
                    (mutf8Length.toInt() shr 8 and 0xFF).toByte(),
                    (mutf8Length.toInt() /* */ and 0xFF).toByte()
                )
            )
        }
    }
}