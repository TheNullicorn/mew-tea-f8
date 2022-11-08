package me.nullicorn.mewteaf8.Mutf8Sink

import me.nullicorn.mewteaf8.*
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.math.min as minInt

abstract class WriteCharactersSuccessfullyTests<Sink : Mutf8Sink, Destination> : Mutf8SinkTests<Sink, Destination>() {

    @Test
    @JsName("A")
    fun `writeFrom should not write any bytes if the sequence or array is empty or if the size of the range is 0`() {
        // Test an array/string that is entirely empty itself.
        assertAllMethodsProduceBytes(chars = CharArray(size = 0), expectedBytes = emptyList())

        // Test strings that do have characters, but using a `startIndex`/`endIndex` that are the same, thus selecting
        // `0` characters to be encoded.
        for (charArray in sampleStrings.filter { it.isNotEmpty() }) {
            val indicesToTest = buildSet {
                // Always test the first index.
                add(0)

                // Always test the last index.
                add(charArray.lastIndex)

                // Test ~20 random indices over the entire array. Less may be added if they're already in the set.
                var i = 0
                addRandomlyWhile({ i < minInt(charArray.size, 20) }, { random ->
                    i++
                    random.nextInt(until = charArray.size)
                })
            }

            for (index in indicesToTest)
                assertAllMethodsProduceBytes(
                    charArray,
                    expectedBytes = emptyList(),
                    startIndex = index,
                    endIndex = index
                )
        }
    }

    @Test
    @JsName("B")
    fun `writeFrom should encode characters from U+0001 until U+0080 using 1 byte`() {
        assertSpecificCharactersAreEncodedCorrectly(singleByteOutputChars)
    }

    @Test
    @JsName("C")
    fun `writeFrom should encode U+0000 and characters from U+0080 until U+0800 using 2 bytes`() {
        assertSpecificCharactersAreEncodedCorrectly(doubleByteOutputChars)
    }

    @Test
    @JsName("D")
    fun `writeFrom should encode characters from U+0800 until U+FFFF using 3 bytes`() {
        assertSpecificCharactersAreEncodedCorrectly(tripleByteOutputChars)
    }

    @Test
    @JsName("E")
    fun `writeFrom should correctly encode entire sequences of characters whose lengths in bytes may vary`() {
        for (charArray in sampleStrings)
            assertAllMethodsProduceBytes(
                chars = charArray,
                expectedBytes = buildList {
                    for (char in charArray)
                        addAllBytesOf(char)
                })
    }

    @Test
    @JsName("F")
    fun `writeFrom should only encode characters within the specified range if one is specified`() {
        for (charArray in sampleStrings.filter { it.size > 1 }) {
            val startAndEndIndices = buildSet {
                addRandomlyWhile({ size < 20 }, { random ->
                    // Start from `1` so that the range is never the entire array/sequence. Other tests already cover
                    // that; this one is specifically testing smaller ranges within the arrays/sequences.
                    val rangeSize = random.nextInt(from = 1, until = charArray.size)

                    // Choose a random offset for the range to start at without putting the `endIndex` out of bounds.
                    val startIndex = random.nextInt(until = charArray.size - rangeSize)
                    val endIndex = startIndex + rangeSize

                    return@addRandomlyWhile startIndex to endIndex
                })
            }

            for ((startIndex, endIndex) in startAndEndIndices)
                assertAllMethodsProduceBytes(
                    chars = charArray,
                    startIndex = startIndex,
                    endIndex = endIndex,
                    expectedBytes = buildList {
                        for (i in startIndex until endIndex)
                            addAllBytesOf(charArray[i])
                    })
        }
    }

    /**
     * Asserts that all the supplied [charsToTest] are encoded correctly by each of [Mutf8Sink]'s methods, both on their
     * own and as one big sequence.
     */
    private fun assertSpecificCharactersAreEncodedCorrectly(charsToTest: Iterable<Char>) {
        val chars = charsToTest
            .toList()
            .toCharArray()
            .apply {
                shuffle(random = createReproducibleRandom())
            }

        for (char in chars)
            assertAllMethodsProduceBytes(
                chars = charArrayOf(char),
                expectedBytes = buildList {
                    addAllBytesOf(char)
                })

        assertAllMethodsProduceBytes(chars, expectedBytes = buildList {
            for (char in chars)
                addAllBytesOf(char)
        })
    }

    /**
     * Asserts that all methods of [Mutf8Sink] produce the [expectedBytes] when supplied with the [chars], [startIndex],
     * and [endIndex].
     *
     * Overloads that encode the entire sequence or array, rather than just a range, don't have [startIndex] or
     * [endIndex] parameters. For those, a copy of the characters in that range is taken from [chars] and passed to the
     * method, rather than the entire [chars]. That way, it should produce the same output as the range-based methods
     * as if they were supplied with that range.
     */
    private fun assertAllMethodsProduceBytes(
        chars: CharArray,
        expectedBytes: List<Byte>,
        startIndex: Int = 0,
        endIndex: Int = chars.size
    ) {
        fun testWriterMethod(write: Mutf8Sink.() -> Unit) {
            val sink = Sink()
            sink.write()
            assertContentEquals(expectedBytes.toByteArray(), actual = sink.bytes)
        }

        val string = chars.concatToString()

        // Test the overload that writes the entire array/sequence and doesn't take a range.
        if (startIndex == 0 && endIndex == chars.size) {
            // If the supplied range is already the same as the array/sequence's, then pass the entire thing.
            testWriterMethod { writeFromArray(chars) }
            testWriterMethod { writeFromSequence(string) }
        } else {
            // Otherwise, create a new array & sequence that's just the Chars in that range and test it with them.
            val charsInRange = chars.copyOfRange(startIndex, endIndex)
            testWriterMethod { writeFromArray(charsInRange) }
            testWriterMethod { writeFromSequence(charsInRange.concatToString()) }
        }

        // Test the overloads of `writeFromArray()` that take a range, both as an `IntRange` and as a separate
        // `startIndex` & `endIndex`.
        testWriterMethod { writeFromArray(chars, startIndex, endIndex) }
        testWriterMethod { writeFromArray(chars, range = startIndex until endIndex) }

        // Test the overloads of `writeFromSequence()` that take a range, both as an `IntRange` and as a separate
        // `startIndex` & `endIndex`.
        testWriterMethod { writeFromSequence(string, startIndex, endIndex) }
        testWriterMethod { writeFromSequence(string, range = startIndex until endIndex) }
    }
}