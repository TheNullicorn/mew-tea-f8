@file:JvmSynthetic

package me.nullicorn.mewteaf8.internal

import kotlin.jvm.JvmSynthetic

/**
 * A sequence of bytes that can be read in bulk, specifically for the purpose of reading Modified UTF-8 strings.
 */
@InternalMutf8Api
interface Mutf8Source {

    /**
     * Reads a specific [amount] of bytes from the source and returns them in order as a [ByteArray].
     *
     * @param[amount] The number of bytes to read.
     * @return an array, whose size equals [amount], of the next bytes that appear in the source in order.
     *
     * @throws[Mutf8IOException] if the source does not have enough bytes left in it to supply the requested [amount].
     * @throws[Mutf8IOException] if an I/O related issue occurs while trying to read those bytes.
     */
    fun readBytes(amount: UShort): ByteArray

    /**
     * Reads the length, or "UTF Length" as Java refers to it, of a Modified UTF-8 string from the source.
     *
     * For a simple stream, this should behave as if two bytes were read from the source and combined in big-endian
     * order. In code, that would be:
     * ```kotlin
     * val bytes: ByteArray = readBytes(amount = 2u)
     * val utfLength: UShort = ((bytes[0].toInt() and 0xFF shl 8) or (bytes[1].toInt() and 0xFF)).toUShort()
     * return utfLength
     * ```
     *
     * @return the `length` value that appeared next in the source.
     *
     * @throws[Mutf8IOException] if the source does not have enough bytes left in it to read the length.
     * @throws[Mutf8IOException] if an I/O related issue occurs while trying to read those bytes.
     */
    fun readLength(): UShort

    /**
     * Reads a Modified UTF-8 string from the source.
     *
     * @param[utfLength] The number of bytes that the string is represented using.
     *
     * This is also the number of bytes that will be read from the source, assuming no exception is thrown.
     * @return the string that was encoded in those bytes.
     *
     * @throws[Mutf8EOFException] if the source does not have enough bytes left in it to read the amount
     * specified by [utfLength].
     * @throws[Mutf8IOException] if an I/O related issue occurs while trying to read those bytes.
     * @throws[Mutf8TruncatedCharacterException] if a 2-byte character starts on the last byte of the string, meaning
     * the second byte doesn't actually exist.
     * @throws[Mutf8TruncatedCharacterException] if a 3-byte character starts on the last or second-to-last bytes of
     * the string, meaning the second and/or third bytes don't actually exist.
     * @throws[Mutf8MalformedPrimaryByteException] if the first byte of a character has all 4 of its most-significant
     * bits set, which is not expected of any byte in a Modified UTF-8 string. In other words, the bytes bits match the
     * pattern `1111 xxxx`, where each `x` can be either `1` or `0`.
     * @throws[Mutf8MalformedPrimaryByteException] if the first byte of a character has its most-significant bit set
     * (`1`) and their second-most-significant bit unset (`0`). In other words, the bytes matches the pattern
     * `10xx xxxx` (where each `x` can be either `1` or `0`), which is only expected for the 2nd and 3rd bytes of
     * multi-byte characters.
     * @throws[Mutf8MalformedSecondaryByteException] if the second or third bytes of a 2-byte or 3-byte character don't
     * have their most-significant bit set (`1`), and their second-most-significant bit unset (`0`). In other words,
     * the byte does not match the expected pattern, `10xx xxxx`, where each `x` can be either `1` or `0`.
     */
    fun readString(utfLength: UShort): String {
        val mutf8LengthInt = utfLength.toInt()
        if (mutf8LengthInt == 0) return ""

        val bytes = readBytes(amount = utfLength)
        var b = 0

        if (bytes.size < mutf8LengthInt)
            throw Mutf8EOFException("$bytes bytes were read from the stream instead of the $utfLength expected")

        val chars = CharArray(size = mutf8LengthInt)
        var c = 0

        // Assume all characters are 7-bit ASCII (1 byte each) until we find one that isn't. At that point, the second
        // loop takes over, handling 2-byte and 3-byte characters as well. This optimization comes from Java's own
        // `DataInput.readUTF()`.
        do {
            val byte1 = bytes[b].toInt() and 0xFF
            if (byte1 and 0x80 == 0x80) break

            chars[c++] = byte1.toChar()
            b++
        } while (b < mutf8LengthInt)

        // Same gist as the previous loop, but now we have to account for characters that are encoded using 2 and 3
        // bytes as well. If all the characters are ASCII, then this is skipped because the previous loop read them all.
        while (b < mutf8LengthInt) {
            val byte1 = bytes[b++].toInt()

            // Using the 4 most-significant bits of the first byte, we can determine how many bytes long the character
            // is.
            when (byte1 and 0xF0 shr 4) {
                // If the most-significant bit is a `0`, then it's only the 1 byte.
                0, 1, 2, 3, 4, 5, 6, 7 -> {
                    chars[c++] = byte1.toChar()
                }

                // If the bits are `110x`, then it's 2 bytes.
                12, 13 -> {
                    // If `byte1` was the last byte, throw because there's no `byte2` for us to read.
                    if (b + 1 > mutf8LengthInt)
                        throw Mutf8TruncatedCharacterException(charSize = 2, bytesLeft = 0)

                    // Read the second byte and ensure its bits match the pattern `10xxxxxx`.
                    val byte2 = bytes[b++].toInt()
                    if (byte2 and 0xC0 != 0x80)
                        throw Mutf8MalformedSecondaryByteException(byte2, charSize = 2, byteOffset = 1)

                    // Combine both bytes into a single Char.
                    chars[c++] = ((byte1 and 0x1F shl 6) or (byte2 and 0x3F)).toChar()
                }

                // If the bits are `1110`, then it's 3 bits.
                14 -> {
                    // If `byte1` was the last byte, throw because there's no `byte2` for us to read. If `byte2` will be
                    // the last byte, throw because there's no `byte3` for us to read.
                    if (b + 2 > mutf8LengthInt)
                        throw Mutf8TruncatedCharacterException(charSize = 2, bytesLeft = mutf8LengthInt - b)

                    // Read the second byte and ensure its bits match the pattern `10xxxxxx`.
                    val byte2 = bytes[b++].toInt()
                    if (byte2 and 0xC0 != 0x80)
                        throw Mutf8MalformedSecondaryByteException(byte2, charSize = 3, byteOffset = 1)

                    // Read the third byte and ensure its bits match the pattern `10xxxxxx`.
                    val byte3 = bytes[b++].toInt()
                    if (byte3 and 0xC0 != 0x80)
                        throw Mutf8MalformedSecondaryByteException(byte2, charSize = 3, byteOffset = 2)

                    // Combine all 3 bytes into a single Char.
                    chars[c++] = ((byte1 and 0x0F shl 12) or (byte2 and 0x3F shl 6) or (byte3 and 0x3F)).toChar()
                }

                // If the bits are `1111` or `10xx`, then we throw because neither is allowed for a char's first byte.
                else -> throw Mutf8MalformedPrimaryByteException(byte1)
            }
        }

        // `c` may be less than `chars.size` if the string had any multibyte characters.
        return chars.concatToString(endIndex = c)
    }
}