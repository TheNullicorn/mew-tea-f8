package me.nullicorn.mewteaf8

import kotlin.js.JsName
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class Mutf8LengthTests {

    private val random = Random(seed = "mew-tea-f8".hashCode())

    // Characters encoded using 1 byte each. This range contains 127 characters.
    private val singleByteChars: Iterable<Char> = '\u0001'..'\u007F'

    // Characters encoded using 2 bytes each. This range contains 1,920 characters.
    private val doubleByteChars: Iterable<Char> = '\u0080'..'\u07FF'

    // Characters encoded using 3 bytes each. There's too many to test them all (63,488), so we choose 1,000 at random.
    private val tripleByteChars: Iterable<Char> = buildSet {
        // Always include the upper & lower bounds of the 3-byte char range.
        add('\u0800')
        add('\uFFFF')

        // Add 998 other randomly selected characters. `random` has a constant seed, so these will always be the same.
        while (size < 1000) {
            val char = random.nextBits(bitCount = 16).toChar()
            if (char in '\u0800'..'\uFFFF') add(char)
        }
    }

    @Test
    @JsName("A")
    fun `mutf8Length should be 0 for an empty string`() {
        val emptyString = CharArray(size = 0).concatToString()
        assertEquals(expected = 0L, actual = emptyString.mutf8Length)
    }

    @Test
    @JsName("B")
    fun `mutf8Length should count the character U+0000 as 2 bytes each`() {
        for (amount in 1..20) {
            val stringOfNulls = CharArray(size = amount) { '\u0000' }.concatToString()
            assertEquals(expected = 2L * amount, actual = stringOfNulls.mutf8Length)
        }
    }

    @Test
    @JsName("C")
    fun `mutf8Length should count characters from U+0001 through U+007F as 1 byte each`() {
        // Test each character individually.
        for (char in singleByteChars)
            for (amount in 1..20) {
                val string = CharArray(size = amount) { char }.concatToString()
                assertEquals(expected = amount.toLong(), actual = string.mutf8Length)
            }

        // Test all of those characters together.
        val string = singleByteChars.joinToString(separator = "")
        assertEquals(expected = singleByteChars.count().toLong(), actual = string.mutf8Length)
    }

    @Test
    @JsName("D")
    fun `mutf8Length should count characters from U+0080 through U+07FF as 2 bytes each`() {
        // Test each character individually.
        for (char in doubleByteChars)
            for (amount in 1..20) {
                val string = CharArray(size = amount) { char }.concatToString()
                assertEquals(expected = 2L * amount, actual = string.mutf8Length)
            }

        // Test all of those characters together.
        val string = doubleByteChars.joinToString(separator = "")
        assertEquals(expected = 2L * doubleByteChars.count(), actual = string.mutf8Length)
    }

    @Test
    @JsName("E")
    fun `mutf8Length should count characters from U+0800 through U+FFFF as 3 bytes each`() {
        // Test each character individually.
        for (char in tripleByteChars)
            for (amount in 1..20) {
                val string = CharArray(size = amount) { char }.concatToString()
                assertEquals(expected = 3L * amount, actual = string.mutf8Length)
            }

        // Test all of those characters together.
        val string = tripleByteChars.joinToString(separator = "")
        assertEquals(expected = 3L * tripleByteChars.count(), actual = string.mutf8Length)
    }

    @Test
    @JsName("F")
    fun `mutf8Length should correctly calculate the length of strings with characters of varying lengths in bytes`() {
        val chars = (singleByteChars + doubleByteChars + tripleByteChars).toCharArray()
        chars.shuffle()
        val string = chars.concatToString()

        val expected = singleByteChars.count() + (2L * doubleByteChars.count()) + (3L * tripleByteChars.count())
        assertEquals(expected, actual = string.mutf8Length)
    }
}