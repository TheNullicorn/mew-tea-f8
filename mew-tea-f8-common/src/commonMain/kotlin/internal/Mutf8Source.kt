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
        val utfLengthInt = utfLength.toInt()

        // The call to `readBytes` below may cause issues if we try to read `0` bytes, so we can short-circuit here
        // because we know the string will be empty if there's no bytes anyway.
        if (utfLengthInt == 0) return ""

        // Read all the string's bytes at once.
        val bytes = readBytes(amount = utfLength)
        if (bytes.size != utfLengthInt)
            throw Mutf8IOException("$utfLength bytes were requested for the string, but only ${bytes.size} were received")

        // Create a possibly oversized array to hold the string's characters as we read them.
        val chars = CharArray(size = utfLengthInt)

        // Our current indices in the `bytes` and `chars` arrays respectively.
        var b = 0
        var c = 0

        // Chars can use up to 3 bytes, which are held in these variables until their bits are combined into a single
        // Char.
        var byte1: Int
        var byte2: Int
        var byte3: Int

        while (b < utfLengthInt) {
            byte1 = bytes[b++].toInt()
            val mostSigNibble = byte1 and 0xF0 shr 4

            // If the most-significant bit is unset, then it's a plain 7-bit ASCII character.
            if (mostSigNibble < 0x8) {
                chars[c++] = (byte1 and 0x7F).toChar()
                continue
            }

            // If the byte's bits match the pattern `110xxxxx`, it's encoded using two bytes.
            if (mostSigNibble == 0xC || mostSigNibble == 0xD) {
                // Make sure we won't exceed the `utfLength` by reading the second byte.
                if (b == utfLengthInt)
                    throw Mutf8TruncatedCharacterException(charSize = 2, bytesLeft = 0)

                // Read the second byte.
                byte2 = bytes.getSecondaryByteOfChar(b++, charSize = { 2 }, byteOffset = { 1 })

                chars[c++] = ((byte1 and 0x1F shl 6) or (byte2 and 0x3F)).toChar()
                continue
            }

            // If the byte's bits match the pattern `1110xxxx`, it's encoded using three bytes.
            if (mostSigNibble == 0xE) {
                // Make sure we won't exceed the `utfLength` by reading the second byte.
                if (b == utfLengthInt)
                    throw Mutf8TruncatedCharacterException(charSize = 3, bytesLeft = 1)

                // Make sure we won't exceed the `utfLength` by reading the third byte.
                if (b == utfLengthInt - 1)
                    throw Mutf8TruncatedCharacterException(charSize = 2, bytesLeft = 0)

                // Read the second & third bytes.
                byte2 = bytes.getSecondaryByteOfChar(b++, charSize = { 3 }, byteOffset = { 1 })
                byte3 = bytes.getSecondaryByteOfChar(b++, charSize = { 3 }, byteOffset = { 2 })

                chars[c++] = ((byte1 and 0x0F shl 12) or (byte2 and 0x3F shl 6) or (byte3 and 0x3F)).toChar()
                continue
            }

            // The first byte matches either the pattern `1111xxxx`, which is not valid for any Modified UTF-8 byte,
            // or the pattern `10xxxxxx`, which is only valid for the 2nd or 3rd bytes of characters.
            throw Mutf8MalformedPrimaryByteException(byte1)
        }

        return chars.concatToString(startIndex = 0, endIndex = c)
    }
}

/**
 * Retrieves & validates the second or third byte of a 2-byte or 3-byte character.
 *
 * @receiver The array to get the byte from.
 * @param[index] The index of the byte in the receiving array.
 * @param[charSize] The total number of bytes being used to encode the character in question.
 *
 * This is only used in the message of an exception, if one is thrown.
 * @param[byteOffset] The byte's absolute offset from the first byte of the character in question. For example, `1` if
 * this is the second byte of the character, or `2` if it's the third.
 *
 * This is only used in the message of an exception, if one is thrown.
 * @return the value of the byte at the given [index] in the array.
 *
 * @throws[Mutf8MalformedSecondaryByteException] if the second or third bytes of a 2-byte or 3-byte character don't
 * have their most-significant bit set (`1`), and their second-most-significant bit unset (`0`). In other words,
 * the byte does not match the expected pattern, `10xx xxxx`, where each `x` can be either `1` or `0`.
 */
@InternalMutf8Api
private inline fun ByteArray.getSecondaryByteOfChar(
    index: Int,
    charSize: () -> Int,
    byteOffset: () -> Int
): Int {
    val byte = this[index].toInt()

    // The 2nd and 3rd bytes of 2-byte and 3-byte characters always have `10` as their two most-significant bits. If
    // not, the character is considered malformed, so we throw an exception.
    if (byte and 0xC0 != 0x80)
        throw Mutf8MalformedSecondaryByteException(byte, charSize(), byteOffset())

    return byte
}