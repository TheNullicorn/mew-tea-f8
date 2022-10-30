package me.nullicorn.mewteaf8

import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

class Mutf8LengthOfCharTests {

    @Test
    @JsName("A")
    fun `mutf8Length should be 1 for characters from U+0001 until U+0080`() {
        for (char in '\u0001'..'\u007F')
            assertEquals(expected = 1, actual = char.mutf8Length)
    }

    @Test
    @JsName("B")
    fun `mutf8Length should be 2 for characters from U+0080 until U+0800`() {
        for (char in '\u0080'..'\u07FF')
            assertEquals(expected = 2, actual = char.mutf8Length)
    }

    @Test
    @JsName("C")
    fun `mutf8Length should be 2 for the character U+0000`() {
        assertEquals(expected = 2, actual = '\u0000'.mutf8Length)
    }

    @Test
    @JsName("D")
    fun `mutf8Length should be 3 for characters from U+0080 through U+FFFF`() {
        for (char in tripleByteOutputChars)
            assertEquals(expected = 3, actual = char.mutf8Length)
    }
}