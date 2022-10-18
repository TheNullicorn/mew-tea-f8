package me.nullicorn.mewteaf8

import java.io.EOFException
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReadMutf8LengthTests {

    @Test
    fun `readMutf8Length should interpret the next 2 bytes of the stream as a UShort in big-endian order`() {
        val lengths: Iterable<UShort> = buildSet {
            val random = Random(seed = "mew-tea-f8".hashCode())

            add(UShort.MIN_VALUE)
            add(UShort.MAX_VALUE)

            while (size < 1000) add(random.nextBits(UShort.SIZE_BITS).toUShort())
        }

        for (mutf8Length in lengths) {
            val stream = byteArrayOf(
                // Add the UShort's bytes in big-endian order.
                (mutf8Length.toInt() and 0xFF00 shr 8).toByte(),
                (mutf8Length.toInt() and 0x00FF).toByte(),
                // Add an extra byte that should NOT be read by the function. We'll check this at the end.
                0xFF.toByte()
            ).inputStream()

            assertEquals(expected = mutf8Length, actual = stream.readMutf8Length())

            // Ensure our extra byte, the last one, was not read by the function.
            assertEquals(expected = 0xFF, actual = stream.read() and 0xFF)
            assertEquals(expected = 0, stream.available())
        }
    }

    @Test
    fun `readMutf8Length should throw an EOFException if there are less than 2 bytes left in the stream`() {
        // Ensure it's thrown when there's 0 bytes.
        val emptyStream = ByteArray(size = 0).inputStream()
        assertFailsWith<EOFException> {
            emptyStream.readMutf8Length()
        }

        // Ensure it's thrown when there's only 1 byte, regardless of its value.
        for (byte in Byte.MIN_VALUE..Byte.MAX_VALUE) {
            val stream = ByteArray(size = 1) { byte.toByte() }.inputStream()

            assertFailsWith<EOFException> {
                stream.readMutf8Length()
            }
        }
    }
}