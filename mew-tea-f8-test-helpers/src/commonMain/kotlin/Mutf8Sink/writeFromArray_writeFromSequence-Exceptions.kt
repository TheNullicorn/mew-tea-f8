package me.nullicorn.mewteaf8.Mutf8Sink

import me.nullicorn.mewteaf8.*
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertFailsWith

abstract class WriteCharactersExceptionallyTests<Sink : Mutf8Sink, Destination> : Mutf8SinkTests<Sink, Destination>() {

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
                var sink = Sink()
                assertFailsWith<IllegalArgumentException> {
                    sink.writeFromSequence(charSequence, charArray.indices)
                }

                // Overload that takes a `startIndex` and `endIndex`.
                sink = Sink()
                assertFailsWith<IllegalArgumentException> {
                    sink.writeFromSequence(charSequence, startIndex = 0, endIndex = charArray.size)
                }

                // Overload that doesn't take a range.
                sink = Sink()
                assertFailsWith<IllegalArgumentException> {
                    sink.writeFromSequence(charSequence)
                }
            }
    }

    @Test
    @JsName("B")
    fun `writeFrom should throw an IndexOutOfBoundsException if the startIndex is negative`() {
        for (charArray in sampleStrings)
            for (startIndex in (-16..-1) + Int.MIN_VALUE)
                assertAllMethodsFailWith<IndexOutOfBoundsException>(
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
                    chars = charArray,
                    startIndex = startIndex,
                    endIndex = 0
                )
    }

    @Test
    @JsName("F")
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
                chars = justShortEnoughArray
            )

            // By adding a 1-byte character to the end of the array, we push its `mutf8Length` too high, so it should
            // now throw an exception.
            assertAllMethodsFailWith<IllegalArgumentException>(
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
                    chars = extraLongArray,
                    startIndex = startIndex,
                    endIndex = startIndex + justShortEnoughRangeSize
                )

                // If we increase the range's size by just 1 more, that should put it over the limit and cause the
                // exception.
                assertAllMethodsFailWith<IllegalArgumentException>(
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
     * @param[E] The exception that each of the sink's range-based methods should throw, given the specified
     * parameters. Acts the same as the reified type parameter of [assertFailsWith], `T`.
     * @param[chars] The characters that the [startIndex] and [endIndex] are based on, and which will be passed to the
     * sink's methods as both a [CharArray] and [CharSequence].
     * @param[startIndex] The value of the `startIndex` parameter passed to the sink's methods, or for methods that take
     * an [IntRange], the first operand of the [until] function.
     * @param[endIndex] The value of the `endIndex` parameter passed to the sink's methods, or for methods that take an
     * [IntRange], the second (right) operand of the [until] function.
     */
    private inline fun <reified E : Exception> assertAllMethodsFailWith(
        chars: CharArray,
        startIndex: Int = 0,
        endIndex: Int = chars.size
    ) {
        val string = chars.concatToString()
        var sink: Sink

        // If the `startIndex` and `endIndex` cover the entire array, then also test the overloads that don't take any
        // range, since they also write the entire array.
        if (startIndex == 0 && endIndex == chars.size) {
            sink = Sink()
            assertFailsWith<E> {
                sink.writeFromArray(chars)
            }

            sink = Sink()
            assertFailsWith<E> {
                sink.writeFromSequence(string)
            }
        }

        // Overload that takes a `CharArray` and `startIndex` + `endIndex`.
        sink = Sink()
        assertFailsWith<E> {
            sink.writeFromArray(chars, startIndex, endIndex)
        }

        // Overload that takes a `CharArray` and `IntRange`.
        sink = Sink()
        assertFailsWith<E> {
            sink.writeFromArray(chars, range = startIndex until endIndex)
        }

        // Overload that takes a `CharSequence` and `startIndex` + `endIndex`.
        sink = Sink()
        assertFailsWith<E> {
            sink.writeFromSequence(string, startIndex, endIndex)
        }

        // Overload that takes a `CharSequence` and `IntRange`.
        sink = Sink()
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
     * @param[chars] The characters that the [startIndex] and [endIndex] are based on, and which will be passed to the
     * sink's methods as both a [CharArray] and [CharSequence].
     * @param[startIndex] The value of the `startIndex` parameter passed to the sink's methods, or for methods that take
     * an [IntRange], the first operand of the [until] function.
     * @param[endIndex] The value of the `endIndex` parameter passed to the sink's methods, or for methods that take an
     * [IntRange], the second (right) operand of the [until] function.
     */
    private fun assertAllMethodsSucceed(
        chars: CharArray,
        startIndex: Int = 0,
        endIndex: Int = chars.size
    ) {
        val string = chars.concatToString()

        // If the `startIndex` and `endIndex` cover the entire array, then also test the overloads that don't take any
        // range, since they also write the entire array.
        if (startIndex == 0 && endIndex == chars.size) {
            Sink().writeFromArray(chars)
            Sink().writeFromSequence(string)
        }

        // Overload that takes a `CharArray` and `startIndex` + `endIndex`.
        Sink().writeFromArray(chars, startIndex, endIndex)

        // Overload that takes a `CharArray` and `IntRange`.
        Sink().writeFromArray(chars, range = startIndex until endIndex)

        // Overload that takes a `CharSequence` and `startIndex` + `endIndex`.
        Sink().writeFromSequence(string, startIndex, endIndex)

        // Overload that takes a `CharSequence` and `IntRange`.
        Sink().writeFromSequence(string, range = startIndex until endIndex)
    }
}