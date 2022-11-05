package me.nullicorn.mewteaf8.Mutf8Source

import me.nullicorn.mewteaf8.*
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

abstract class ReadCharactersSuccessfullyTests<Source : Mutf8Source> : Mutf8SourceTests<Source>() {

    @Test
    @JsName("A")
    fun `readTo should return an empty string if mutf8Length is 0`() {
        val source = Source()
        val emptyString = CharArray(size = 0).concatToString()
        assertEquals(expected = emptyString, actual = source.readToString(mutf8Length = 0))
    }

    @Test
    @JsName("B")
    fun `readTo should interpret a byte as a single 7-bit character when its bits follow the pattern 0xxxxxxx`() {
        assertFixedSizedCharactersAreDecodedCorrectly(
            singleByteInputChars,
            bytesPerChar = 1,
            writeOneChar = { char ->
                add1stOf1Bytes(char)
            })
    }

    @Test
    @JsName("C")
    fun `readTo should interpret the next 2 bytes as a single 11-bit character when the bytes together follow the pattern 110xxxxxx 10xxxxxx`() {
        assertFixedSizedCharactersAreDecodedCorrectly(
            doubleByteInputChars,
            bytesPerChar = 2,
            writeOneChar = { char ->
                add1stOf2Bytes(char)
                add2ndOf2Bytes(char)
            })
    }

    @Test
    @JsName("D")
    fun `readTo should interpret the next 3 bytes as a single 16-bit character when the bytes together follow the pattern 1110xxxx 10xxxxxx 10xxxxxx`() {
        assertFixedSizedCharactersAreDecodedCorrectly(
            doubleByteInputChars,
            bytesPerChar = 3,
            writeOneChar = { char ->
                add1stOf3Bytes(char)
                add2ndOf3Bytes(char)
                add3rdOf3Bytes(char)
            })
    }

    @Test
    @JsName("E")
    fun `readTo should correctly decode sequences that may contain characters of differing lengths in bytes`() {
        for (charArray in sampleStrings) {
            val mutf8Length = charArray.mutf8Length
            require(mutf8Length <= 65535) { "sample array is too long to be encoded; mutf8Length=$mutf8Length" }

            assertAllMethodsProduce(
                expected = charArray,
                mutf8Length = charArray.mutf8Length.toInt(),
                createSource = {
                    Source {
                        for (char in charArray)
                            addAllBytesOf(char)
                    }
                })
        }
    }

    /**
     * Asserts that each of [Mutf8Source]'s `readTo*()` methods can reproduce each original character, both individually
     * and as one sequence of all the characters.
     *
     * The methods that will be tested are:
     * - [Mutf8Source.readToArray]
     * - [Mutf8Source.readToString]
     * - [Mutf8Source.readToAppendable]
     *
     * @param[charsToTest] The characters that will be individually tested & tested together as a sequence.
     * @param[bytesPerChar] The number of bytes that [writeOneChar] will add per character. It should always be the same
     * amount.
     * @param[writeOneChar] A function that encodes one of the [charsToTest], given the [Char], as a sequence of bytes
     * for the test to read. The number of bytes written per [Char] should always be the same and equal to
     * [bytesPerChar].
     *
     * @throws[AssertionError] if any `readTo*()` method does not produce the original character when reading its bytes.
     * @throws[Throwable] if any `readTo*()` method unexpectedly throws an exception.
     */
    private inline fun assertFixedSizedCharactersAreDecodedCorrectly(
        charsToTest: Iterable<Char>,
        bytesPerChar: Int,
        crossinline writeOneChar: MutableList<Byte>.(Char) -> Unit
    ) {
        val chars = charsToTest.toList().toCharArray().also { it.shuffle(random = createReproducibleRandom()) }

        // Test each of the characters individually.
        for (char in chars)
            assertAllMethodsProduce(
                expected = charArrayOf(char),
                mutf8Length = bytesPerChar,
                createSource = {
                    Source {
                        writeOneChar(char)
                    }
                })

        // Test them all together as one big sequence.
        assertAllMethodsProduce(
            expected = chars,
            mutf8Length = chars.size * bytesPerChar,
            createSource = {
                Source {
                    for (char in chars)
                        writeOneChar(char)
                }
            })
    }

    /**
     * Asserts that each of [Mutf8Source]'s `readTo*()` methods output the same [expected] sequence of characters for
     * a given input.
     *
     * The methods that will be tested are:
     * - [Mutf8Source.readToArray]
     * - [Mutf8Source.readToString]
     * - [Mutf8Source.readToAppendable]
     *
     * @param[mutf8Length] The value passed to each method for the `mutf8Length` parameter. This should always be valid
     * because the methods are expected not to throw, which they will/should if it's invalid.
     * @param[createSource] A function that returns a new [Mutf8Source] to call one of the methods on. This will be
     * run once for each tested method to ensure that a source's state doesn't interfere with the behaviour of any
     * method. Namely, once one method read's a source's bytes, there will be none or less for the next method to read
     * which otherwise could cause an unintentional exception that's unrelated to the test.
     *
     * @throws[AssertionError] if any `readTo*()` method does not produce a sequence of characters identical to the
     * [expected] one.
     * @throws[Throwable] if any `readTo*()` method unexpectedly throws an exception.
     */
    private inline fun assertAllMethodsProduce(expected: CharArray, mutf8Length: Int, createSource: () -> Mutf8Source) {
        assertContentEquals(expected, actual = createSource().readToArray(mutf8Length))
        assertContentEquals(expected, actual = createSource().readToString(mutf8Length).toCharArray())
        assertContentEquals(expected, actual = StringBuilder().also {
            createSource().readToAppendable(mutf8Length, destination = it)
        }.toString().toCharArray())
    }
}