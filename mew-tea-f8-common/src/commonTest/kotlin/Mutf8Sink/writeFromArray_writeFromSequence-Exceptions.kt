package me.nullicorn.mewteaf8.Mutf8Sink

import me.nullicorn.mewteaf8.*
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertFailsWith

class WriteFromArrayAndSequenceExceptionsTests {

    @Test
    @JsName("A")
    fun `writeFromSequence should throw an IllegalArgumentException if the length of the CharSequence is negative`() {
        for (charArray in sampleStrings)
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
        for (charArray in sampleStrings)
            for (startIndex in (-16..-1) + Int.MIN_VALUE)
                assertAllMethodsFailWith<IndexOutOfBoundsException>(
                    sink = BlackHoleMutf8Sink,
                    chars = charArray,
                    startIndex = startIndex,
                    endIndex = charArray.size
                )
    }

    @Test
    @JsName("C")
    fun `writeFrom should throw an IndexOutOfBoundsException if the startIndex is greater than or equal to the size or length`() {
        for (charArray in sampleStrings.filter { it.isNotEmpty() })
            for (startIndex in charArray.size..charArray.size + 15)
                assertAllMethodsFailWith<IndexOutOfBoundsException>(
                    sink = BlackHoleMutf8Sink,
                    chars = charArray,
                    startIndex = startIndex,
                    endIndex = startIndex
                )
    }

    @Test
    @JsName("D")
    fun `writeFrom should throw an IndexOutOfBoundsException if the endIndex exceeds the size or length`() {
        for (charArray in sampleStrings)
            for (endIndex in charArray.size + 1..charArray.size + 16)
                assertAllMethodsFailWith<IndexOutOfBoundsException>(
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
        for (charArray in sampleStrings)
            for (startIndex in charArray.indices - 0)
                assertAllMethodsFailWith<IllegalArgumentException>(
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
            override fun writeLength(mutf8Length: Int) =
                throw UnsupportedOperationException("Not the method being tested")
        }

        // Exclude empty samples because they don't have anything to write, so `writeBytes()` will never be called.
        for (charArray in sampleStrings.filter { it.isNotEmpty() })
            assertAllMethodsFailWith<IOException>(
                sink = ioExceptionSink,
                chars = charArray,
                // This range is valid, so they shouldn't cause an exception.
                startIndex = 0,
                endIndex = charArray.size
            )
    }

    @Test
    @JsName("G")
    fun `writeFrom should throw an IllegalArgumentException if the mutf8Length of the characters in the range is greater than 65535`() {
        val random = createReproducibleRandom()
        val maxMutf8Length = UShort.MAX_VALUE.toInt()
        val singleByteChar = singleByteOutputChars.first()
        val doubleByteChar = doubleByteOutputChars.first()
        val tripleByteChar = tripleByteOutputChars.first()

        fun testWithChar(char: Char) {
            val mutf8Length = char.mutf8Length
            val justShortEnoughArray = StringBuilder(char.toString().repeat(maxMutf8Length / mutf8Length))
                .also {
                    while (it.mutf8Length < maxMutf8Length)
                        it.append(singleByteChar)
                }.toString().toCharArray()

            // Nothing should be thrown with the array as it is because its `mutf8Length` is exactly UShort.MAX_VALUE.
            assertAllMethodsSucceed(
                sink = BlackHoleMutf8Sink,
                chars = justShortEnoughArray
            )

            // By adding a 1-byte character to the end of the array, we push its `mutf8Length` too high, so it should
            // now throw an exception.
            assertAllMethodsFailWith<IllegalArgumentException>(
                sink = BlackHoleMutf8Sink,
                chars = justShortEnoughArray + singleByteChar
            )

            // Double the size of the previous array so that we can start testing just-short-enough and just-too-long
            // ranges within it.
            val extraLongArray = justShortEnoughArray + justShortEnoughArray
            val justShortEnoughRangeSize = justShortEnoughArray.size

            // Check 20 random ranges (20 is arbitrary).
            for (i in 0..20) {
                // Choose a random index as the `startIndex` ensuring that the `endIndex`, which will be
                // `justShortEnoughRangeSize` indices greater, will still be within the array's bounds.
                val startIndex = random.nextInt(until = extraLongArray.size - justShortEnoughRangeSize - 1)

                // The `mutf8Length` of the range we chose should be just shy of UShort.MAX_VALUE, so no exception
                // should be thrown.
                assertAllMethodsSucceed(
                    sink = BlackHoleMutf8Sink,
                    chars = extraLongArray,
                    startIndex = startIndex,
                    endIndex = startIndex + justShortEnoughRangeSize
                )

                // If we increase the range's size by just 1 more, that should put it over the limit and cause the
                // exception.
                assertAllMethodsFailWith<IllegalArgumentException>(
                    sink = BlackHoleMutf8Sink,
                    chars = extraLongArray,
                    startIndex = startIndex,
                    endIndex = startIndex + justShortEnoughRangeSize + 1
                )
            }
        }

        testWithChar(singleByteChar)
        testWithChar(doubleByteChar)
        testWithChar(tripleByteChar)
    }

    /**
     * Asserts that all (or most) methods of [Mutf8Sink] throw a specific exception [E] when supplied with the given
     * [chars], [startIndex], and [endIndex].
     *
     * If `startIndex` is `0` and `endIndex` is `chars.size`, then the overloads of
     * [writeFromArray][Mutf8Sink.writeFromArray], [writeFromSequence][Mutf8Sink.writeFromSequence] will also be tested.
     * Otherwise, they'll be excluded because there's no way to specify a custom range of indices with them. The
     * aforementioned values are the same as the default values those two overloads.
     *
     * @param[E] The exception that each of the [sink]'s range-based methods should throw, given the specified
     * parameters. Acts the same as the reified type parameter of [assertFailsWith], `T`.
     * @param[sink] The sink to call each method on, expected the exception [E] to be thrown.
     * @param[chars] The characters that the [startIndex] and [endIndex] are based on, and which will be passed to the
     * [sink]'s methods as both a [CharArray] and [CharSequence].
     * @param[startIndex] The value of the `startIndex` parameter passed to the [sink]'s methods, or for methods that
     * take an [IntRange], the first operand of the [until] function.
     * @param[endIndex] The value of the `endIndex` parameter passed to the [sink]'s methods, or for methods that take
     * an [IntRange], the second (right) operand of the [until] function.
     */
    private inline fun <reified E : Exception> assertAllMethodsFailWith(
        sink: Mutf8Sink,
        chars: CharArray,
        startIndex: Int = 0,
        endIndex: Int = chars.size
    ) {
        val string = chars.concatToString()

        // If the `startIndex` and `endIndex` cover the entire array, then also test the overloads that don't take any
        // range, since they also write the entire array.
        if (startIndex == 0 && endIndex == chars.size) {
            assertFailsWith<E> {
                sink.writeFromArray(chars)
            }

            assertFailsWith<E> {
                sink.writeFromSequence(string)
            }
        }

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

    /**
     * Asserts that all (or most) methods of [Mutf8Sink] do not raise any exceptions when supplied with the given
     * [chars], [startIndex], and [endIndex].
     *
     * If `startIndex` is `0` and `endIndex` is `chars.size`, then the overloads of
     * [writeFromArray][Mutf8Sink.writeFromArray], [writeFromSequence][Mutf8Sink.writeFromSequence] will also be tested.
     * Otherwise, they'll be excluded because there's no way to specify a custom range of indices with them. The
     * aforementioned values are the same as the default values those two overloads.
     *
     * @param[sink] The sink to call each method on.
     * @param[chars] The characters that the [startIndex] and [endIndex] are based on, and which will be passed to the
     * [sink]'s methods as both a [CharArray] and [CharSequence].
     * @param[startIndex] The value of the `startIndex` parameter passed to the [sink]'s methods, or for methods that
     * take an [IntRange], the first operand of the [until] function.
     * @param[endIndex] The value of the `endIndex` parameter passed to the [sink]'s methods, or for methods that take
     * an [IntRange], the second (right) operand of the [until] function.
     */
    private fun assertAllMethodsSucceed(
        sink: Mutf8Sink,
        chars: CharArray,
        startIndex: Int = 0,
        endIndex: Int = chars.size
    ) {
        val string = chars.concatToString()

        // If the `startIndex` and `endIndex` cover the entire array, then also test the overloads that don't take any
        // range, since they also write the entire array.
        if (startIndex == 0 && endIndex == chars.size) {
            sink.writeFromArray(chars)
            sink.writeFromSequence(string)
        }

        // Overload that takes a `CharArray` and `startIndex` + `endIndex`.
        sink.writeFromArray(chars, startIndex, endIndex)

        // Overload that takes a `CharArray` and `IntRange`.
        sink.writeFromArray(chars, range = startIndex until endIndex)

        // Overload that takes a `CharSequence` and `startIndex` + `endIndex`.
        sink.writeFromSequence(string, startIndex, endIndex)

        // Overload that takes a `CharSequence` and `IntRange`.
        sink.writeFromSequence(string, range = startIndex until endIndex)
    }
}