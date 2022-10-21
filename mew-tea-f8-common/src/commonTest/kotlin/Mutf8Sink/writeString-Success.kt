#!idk

@file:Suppress("PackageName")

package me.nullicorn.mewteaf8.Mutf8Sink

import me.nullicorn.mewteaf8.*
import me.nullicorn.mewteaf8.internal.InternalMutf8Api
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertContentEquals

@OptIn(InternalMutf8Api::class)
class WriteStringSuccessfullyTests {

    @Test
    @JsName("A")
    fun `writeString should encode characters from U+0001 through U+007F using 1 byte each`() {
        testSuccessfulEncodingOfRange(singleByteOutputSamples, writeChar = { char ->
            add1stOf1Bytes(char)
        })
    }

    @Test
    @JsName("B")
    fun `writeString should encode characters from U+0080 through U+07FF using 2 bytes each`() {
        testSuccessfulEncodingOfRange(doubleByteOutputSamples, writeChar = { char ->
            add1stOf2Bytes(char)
            add2ndOf2Bytes(char)
        })
    }

    @Test
    @JsName("C")
    fun `writeString should encode characters from U+0800 through U+FFFF using 3 bytes each`() {
        testSuccessfulEncodingOfRange(tripleByteOutputSamples, writeChar = { char ->
            add1stOf3Bytes(char)
            add2ndOf3Bytes(char)
            add3rdOf3Bytes(char)
        })
    }

    @Test
    @JsName("D")
    fun `writeString should encode U+0000 using 2 bytes`() {
        testSuccessfulEncodingOfRange('\u0000'.toString().asIterable(), writeChar = { char ->
            add1stOf2Bytes(char)
            add2ndOf2Bytes(char)
        })
    }

    @Test
    @JsName("E")
    fun `writeString should correctly encode strings with characters of varying lengths in bytes`() {
        testSuccessfulEncodingOfRange(
            chars = singleByteOutputSamples + doubleByteOutputSamples + tripleByteOutputSamples,
            writeChar = { char ->
                if (char in '\u0001'..'\u007F') {
                    add1stOf1Bytes(char)
                } else if (char < '\u0800') {
                    add1stOf2Bytes(char)
                    add2ndOf2Bytes(char)
                } else {
                    add1stOf3Bytes(char)
                    add2ndOf3Bytes(char)
                    add3rdOf3Bytes(char)
                }
            })
    }

    private inline fun testSuccessfulEncodingOfRange(
        chars: Iterable<Char>,
        crossinline writeChar: MutableList<Byte>.(Char) -> Unit
    ) {
        val stringOfAllChars = chars.shuffled(random = createReproducableRandom()).joinToString(separator = "")

        for (string in setOf(stringOfAllChars)) {
            val expectedBytes = buildList {
                for (char in string)
                    writeChar(char)
            }

            val sink = TestingMutf8Sink()
            sink.writeString(string)

            assertContentEquals(expectedBytes, actual = sink.bytes)
        }
    }
}