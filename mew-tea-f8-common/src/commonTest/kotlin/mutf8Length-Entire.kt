package me.nullicorn.mewteaf8

import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EntireMutf8LengthTests {

    @Test
    @JsName("A")
    fun `mutf8Length should be 0 for an empty string`() {
        val emptyChars = CharArray(size = 0)
        assertEquals(expected = 0L, actual = emptyChars.mutf8Length)
        assertEquals(expected = 0L, actual = emptyChars.concatToString().mutf8Length)
    }

    @Test
    @JsName("B")
    fun `mutf8Length should count the character U+0000 as 2 bytes each`() {
        for (amount in 1..20) {
            val nullChars = CharArray(size = amount) { '\u0000' }
            assertEquals(expected = 2L * amount, actual = nullChars.mutf8Length)
            assertEquals(expected = 2L * amount, actual = nullChars.concatToString().mutf8Length)
        }
    }

    @Test
    @JsName("C")
    fun `mutf8Length should count characters from U+0001 through U+007F as 1 byte each`() {
        // Test each character individually.
        for (char in singleByteOutputChars)
            for (amount in 1..20) {
                val chars = CharArray(size = amount) { char }
                assertEquals(expected = amount.toLong(), actual = chars.mutf8Length)
                assertEquals(expected = amount.toLong(), actual = chars.concatToString().mutf8Length)
            }

        // Test all of those characters together.
        val chars = singleByteOutputChars.toList().toCharArray()
        assertEquals(expected = singleByteOutputChars.count().toLong(), actual = chars.mutf8Length)
        assertEquals(expected = singleByteOutputChars.count().toLong(), actual = chars.concatToString().mutf8Length)
    }

    @Test
    @JsName("D")
    fun `mutf8Length should count characters from U+0080 through U+07FF as 2 bytes each`() {
        // Test each character individually.
        for (char in doubleByteOutputChars)
            for (amount in 1..20) {
                val chars = CharArray(size = amount) { char }
                assertEquals(expected = 2L * amount, actual = chars.mutf8Length)
                assertEquals(expected = 2L * amount, actual = chars.concatToString().mutf8Length)
            }

        // Test all of those characters together.
        val chars = doubleByteOutputChars.toList().toCharArray()
        assertEquals(expected = 2L * doubleByteOutputChars.count(), actual = chars.mutf8Length)
        assertEquals(expected = 2L * doubleByteOutputChars.count(), actual = chars.concatToString().mutf8Length)
    }

    @Test
    @JsName("E")
    fun `mutf8Length should count characters from U+0800 through U+FFFF as 3 bytes each`() {
        // Test each character individually.
        for (char in tripleByteOutputChars)
            for (amount in 1..20) {
                val chars = CharArray(size = amount) { char }
                assertEquals(expected = 3L * amount, actual = chars.mutf8Length)
                assertEquals(expected = 3L * amount, actual = chars.concatToString().mutf8Length)
            }

        // Test all of those characters together.
        val chars = tripleByteOutputChars.toList().toCharArray()
        assertEquals(expected = 3L * tripleByteOutputChars.count(), actual = chars.mutf8Length)
        assertEquals(expected = 3L * tripleByteOutputChars.count(), actual = chars.concatToString().mutf8Length)
    }

    @Test
    @JsName("F")
    fun `mutf8Length should correctly calculate the length of strings with characters of varying lengths in bytes`() {
        val chars = (singleByteOutputChars + doubleByteOutputChars + tripleByteOutputChars).toCharArray()
        chars.shuffle()

        val expectedLength = singleByteOutputChars.count() + (2L * doubleByteOutputChars.count()) + (3L * tripleByteOutputChars.count())
        assertEquals(expectedLength, actual = chars.mutf8Length)
        assertEquals(expectedLength, actual = chars.concatToString().mutf8Length)
    }

    @Test
    @JsName("G")
    fun `mutf8Length should throw an IllegalArgumentException if the length of the CharSequence is negative`() {
        for (charArray in sampleStrings)
            for (negativeLength in (-16..-1) + Int.MIN_VALUE) {
                val charSequence = object : CharSequence by charArray.toString() {
                    override val length: Int
                        get() = negativeLength
                }

                assertFailsWith<IllegalArgumentException> {
                    charSequence.mutf8Length
                }
            }
    }
}