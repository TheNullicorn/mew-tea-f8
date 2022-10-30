package me.nullicorn.mewteaf8.Mutf8Sink

import me.nullicorn.mewteaf8.IOException
import me.nullicorn.mewteaf8.Mutf8Sink
import me.nullicorn.mewteaf8.samples
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertFailsWith

class WriteFromArrayAndSequenceExceptionsTests {

    @Test
    @JsName("A")
    fun `writeFromSequence should throw an IllegalArgumentException if the length of the CharSequence is negative`() {
        for (charArray in samples)
            for (negativeLength in (-16..-1) + Int.MIN_VALUE) {
                val charSequence = object : CharSequence by charArray.toString() {
                    override val length: Int
                        get() = negativeLength
                }

                // Overload that takes an `IntRange`.
                assertFailsWith<IllegalArgumentException> {
                    BlackHoleMutf8Sink.writeFromSequence(charSequence, charArray.indices)
                }

                // Overload that takes a `startIndex` and `endIndex`.
                assertFailsWith<IllegalArgumentException> {
                    BlackHoleMutf8Sink.writeFromSequence(charSequence, startIndex = 0, endIndex = charArray.size)
                }

                // Overload that doesn't take a range.
                assertFailsWith<IllegalArgumentException> {
                    BlackHoleMutf8Sink.writeFromSequence(charSequence)
                }
            }
    }

    @Test
    @JsName("B")
    fun `writeFrom should throw an IndexOutOfBoundsException if the startIndex is negative`() {
        for (charArray in samples)
            for (startIndex in (-16..-1) + Int.MIN_VALUE) {
                val string = charArray.concatToString()

                // Overload that takes a `CharArray` and `startIndex` + `endIndex`.
                assertFailsWith<IndexOutOfBoundsException> {
                    BlackHoleMutf8Sink.writeFromArray(charArray, startIndex, endIndex = charArray.size)
                }

                // Overload that takes a `CharArray` and `IntRange`.
                assertFailsWith<IndexOutOfBoundsException> {
                    BlackHoleMutf8Sink.writeFromArray(charArray, range = startIndex until charArray.size)
                }

                // Overload that takes a `CharSequence` and `startIndex` + `endIndex`.
                assertFailsWith<IndexOutOfBoundsException> {
                    BlackHoleMutf8Sink.writeFromSequence(string, startIndex, endIndex = string.length)
                }

                // Overload that takes a `CharSequence` and `IntRange`.
                assertFailsWith<IndexOutOfBoundsException> {
                    BlackHoleMutf8Sink.writeFromSequence(string, range = startIndex until string.length)
                }
            }
    }

    @Test
    @JsName("C")
    fun `writeFrom should throw an IndexOutOfBoundsException if the startIndex is greater than or equal to the size or length`() {
        for (charArray in samples.filter { it.isNotEmpty() })
            for (startIndex in charArray.size..charArray.size + 15) {
                val string = charArray.concatToString()

                // Overload that takes a `CharArray` and `startIndex` + `endIndex`.
                assertFailsWith<IndexOutOfBoundsException> {
                    BlackHoleMutf8Sink.writeFromArray(charArray, startIndex, endIndex = startIndex)
                }

                // Overload that takes a `CharArray` and `IntRange`.
                assertFailsWith<IndexOutOfBoundsException> {
                    BlackHoleMutf8Sink.writeFromArray(charArray, range = startIndex until startIndex)
                }

                // Overload that takes a `CharSequence` and `startIndex` + `endIndex`.
                assertFailsWith<IndexOutOfBoundsException> {
                    BlackHoleMutf8Sink.writeFromSequence(string, startIndex, endIndex = startIndex)
                }

                // Overload that takes a `CharSequence` and `IntRange`.
                assertFailsWith<IndexOutOfBoundsException> {
                    BlackHoleMutf8Sink.writeFromSequence(string, range = startIndex until startIndex)
                }
            }
    }

    @Test
    @JsName("D")
    fun `writeFrom should throw an IndexOutOfBoundsException if the endIndex exceeds the size or length`() {
        for (charArray in samples)
            for (endIndex in charArray.size + 1..charArray.size + 16) {
                val string = charArray.concatToString()

                // Overload that takes a `CharArray` and `startIndex` + `endIndex`.
                assertFailsWith<IndexOutOfBoundsException> {
                    BlackHoleMutf8Sink.writeFromArray(charArray, startIndex = 0, endIndex)
                }

                // Overload that takes a `CharArray` and `IntRange`.
                assertFailsWith<IndexOutOfBoundsException> {
                    BlackHoleMutf8Sink.writeFromArray(charArray, range = 0 until endIndex)
                }

                // Overload that takes a `CharSequence` and `startIndex` + `endIndex`.
                assertFailsWith<IndexOutOfBoundsException> {
                    BlackHoleMutf8Sink.writeFromSequence(string, startIndex = 0, endIndex)
                }

                // Overload that takes a `CharSequence` and `IntRange`.
                assertFailsWith<IndexOutOfBoundsException> {
                    BlackHoleMutf8Sink.writeFromSequence(string, range = 0 until endIndex)
                }
            }
    }

    @Test
    @JsName("E")
    fun `writeFrom should throw an IllegalArgumentException if the startIndex is greater than the endIndex`() {
        // For this test we'll set the `endIndex` to always be `0`, and the `startIndex` to be every index greater than
        // that (hence why we exclude `0` from the list via `- 0`).
        for (charArray in samples)
            for (startIndex in charArray.indices - 0) {
                val string = charArray.concatToString()

                // Overload that takes a `CharArray` and `startIndex` + `endIndex`.
                assertFailsWith<IllegalArgumentException> {
                    BlackHoleMutf8Sink.writeFromArray(charArray, startIndex, endIndex = 0)
                }

                // Overload that takes a `CharArray` and `IntRange`.
                assertFailsWith<IllegalArgumentException> {
                    BlackHoleMutf8Sink.writeFromArray(charArray, range = startIndex until 0)
                }

                // Overload that takes a `CharSequence` and `startIndex` + `endIndex`.
                assertFailsWith<IllegalArgumentException> {
                    BlackHoleMutf8Sink.writeFromSequence(string, startIndex, endIndex = 0)
                }

                // Overload that takes a `CharSequence` and `IntRange`.
                assertFailsWith<IllegalArgumentException> {
                    BlackHoleMutf8Sink.writeFromSequence(string, range = startIndex until 0)
                }
            }
    }

    @Test
    @JsName("F")
    fun `writeFrom should propagate any exceptions thrown by writeBytes`() {
        val ioExceptionSink = object : Mutf8Sink() {
            override fun writeBytes(bytes: ByteArray, untilIndex: Int) {
                throw IOException("This should reach the test; it should not be caught or handled by writeFrom*()")
            }
        }

        // Exclude empty samples because they don't have anything to write, so `writeBytes()` will never be called.
        for (charArray in samples.filter { it.isNotEmpty() }) {
            val string = charArray.concatToString()

            // Overload that takes a `CharArray` and `startIndex` + `endIndex`.
            assertFailsWith<IOException> {
                ioExceptionSink.writeFromArray(charArray, startIndex = 0, endIndex = charArray.size)
            }

            // Overload that takes a `CharArray` and `IntRange`.
            assertFailsWith<IOException> {
                ioExceptionSink.writeFromArray(charArray, range = charArray.indices)
            }

            // Overload that takes a `CharSequence` and `startIndex` + `endIndex`.
            assertFailsWith<IOException> {
                ioExceptionSink.writeFromSequence(string, startIndex = 0, endIndex = string.length)
            }

            // Overload that takes a `CharSequence` and `IntRange`.
            assertFailsWith<IOException> {
                ioExceptionSink.writeFromSequence(string, range = string.indices)
            }
        }
    }
}