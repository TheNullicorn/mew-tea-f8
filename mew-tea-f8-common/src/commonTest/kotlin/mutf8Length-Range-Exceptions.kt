package me.nullicorn.mewteaf8

import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Mutf8LengthWithRangeExceptionsTests {

    @Test
    @JsName("A")
    fun `mutf8Length should throw an IndexOutOfBoundsException if the startIndex is less than 0`() {
        for (charArray in sampleStrings)
            for (index in (-16..-1) + Int.MIN_VALUE) {
                // Test `CharArray` overload.
                assertFailsWith<IndexOutOfBoundsException> {
                    charArray.mutf8Length(startIndex = index, endIndex = charArray.lastIndex + 1)
                }

                // Test `CharSequence` overload.
                assertFailsWith<IndexOutOfBoundsException> {
                    charArray.concatToString().mutf8Length(startIndex = index, endIndex = charArray.lastIndex + 1)
                }
            }
    }

    @Test
    @JsName("B")
    fun `mutf8Length should throw an IndexOutOfBoundsException if the endIndex is greater than the length or size`() {
        for (charArray in sampleStrings)
            for (index in (charArray.size + 1..charArray.size + 16) + Int.MAX_VALUE) {
                // Test `CharArray` overload.
                assertFailsWith<IndexOutOfBoundsException> {
                    charArray.mutf8Length(startIndex = 0, endIndex = index)
                }

                // Test `CharSequence` overload.
                assertFailsWith<IndexOutOfBoundsException> {
                    charArray.concatToString().mutf8Length(startIndex = 0, endIndex = index)
                }
            }
    }

    @Test
    @JsName("C")
    fun `mutf8Length should throw an IndexOutOfBoundsException if the startIndex is greater than or equal to the length or size`() {
        for (charArray in sampleStrings.filter { it.isNotEmpty() }) {
            // Test `CharArray` overload.
            assertFailsWith<IndexOutOfBoundsException> {
                charArray.mutf8Length(startIndex = charArray.size, endIndex = charArray.size)
            }

            // Test `CharSequence` overload.
            assertFailsWith<IndexOutOfBoundsException> {
                charArray.concatToString().mutf8Length(startIndex = charArray.size, endIndex = charArray.size)
            }
        }
    }

    @Test
    @JsName("D")
    fun `mutf8Length should throw an IllegalArgumentException if the length of the CharSequence is negative`() {
        for (charArray in sampleStrings)
            for (negativeLength in (-16..-1) + Int.MIN_VALUE) {
                val charSequence = object : CharSequence by charArray.toString() {
                    override val length: Int
                        get() = negativeLength
                }

                assertFailsWith<IllegalArgumentException> {
                    charSequence.mutf8Length(startIndex = 0, endIndex = charArray.lastIndex + 1)
                }
            }
    }

    @Test
    @JsName("E")
    fun `mutf8Length should throw an IllegalArgumentException if the startIndex is greater than the endIndex`() {
        // For this test we'll set the `endIndex` to always be `0`, and the `startIndex` to be every index greater than
        // that (hence why we exclude `0` from the list via `- 0`).
        for (charArray in sampleStrings)
            for (startIndex in charArray.indices - 0) {
                assertFailsWith<IllegalArgumentException> {
                    charArray.mutf8Length(startIndex, endIndex = 0)
                }

                assertFailsWith<IllegalArgumentException> {
                    charArray.concatToString().mutf8Length(startIndex, endIndex = 0)
                }
            }
    }
}