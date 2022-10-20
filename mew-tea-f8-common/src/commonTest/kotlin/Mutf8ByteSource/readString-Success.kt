@file:Suppress("PackageName")

package me.nullicorn.mewteaf8.Mutf8ByteSource

import me.nullicorn.mewteaf8.*
import me.nullicorn.mewteaf8.internal.InternalMutf8Api
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(InternalMutf8Api::class)
class ReadStringSuccessfullyTests {

    @Test
    @JsName("A")
    fun `readString should return an empty string if utfLength is 0`() {
        val emptyString = CharArray(size = 0).concatToString()
        val emptySource = buildTestSource { }


        assertEquals(expected = emptyString, actual = emptySource.readString(utfLength = 0u))
    }

    @Test
    @JsName("B")
    fun `readString should decode a character using 1 byte when that byte's bits follow the pattern 0xxxxxxx`() {
        // We include U+0000 in this range because even though no normal writer should ever encode it using 1 byte,
        // Java's reader allows for it, so we will too.
        testSuccessfulDecodingOfRange('\u0000'..'\u007F', writeChar = { char ->
            add1stOf1Bytes(char)
        })
    }

    @Test
    @JsName("C")
    fun `readString should decode a character using 2 bytes when the first byte's bits follow the pattern 110xxxxx`() {
        // We include (U+0001 .. U+007F) in this range because even though no normal writer should ever encode those
        // using 2 bytes, Java's reader allows for it, so we will too.
        testSuccessfulDecodingOfRange('\u0000'..'\u07FF', writeChar = { char ->
            add1stOf2Bytes(char)
            add2ndOf2Bytes(char)
        })
    }

    @Test
    @JsName("D")
    fun `readString should decode a character using 3 bytes when the first byte's bits follow the pattern 1110xxxx`() {
        // We include (U+0000 .. U+07FF) in this range because even though no normal writer should ever encode those
        // using 3 bytes, Java's reader allows for it, so we will too. Then, because (U+0800 .. U+FFFF) is such a big
        // range (60k+ chars), we randomly select 1,000 of those to test.
        val charsToTest = ('\u0000'..'\u07FF') + buildSet {
            add('\u0800')
            add('\uFFFF')

            val random = createReproducableRandom()
            while (size < 1000) {
                val threeByteChar = random.nextBits(Char.SIZE_BITS).toChar()
                if (threeByteChar in '\u0800'..'\uFFFF') add(threeByteChar)
            }
        }

        testSuccessfulDecodingOfRange(charsToTest, writeChar = { char ->
            add1stOf3Bytes(char)
            add2ndOf3Bytes(char)
            add3rdOf3Bytes(char)
        })
    }

    /**
     * Code shared between this class's other tests, which differ only in which characters they test and how those
     * characters are encoded.
     */
    private inline fun testSuccessfulDecodingOfRange(
        chars: Iterable<Char>,
        crossinline writeChar: MutableList<Byte>.(Char) -> Unit
    ) {
        val charList = chars.toList()

        // Test that each char is decoded correctly on its own, without any other characters around it.
        for (char in charList) {
            val string = char.toString()
            val source = buildTestSource {
                writeChar(char)
            }
            assertEquals(expected = string, actual = source.readString(utfLength = source.size.toUShort()))
        }

        // Put all the characters into one big string and make sure that it is decoded correctly.
        val string = charList.toCharArray().apply { shuffle(random = createReproducableRandom()) }.concatToString()
        val source = buildTestSource {
            for (char in string)
                writeChar(char)
        }
        assertEquals(expected = string, actual = source.readString(utfLength = source.size.toUShort()))
    }
}