package me.nullicorn.mewteaf8.Mutf8Source

import me.nullicorn.mewteaf8.*
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

abstract class ReadCharactersExceptionallyTests<Source : Mutf8Source> : Mutf8SourceTests<Source>() {

    @Test
    @JsName("A")
    fun `readTo should throw an IllegalArgumentException if mutf8Length is negative`() {
        for (charArray in sampleStrings)
            for (mutf8Length in (-16..-1) + Int.MIN_VALUE)
                assertAllMethodsFailWith<IllegalArgumentException>(
                    mutf8Length,
                    createSource = {
                        Source {
                            charArray.forEach { addAllBytesOf(it) }
                        }
                    })
    }

    @Test
    @JsName("B")
    fun `readTo should throw an IllegalArgumentException if mutf8Length exceeds 65535`() {
        val maxMutf8Length = 65535

        // Exactly that many bytes is fine, so no exception should be thrown.
        val singleCharByte: Byte = buildList { add1stOf1Bytes(singleByteOutputChars.first()) }.first()
        val almostTooManyBytes: List<Byte> = List(size = maxMutf8Length, init = { singleCharByte })
        assertAllMethodsSucceed(mutf8Length = maxMutf8Length, createSource = {
            Source(input = almostTooManyBytes)
        })

        // Test the next highest `mutf8Length` after the max, plus a few more, plus `Int.MAX_VALUE`. All of these should
        // result in an `IllegalArgumentException` for being too high.
        for (charArray in sampleStrings)
            for (mutf8Length in (maxMutf8Length + 1..maxMutf8Length + 16) + Int.MAX_VALUE)
                assertAllMethodsFailWith<IllegalArgumentException>(mutf8Length, createSource = { Source() })
    }

    @Test
    @JsName("C")
    fun `readTo should propagate EOFExceptions thrown by readBytes`() {
        assertExceptionIsPropagatedFromReadBytes(::EOFException)
    }

    @Test
    @JsName("D")
    fun `readTo should propagate IOExceptions thrown by readBytes`() {
        assertExceptionIsPropagatedFromReadBytes(::IOException)
    }

    @Test
    @JsName("E")
    fun `readTo should throw an EOFException if readBytes returns less bytes than requested`() {
        // Create a `Mutf8Source` that always returns 1 fewer byte than requested when read from.
        val singleCharByte: Byte = buildList { add1stOf1Bytes(singleByteOutputChars.first()) }.first()
        val createSourceThatReturnsNotEnoughBytes: () -> Mutf8Source = {
            object : Mutf8Source() {
                override fun readBytes(amount: Int): ByteArray {
                    assertTrue(amount != 0, "Can't return any fewer bytes than 0 for the test")
                    return ByteArray(size = amount - 1, init = { singleCharByte })
                }

                override fun readLength() = throw UnsupportedOperationException("Not the method being tested")
            }
        }

        // Test some sample values, plus the highest allowed `mutf8Length`. The only in-range number we can't test here
        // is `0` because `readBytes()` would need to return an array with a negative size to return less than `0`,
        // which is not possible.
        for (mutf8Length in (1..16) + 65535)
            assertAllMethodsFailWith<EOFException>(mutf8Length, createSourceThatReturnsNotEnoughBytes)
    }

    @Test
    @JsName("F")
    fun `readTo should throw an IOException if readBytes returns more bytes than requested`() {
        // Create a `Mutf8Source` that always returns 1 more byte than requested when read from.
        val singleCharByte: Byte = buildList { add1stOf1Bytes(singleByteOutputChars.first()) }.first()
        val createSourceThatReturnsTooManyBytes: () -> Mutf8Source = {
            object : Mutf8Source() {
                override fun readBytes(amount: Int): ByteArray {
                    assertTrue(amount != Int.MAX_VALUE, "Can't return too many bytes for test without overflowing")
                    return ByteArray(size = amount + 1, init = { singleCharByte })
                }

                override fun readLength() = throw UnsupportedOperationException("Not the method being tested")
            }
        }

        // Test some sample values, plus the highest allowed `mutf8Length`. The only in-range number we can't test here
        // is `0` because `readBytes()` may not even be called to give the test a chance to return too many bytes.
        for (mutf8Length in (1..16) + 65535)
            assertAllMethodsFailWith<IOException>(mutf8Length, createSourceThatReturnsTooManyBytes)
    }

    @Test
    @JsName("G")
    fun `readTo should throw a UTFDataFormatException if a 2-byte character starts on the last byte of a string`() {
        for (char in doubleByteInputChars)
            for (include2ndByteOutOfBounds in setOf(false, true))
                assertAllMethodsFailWith<UTFDataFormatException>(
                    mutf8Length = 1,
                    createSource = {
                        Source {
                            add1stOf2Bytes(char)

                            // Test that it `readTo*()` follows the supplied `mutf8Length`. It shouldn't reach outside
                            // the specified `mutf8Length` even if the rest of the incomplete character is there.
                            if (include2ndByteOutOfBounds)
                                add2ndOf2Bytes(char)
                        }
                    })
    }

    @Test
    @JsName("H")
    fun `readTo should throw a UTFDataFormatException if a 3-byte character starts on the last or second-to-last byte of a string`() {
        // For each sample character, alternate between omitting only the 3rd byte & omitting both the 2nd and 3rd. For
        // each of those, also include a test where the missing bytes are technically there, but are outside the limit
        // specified by `mutf8Length`, that way we can be sure that the function doesn't try to reach for bytes outside
        // the `mutf8Length` to complete a character.
        for (char in tripleByteInputChars)
            for (bytesToOmit in setOf(1, 2))
                for (includeOtherBytesOutOfBounds in setOf(false, true))
                    assertAllMethodsFailWith<UTFDataFormatException>(
                        mutf8Length = 3 - bytesToOmit,
                        createSource = {
                            Source {
                                add1stOf3Bytes(char)

                                if (bytesToOmit >= 1 || includeOtherBytesOutOfBounds)
                                    add2ndOf3Bytes(char)

                                if (bytesToOmit == 2 || includeOtherBytesOutOfBounds)
                                    add3rdOf3Bytes(char)
                            }
                        }
                    )
    }

    @Test
    @JsName("I")
    fun `readTo should throw a UTFDataFormatException if the first byte of a characters has bits matching the pattern 1111xxxx or 10xxxxxx`() {
        // We'll be running the same test with each of the 3 char ranges (1-byte, 2-byte, 3-byte) but they're all a
        // little different. Instead of making separate tests, we use a local function here to share those assertions.
        fun testByCorruptingFirstByte(
            charsToTest: Iterable<Char>,
            bytesPerChar: Int,
            addCorrectBytesOfChar: MutableList<Byte>.(Char) -> Unit
        ) {
            for (char in charsToTest) {
                // Assert that it fails when the first byte's bits are `1111xxxx`, which is never allowed.
                assertAllMethodsFailWith<UTFDataFormatException>(
                    mutf8Length = bytesPerChar,
                    createSource = {
                        Source {
                            // Write the character's correct bytes.
                            addCorrectBytesOfChar(char)
                            // Edit the first byte so that its bits match the aforementioned pattern.
                            this[0] = (this[0].toInt() or 0xF0).toByte()
                        }
                    })

                // Assert that it fails when the first byte's bits are `10xxxxxx`, which is only meant for the 2nd & 3rd
                // bytes.
                assertAllMethodsFailWith<UTFDataFormatException>(
                    mutf8Length = bytesPerChar,
                    createSource = {
                        Source {
                            // Write the character's correct bytes.
                            addCorrectBytesOfChar(char)
                            // Edit the first byte so that its bits match the aforementioned pattern.
                            this[0] = (this[0].toInt() and 0x3F or 0x80).toByte()
                        }
                    })
            }
        }

        // Test each of the 3 character ranges with their respective samples & encodings.

        testByCorruptingFirstByte(bytesPerChar = 1, charsToTest = singleByteInputChars) {
            add1stOf1Bytes(it)
        }

        testByCorruptingFirstByte(bytesPerChar = 2, charsToTest = doubleByteInputChars) {
            add1stOf2Bytes(it)
            add2ndOf2Bytes(it)
        }

        testByCorruptingFirstByte(bytesPerChar = 3, charsToTest = tripleByteInputChars) {
            add1stOf3Bytes(it)
            add2ndOf3Bytes(it)
            add3rdOf3Bytes(it)
        }
    }

    @Test
    @JsName("J")
    fun `readTo should throw a UTFDataFormatException if the second or third bytes of a character don't have bits matching the pattern 10xxxxxx`() {
        // All combos of the 2 most-significant bits that aren't allowed on secondary bytes. The `- 0b10` is what
        // excludes the correct pattern from our list of bad patterns.
        val badBitPairs = (0b00..0b11) - 0b10

        for (char in doubleByteInputChars)
            for (badBitPair in badBitPairs)
                assertAllMethodsFailWith<UTFDataFormatException>(
                    mutf8Length = 2,
                    createSource = {
                        Source {
                            add1stOf2Bytes(char)
                            add2ndOf2Bytes(char)

                            // Replace the correct bits with our malformed ones.
                            this[1] = (this[1].toInt() and 0x3F or (badBitPair shl 6)).toByte()
                        }
                    })

        for (char in tripleByteInputChars)
            for (badByte in setOf(2, 3))
                for (badBitPair in badBitPairs)
                    assertAllMethodsFailWith<UTFDataFormatException>(
                        mutf8Length = 3,
                        createSource = {
                            Source {
                                add1stOf3Bytes(char)
                                add2ndOf3Bytes(char)
                                add3rdOf3Bytes(char)

                                // Replace the correct bits with our malformed ones in whichever byte we're corrupting.
                                this[badByte - 1] = (this[badByte - 1].toInt() and 0x3F or (badBitPair shl 6)).toByte()
                            }
                        })
    }

    /**
     * Asserts that when [readBytes][Mutf8Source.readBytes] throws a specific type of exception, that same exception
     * makes its way back to the caller of [each][assertAllMethodsFailWith] `readTo*()` method rather than being caught
     * and handled by it.
     *
     * @param[E] The [Exception] class that [readBytes][Mutf8Source.readBytes] will throw for this test.
     * @param[exceptionConstructor] A reference to [E]'s constructor, which accepts a single `message: String` as its
     * parameter. For example, `::IOException`.
     *
     * @throws[AssertionError] if any `readTo*()` method catches the exception or throws a different type of exception.
     */
    private inline fun <reified E : Exception> assertExceptionIsPropagatedFromReadBytes(crossinline exceptionConstructor: (message: String) -> E) {
        // Create a `Mutf8Source` that always throws the exception `E` when read from.
        val createSourceThatAlwaysThrows: () -> Mutf8Source = {
            object : Mutf8Source() {
                override fun readBytes(amount: Int) =
                    throw exceptionConstructor("This should reach the test; readTo*() should not catch or handle it")

                override fun readLength() = throw UnsupportedOperationException("Not the method being tested")
            }
        }

        // `mutf8Length` shouldn't matter here, so we just run it with a few sample values. It just can't be `0` for
        // this test because then `readBytes()` doesn't need to be called & the exception will never be thrown.
        for (mutf8Length in 1..16)
            assertAllMethodsFailWith<E>(mutf8Length, createSourceThatAlwaysThrows)
    }

    /**
     * Asserts that each of [Mutf8Source]'s `readTo*()` methods throw a specific type of exception.
     *
     * The methods that will be tested are:
     * - [Mutf8Source.readToArray]
     * - [Mutf8Source.readToString]
     * - [Mutf8Source.readToAppendable]
     *
     * @param[mutf8Length] The value passed to each method for the `mutf8Length` parameter. This value is allowed to be
     * invalid if the source's response to that is what's being tested with this assertion.
     * @param[createSource] A function that returns a new [Mutf8Source] to call one of the methods on. This will be
     * run once for each tested method to ensure that a source's state doesn't interfere with the behaviour of any
     * method.
     *
     * @throws[AssertionError] if any of the tested methods complete without throwing [E].
     */
    private inline fun <reified E : Exception> assertAllMethodsFailWith(
        mutf8Length: Int,
        crossinline createSource: () -> Mutf8Source
    ) {
        createSource().run { assertFailsWith<E> { readToArray(mutf8Length) } }
        createSource().run { assertFailsWith<E> { readToString(mutf8Length) } }
        createSource().run { assertFailsWith<E> { readToAppendable(mutf8Length, destination = StringBuilder()) } }
    }

    /**
     * Asserts that each of [Mutf8Source]'s `readTo*()` methods complete without throwing any exceptions.
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
     * @throws[AssertionError] if any of the tested methods throw an exception, or if [createSource] throws its own
     * [AssertionError] for whatever reason.
     */
    private inline fun assertAllMethodsSucceed(mutf8Length: Int, crossinline createSource: () -> Mutf8Source) {
        try {
            createSource().readToArray(mutf8Length)
            createSource().readToString(mutf8Length)
            createSource().readToAppendable(mutf8Length, destination = StringBuilder())

        } catch (cause: AssertionError) {
            // If it's already an `AssertionError` for whatever reason, don't wrap it.
            throw cause

        } catch (cause: Throwable) {
            // Otherwise, print the stack & throw an `AssertionError` with a little context to mark the test as failed.
            cause.printStackTrace()
            throw AssertionError("Expected no exception to be thrown, but ${cause::class.simpleName} was")
        }
    }
}