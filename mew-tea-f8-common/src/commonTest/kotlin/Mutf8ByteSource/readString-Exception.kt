@file:Suppress("PackageName")

package me.nullicorn.mewteaf8.Mutf8ByteSource

import me.nullicorn.mewteaf8.*
import me.nullicorn.mewteaf8.internal.*
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertFailsWith

@OptIn(InternalMutf8Api::class)
class ReadStringExceptionsTests {

    @Test
    @JsName("A")
    fun `readString should propogate Mutf8EOFExceptions thrown by readBytes`() {
        for (totalBytes in 0..20)
            for (extraBytesToTryReading in 1..5) {
                val source = buildTestSource {
                    for (i in 0 until totalBytes)
                        add1stOf1Bytes('A')
                }

                // Read more byte that we actually included, which should cause the exception to be thrown.
                assertFailsWith<Mutf8EOFException> {
                    source.readBytes(amount = (totalBytes + extraBytesToTryReading).toUShort())
                }
            }
    }

    @Test
    @JsName("B")
    fun `readString should propogate Mutf8IOExceptions thrown by readBytes`() {
        val source = object : Mutf8Source {

            override fun readBytes(amount: UShort): ByteArray {
                throw Mutf8IOException("Test; this should NOT be caught by readString()")
            }

            override fun readLength(): UShort {
                throw UnsupportedOperationException("This method is not being tested")
            }
        }

        assertFailsWith<Mutf8IOException> {
            source.readString(utfLength = 0u)
        }
    }

    @Test
    @JsName("C")
    fun `readString should throw CharacterStartedTooLateException if a 2-byte character is started on the last byte`() {
        for (char in doubleByteSamples)
            for (include2ndByteOutOfBounds in setOf(false, true)) {
                val source = buildTestSource {
                    add1stOf2Bytes(char)

                    // Test that it strictly follows the supplied `utfLength`, even though the 2nd byte is there, but
                    // outside the requested range of bytes.
                    if (include2ndByteOutOfBounds)
                        add2ndOf2Bytes(char)
                }

                assertFailsWith<Mutf8TruncatedCharacterException> {
                    source.readString(utfLength = 1u)
                }
            }
    }

    @Test
    @JsName("D")
    fun `readString should throw CharacterStartedTooLateException if a 3-byte character is started on the last or second-to-last byte`() {
        // For each sample character, alternate between omitting only the 3rd byte & omitting both the 2nd and 3rd. For
        // each of those, also include a test where the missing bytes are technically there, but are outside the limit
        // specified by `utfLength`, that way we can be sure that the function doesn't try to reach for bytes outside
        // the `utfLength` to complete a character.
        for (char in tripleByteSamples)
            for (bytesToOmit in setOf(1, 2))
                for (includeOtherBytesOutOfBounds in setOf(false, true)) {
                    val source = buildTestSource {
                        add1stOf3Bytes(char)

                        if (bytesToOmit > 1 || includeOtherBytesOutOfBounds)
                            add2ndOf3Bytes(char)

                        if (bytesToOmit > 2 || includeOtherBytesOutOfBounds)
                            add3rdOf3Bytes(char)
                    }

                    // If `includeOtherBytesOutOfBounds` is `true`, then `size` will be larger than the # of bytes we
                    // intend to read. To get only the # we'll be reading, we subtract it from 3, the total # of bytes
                    // per character, in those cases.
                    val utfLength = if (includeOtherBytesOutOfBounds) 3 - bytesToOmit else source.size

                    assertFailsWith<Mutf8TruncatedCharacterException> {
                        source.readString(utfLength = utfLength.toUShort())
                    }
                }
    }

    @Test
    @JsName("E")
    fun `readString should throw MalformedPrimaryByteException if a character's first byte has bits that match the pattern 1111xxxx`() {
        for (char in singleByteSamples) {
            val source = buildTestSource {
                // Set all 4 most-significant bits in the byte.
                add((char.code and 0x0F or 0xF0).toByte())
            }
            assertFailsWith<Mutf8MalformedPrimaryByteException> {
                source.readString(utfLength = 1u)
            }
        }

        for (char in doubleByteSamples) {
            val source = buildTestSource {
                // Set all 4 most-significant bits in the first byte, but write the second one normally.
                add((char.code shr 6 and 0x0F or 0xF0).toByte())
                add2ndOf2Bytes(char)
            }
            assertFailsWith<Mutf8MalformedPrimaryByteException> {
                source.readString(utfLength = 2u)
            }
        }

        for (char in tripleByteSamples) {
            val source = buildTestSource {
                // Set all 4 most-significant bits in the first byte, but write the second and third ones normally.
                add((char.code shr 12 and 0x0F or 0xF0).toByte())
                add2ndOf3Bytes(char)
                add3rdOf3Bytes(char)
            }
            assertFailsWith<Mutf8MalformedPrimaryByteException> {
                source.readString(utfLength = 3u)
            }
        }
    }

    @Test
    @JsName("F")
    fun `readString should throw MalformedSecondaryByteException if the second or third bytes of a character don't have bits matching the pattern 10xxxxxx`() {
        // All combos of the 2 most-significant bits that aren't allowed on secondary bytes.
        val badBitPairs = (0b00..0b11) - 0b10

        for (char in doubleByteSamples)
            for (badBitPair in badBitPairs) {
                val source = buildTestSource {
                    add1stOf2Bytes(char)
                    add((char.code and 0x3F or (badBitPair shl 6)).toByte())
                }
                assertFailsWith<Mutf8MalformedSecondaryByteException> {
                    source.readString(utfLength = 2u)
                }
            }

        for (char in tripleByteSamples)
            for (badByte in setOf(2, 3))
                for (badBitPair in badBitPairs) {
                    val source = buildTestSource {
                        add1stOf3Bytes(char)

                        if (badByte == 2) {
                            add((char.code shr 6 and 0x3F or (badBitPair shl 6)).toByte())
                            add3rdOf3Bytes(char)
                        } else {
                            add2ndOf3Bytes(char)
                            add((char.code and 0x3F or (badBitPair shl 6)).toByte())
                        }
                    }
                    assertFailsWith<Mutf8MalformedSecondaryByteException> {
                        source.readString(utfLength = 3u)
                    }
                }
    }
}