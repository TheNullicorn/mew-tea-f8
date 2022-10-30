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
            for (startIndex in (-16..-1) + Int.MIN_VALUE)
                assertAllRangedMethodsFailWith<IndexOutOfBoundsException>(
                    sink = BlackHoleMutf8Sink,
                    chars = charArray,
                    startIndex = startIndex,
                    endIndex = charArray.size
                )
    }

    @Test
    @JsName("C")
    fun `writeFrom should throw an IndexOutOfBoundsException if the startIndex is greater than or equal to the size or length`() {
        for (charArray in samples.filter { it.isNotEmpty() })
            for (startIndex in charArray.size..charArray.size + 15)
                assertAllRangedMethodsFailWith<IndexOutOfBoundsException>(
                    sink = BlackHoleMutf8Sink,
                    chars = charArray,
                    startIndex = startIndex,
                    endIndex = startIndex
                )
    }

    @Test
    @JsName("D")
    fun `writeFrom should throw an IndexOutOfBoundsException if the endIndex exceeds the size or length`() {
        for (charArray in samples)
            for (endIndex in charArray.size + 1..charArray.size + 16)
                assertAllRangedMethodsFailWith<IndexOutOfBoundsException>(
                    sink = BlackHoleMutf8Sink,
                    chars = charArray,
                    startIndex = 0,
                    endIndex = endIndex
                )
    }

    @Test
    @JsName("E")
    fun `writeFrom should throw an IllegalArgumentException if the startIndex is greater than the endIndex`() {
        // For this test we'll set the `endIndex` to always be `0`, and the `startIndex` to be every index greater than
        // that (hence why we exclude `0` from the list via `- 0`).
        for (charArray in samples)
            for (startIndex in charArray.indices - 0)
                assertAllRangedMethodsFailWith<IllegalArgumentException>(
                    sink = BlackHoleMutf8Sink,
                    chars = charArray,
                    startIndex = startIndex,
                    endIndex = 0
                )
    }

    @Test
    @JsName("F")
    fun `writeFrom should propagate any exceptions thrown by writeBytes`() {
        // A sink that always throws an `IOException` when written to.
        val ioExceptionSink = object : Mutf8Sink() {
            override fun writeBytes(bytes: ByteArray, untilIndex: Int) {
                throw IOException("This should reach the test; it should not be caught or handled by writeFrom*()")
            }
        }

        // Exclude empty samples because they don't have anything to write, so `writeBytes()` will never be called.
        for (charArray in samples.filter { it.isNotEmpty() })
            assertAllRangedMethodsFailWith<IOException>(
                sink = ioExceptionSink,
                chars = charArray,
                // This range is valid, so they shouldn't cause an exception.
                startIndex = 0,
                endIndex = charArray.size
            )
    }

    /**
     * Asserts that all methods of [Mutf8Sink] that accept a range, be it an [IntRange] or a `startIndex` & `endIndex`,
     * throw a specific exception [E] when supplied with the given [chars], [startIndex], and [endIndex].
     *
     * @param[E] The exception that each of the [sink]'s range-based methods should throw, given the specified
     * parameters. Acts the same as the reified type parameter of [assertFailsWith], `T`.
     * @param[sink] The sink to call each range-based method on, expected the exception [E] to be thrown.
     * @param[chars] The characters that the [startIndex] and [endIndex] are based on, and which will be passed to the
     * [sink]'s methods as both a [CharArray] and [CharSequence].
     * @param[startIndex] The value of the `startIndex` parameter passed to the [sink]'s methods, or for methods that
     * take an [IntRange], the first operand of the [until] function.
     * @param[endIndex] The value of the `endIndex` parameter passed to the [sink]'s methods, or for methods that take
     * an [IntRange], the second (right) operand of the [until] function.
     */
    private inline fun <reified E : Exception> assertAllRangedMethodsFailWith(
        sink: Mutf8Sink,
        chars: CharArray,
        startIndex: Int,
        endIndex: Int
    ) {
        val string = chars.concatToString()

        // Overload that takes a `CharArray` and `startIndex` + `endIndex`.
        assertFailsWith<E> {
            sink.writeFromArray(chars, startIndex, endIndex)
        }

        // Overload that takes a `CharArray` and `IntRange`.
        assertFailsWith<E> {
            sink.writeFromArray(chars, range = startIndex until endIndex)
        }

        // Overload that takes a `CharSequence` and `startIndex` + `endIndex`.
        assertFailsWith<E> {
            sink.writeFromSequence(string, startIndex, endIndex)
        }

        // Overload that takes a `CharSequence` and `IntRange`.
        assertFailsWith<E> {
            sink.writeFromSequence(string, range = startIndex until endIndex)
        }
    }
}