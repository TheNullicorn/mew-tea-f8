package me.nullicorn.mewteaf8

/**
 * Something that Modified UTF-8 data can be read from.
 *
 * For convenience in various scenarios, said data can be accessed in several ways:
 * - [readToAppendable]: The characters are added to an existing [Appendable], such as a [StringBuilder]
 * - [readToString]: The characters are read and returned as a [String]
 * - [readToArray]: The characters are read and returned as a [CharArray]
 *
 * All the aforementioned methods require the `mutf8Length` to be known in advance, which is the total number of bytes
 * that the characters are encoded in. In standard Modified UTF-8 data, the `mutf8Length` is encoded as an unsigned,
 * big-endian, 16-bit integer, and is encoded immediately before the first character's bytes, if any. Otherwise, its
 * 2 bytes are both `0`, and no character bytes follow them.
 */
abstract class Mutf8Source {

    /**
     * Reads a specific [amount] of bytes from the underlying source of data, and returns those bytes, in order, as a
     * [ByteArray].
     *
     * @param[amount] The number of bytes to read, and the [size][ByteArray.size] of the returned [ByteArray].
     * @return an array of all the bytes that were read from the source, in order. Its [size][ByteArray.size] is equal
     * to [amount].
     *
     * @throws[EOFException] if there are fewer bytes left in the source than expected by the [amount].
     * @throws[IOException] if the underlying source of bytes cannot be accessed.
     */
    @Throws(IOException::class, EOFException::class)
    protected abstract fun readBytes(amount: Int): ByteArray

    /**
     * Reads a Modified UTF-8 string from the source and [appends][Appendable.append] each of its characters, in order,
     * to an [Appendable] [destination], such as a [StringBuilder].
     *
     * This method does not read the 2-byte "UTF length" that comes before standard Modified UTF-8 strings. That must be
     * read separately (if applicable) and passed to this method via the [mutf8Length] parameter.
     *
     * @param[mutf8Length] The number of bytes used to encode the string. This must be at least `0` and at most
     * `65535` ([UShort.MAX_VALUE]).
     *
     * Unless an [EOFException] is thrown, this is exactly the number of bytes that are [read][readBytes] by this
     * method.
     *
     * @throws[IllegalArgumentException] if [mutf8Length] is negative.
     * @throws[IllegalArgumentException] if [mutf8Length] exceeds [UShort.MAX_VALUE].
     * @throws[EOFException] if there are fewer bytes left in the source than expected by the [mutf8Length].
     * @throws[IOException] if the underlying source of bytes cannot be accessed.
     * @throws[IOException] if [readBytes] returns *more* bytes than expected by the [mutf8Length].
     * @throws[UTFDataFormatException] if a 2-byte character starts on the last byte of the string, meaning the second
     * byte either doesn't exist, or is outside the string's range.
     * @throws[UTFDataFormatException] if a 3-byte character starts on the last or second-to-last bytes of the string,
     * meaning the second and/or third bytes either don't exist, or are outside the string's range.
     * @throws[UTFDataFormatException] if the first byte of a character has all 4 of its most-significant bits set
     * (`1`), which is not expected of any byte in a Modified UTF-8 string. In other words, the byte's bits follow the
     * pattern `1111 xxxx`, where each `x` can be either a `1` or `0`.
     * @throws[UTFDataFormatException] if the first byte of a character has its most-significant bit set (`1`) and its
     * second-most-significant bit unset (`0`), which is only expected for the second and third bytes of characters. In
     * other words, the byte's bits follow the pattern `10xx xxxx`, where each `x` can be either a `1` or `0`.
     */
    @Throws(IOException::class, EOFException::class, UTFDataFormatException::class)
    fun readToAppendable(mutf8Length: Int, destination: Appendable) {
        if (mutf8Length == 0) return

        read(mutf8Length,
            begin = {},
            consume = { char ->
                destination.append(char)
            })
    }

    /**
     * Reads a Modified UTF-8 string from the source and returns its characters as a [CharArray] in their original
     * order.
     *
     * This method does not read the 2-byte "UTF length" that comes before standard Modified UTF-8 strings. That must be
     * read separately (if applicable) and passed to this method via the [mutf8Length] parameter.
     *
     * All indices in the returned array are populated by a character that was read from the source. There is no unused
     * space at the beginning, end, or anywhere in the middle of the array.
     *
     * @param[mutf8Length] The number of bytes used to encode the string. This must be at least `0` and at most
     * `65535` ([UShort.MAX_VALUE]).
     *
     * Unless an [EOFException] is thrown, this is exactly the number of bytes that are [read][readBytes] by this
     * method.
     * @return an array of all the characters that were read, in order. Note that its [size][CharArray.size]...
     * - will never exceed [mutf8Length]
     * - will only be `0` if [mutf8Length] is also `0`
     * - will never be `0` if [mutf8Length] is not `0` either
     *
     * @throws[IllegalArgumentException] if [mutf8Length] is negative.
     * @throws[IllegalArgumentException] if [mutf8Length] exceeds [UShort.MAX_VALUE].
     * @throws[EOFException] if there are fewer bytes left in the source than expected by the [mutf8Length].
     * @throws[IOException] if the underlying source of bytes cannot be accessed.
     * @throws[IOException] if [readBytes] returns *more* bytes than expected by the [mutf8Length].
     * @throws[UTFDataFormatException] if a 2-byte character starts on the last byte of the string, meaning the second
     * byte either doesn't exist, or is outside the string's range.
     * @throws[UTFDataFormatException] if a 3-byte character starts on the last or second-to-last bytes of the string,
     * meaning the second and/or third bytes either don't exist, or are outside the string's range.
     * @throws[UTFDataFormatException] if the first byte of a character has all 4 of its most-significant bits set
     * (`1`), which is not expected of any byte in a Modified UTF-8 string. In other words, the byte's bits follow the
     * pattern `1111 xxxx`, where each `x` can be either a `1` or `0`.
     * @throws[UTFDataFormatException] if the first byte of a character has its most-significant bit set (`1`) and its
     * second-most-significant bit unset (`0`), which is only expected for the second and third bytes of characters. In
     * other words, the byte's bits follow the pattern `10xx xxxx`, where each `x` can be either a `1` or `0`.
     */
    @Throws(IOException::class, EOFException::class, UTFDataFormatException::class)
    fun readToArray(mutf8Length: Int): CharArray {
        if (mutf8Length == 0) return EMPTY_CHAR_ARRAY

        lateinit var chars: CharArray
        var c = 0

        read(mutf8Length,
            begin = { chars = CharArray(mutf8Length) },
            consume = { char ->
                chars[c++] = char
            })

        // If the entire array was filled, then return it. Otherwise, return a copy of the filled region.
        return if (c == chars.size) chars else chars.copyOf(newSize = c)
    }

    /**
     * Reads a Modified UTF-8 string from the source and returns it as a [String].
     *
     * This method does not read the 2-byte "UTF length" that comes before standard Modified UTF-8 strings. That must be
     * read separately (if applicable) and passed to this method via the [mutf8Length] parameter.
     *
     * @param[mutf8Length] The number of bytes used to encode the string. This must be at least `0` and at most
     * `65535` ([UShort.MAX_VALUE]).
     *
     * Unless an [EOFException] is thrown, this is exactly the number of bytes that are [read][readBytes] by this
     * method.
     * @return a string of all the characters that were read, in order. Note that its [length][String.length]...
     * - will never exceed [mutf8Length]
     * - will only be `0` if [mutf8Length] is also `0`
     * - will never be `0` if [mutf8Length] is not `0` either
     *
     * @throws[IllegalArgumentException] if [mutf8Length] is negative.
     * @throws[IllegalArgumentException] if [mutf8Length] exceeds [UShort.MAX_VALUE].
     * @throws[EOFException] if there are fewer bytes left in the source than expected by the [mutf8Length].
     * @throws[IOException] if the underlying source of bytes cannot be accessed.
     * @throws[IOException] if [readBytes] returns *more* bytes than expected by the [mutf8Length].
     * @throws[UTFDataFormatException] if a 2-byte character starts on the last byte of the string, meaning the second
     * byte either doesn't exist, or is outside the string's range.
     * @throws[UTFDataFormatException] if a 3-byte character starts on the last or second-to-last bytes of the string,
     * meaning the second and/or third bytes either don't exist, or are outside the string's range.
     * @throws[UTFDataFormatException] if the first byte of a character has all 4 of its most-significant bits set
     * (`1`), which is not expected of any byte in a Modified UTF-8 string. In other words, the byte's bits follow the
     * pattern `1111 xxxx`, where each `x` can be either a `1` or `0`.
     * @throws[UTFDataFormatException] if the first byte of a character has its most-significant bit set (`1`) and its
     * second-most-significant bit unset (`0`), which is only expected for the second and third bytes of characters. In
     * other words, the byte's bits follow the pattern `10xx xxxx`, where each `x` can be either a `1` or `0`.
     */
    @Throws(IOException::class, EOFException::class, UTFDataFormatException::class)
    fun readToString(mutf8Length: Int): String {
        if (mutf8Length == 0) return ""

        lateinit var chars: CharArray
        var c = 0

        read(mutf8Length,
            begin = { chars = CharArray(mutf8Length) },
            consume = { char ->
                chars[c++] = char
            })

        return chars.concatToString(endIndex = c)
    }

    /**
     * Reads a non-empty Modified UTF-8 string from the source.
     *
     * @param[mutf8Length] The number of bytes that the string is represented using. This must be at least `1`. This
     * must be at least `1` and at most `65535` ([UShort.MAX_VALUE]).
     *
     * This is also the number of bytes that will be read from the source, assuming no exception is thrown.
     * @param[begin] A function that will be called immediately before [consume] is called the first time.
     *
     * This should be used to allocate any resources required by [consume]. It is called after any obvious exceptions
     * have been ruled out, ensuring that those resources won't immediately go to waste if an exception is thrown.
     * @param[consume] A function called once for each character that is read.
     *
     * Unless an exception is thrown, this will always be called at least once.
     * @return the string that was encoded in those bytes.
     *
     * @throws[IllegalArgumentException] if [mutf8Length] is `0`; the caller should handle empty strings on its own by
     * returning the equivalent for its respective type (e.g. `""` for strings, or `CharArray(size = 0)` for arrays).
     * @throws[IllegalArgumentException] if [mutf8Length] is negative.
     * @throws[IllegalArgumentException] if [mutf8Length] exceeds [UShort.MAX_VALUE].
     * @throws[EOFException] if there are fewer bytes left in the source than expected by the [mutf8Length].
     * @throws[IOException] if the underlying source of bytes cannot be accessed.
     * @throws[IOException] if [readBytes] returns *more* bytes than expected by the [mutf8Length].
     * @throws[UTFDataFormatException] if a 2-byte character starts on the last byte of the string, meaning the second
     * byte either doesn't exist, or is outside the string's range.
     * @throws[UTFDataFormatException] if a 3-byte character starts on the last or second-to-last bytes of the string,
     * meaning the second and/or third bytes either don't exist, or are outside the string's range.
     * @throws[UTFDataFormatException] if the first byte of a character has all 4 of its most-significant bits set
     * (`1`), which is not expected of any byte in a Modified UTF-8 string. In other words, the byte's bits follow the
     * pattern `1111 xxxx`, where each `x` can be either a `1` or `0`.
     * @throws[UTFDataFormatException] if the first byte of a character has its most-significant bit set (`1`) and its
     * second-most-significant bit unset (`0`), which is only expected for the second and third bytes of characters. In
     * other words, the byte's bits follow the pattern `10xx xxxx`, where each `x` can be either a `1` or `0`.
     */
    @Throws(IOException::class, EOFException::class, UTFDataFormatException::class)
    private inline fun read(mutf8Length: Int, begin: () -> Unit, consume: (Char) -> Unit) {
        require(mutf8Length != 0) { "read() should not be called for empty strings" }
        require(mutf8Length > 0) { "mutf8Length must be at least 1, not $mutf8Length" }
        require(mutf8Length <= UShort.MAX_VALUE.toInt()) { "mutf8Length must be at most ${UShort.MAX_VALUE}, not $mutf8Length" }

        val bytes = readBytes(amount = mutf8Length)
        var b = 0

        if (bytes.size < mutf8Length)
            throw EOFException("${bytes.size} bytes were read from the stream instead of the $mutf8Length expected")

        if (bytes.size > mutf8Length)
            throw IOException("${bytes.size} were received but only $mutf8Length were requested")

        begin()

        // Assume all characters are 7-bit ASCII (1 byte each) until we find one that isn't. At that point, the second
        // loop takes over, handling 2-byte and 3-byte characters as well. This optimization comes from Java's own
        // `DataInput.readUTF()`.
        do {
            val byte1 = bytes[b].toInt() and 0xFF
            if (byte1 and 0x80 == 0x80) break

            consume(byte1.toChar())
            b++
        } while (b < mutf8Length)

        // Same gist as the previous loop, but now we have to account for characters that are encoded using 2 and 3
        // bytes as well. If all the characters are ASCII, then this is skipped because the previous loop read them all.
        while (b < mutf8Length) {
            val byte1 = bytes[b++].toInt()

            // Using the 4 most-significant bits of the first byte, we can determine how many bytes long the character
            // is.
            when (byte1 shr 4 and 0xF) {
                // If the most-significant bit is a `0`, then it's only the 1 byte.
                0, 1, 2, 3, 4, 5, 6, 7 -> {
                    consume(byte1.toChar())
                }

                // If the bits are `110x`, then it's 2 bytes.
                12, 13 -> {
                    // If `byte1` was the last byte, throw because there's no `byte2` for us to read.
                    if (b + 1 > mutf8Length)
                        throwForTruncatedChar(charSize = 2, bytesLeft = 0)

                    // Read the second byte and ensure its bits match the pattern `10xxxxxx`.
                    val byte2 = bytes[b++].toInt()
                    if (byte2 and 0xC0 != 0x80)
                        throwForBadSecondaryByte(byte2, charSize = 2, byteOffset = 1)

                    // Combine both bytes into a single Char.
                    consume(((byte1 and 0x1F shl 6) or (byte2 and 0x3F)).toChar())
                }

                // If the bits are `1110`, then it's 3 bits.
                14 -> {
                    // If `byte1` was the last byte, throw because there's no `byte2` for us to read. If `byte2` will be
                    // the last byte, throw because there's no `byte3` for us to read.
                    if (b + 2 > mutf8Length)
                        throwForTruncatedChar(charSize = 2, bytesLeft = mutf8Length - b)

                    // Read the second byte and ensure its bits match the pattern `10xxxxxx`.
                    val byte2 = bytes[b++].toInt()
                    if (byte2 and 0xC0 != 0x80)
                        throwForBadSecondaryByte(byte2, charSize = 3, byteOffset = 1)

                    // Read the third byte and ensure its bits match the pattern `10xxxxxx`.
                    val byte3 = bytes[b++].toInt()
                    if (byte3 and 0xC0 != 0x80)
                        throwForBadSecondaryByte(byte2, charSize = 3, byteOffset = 2)

                    // Combine all 3 bytes into a single Char.
                    consume(((byte1 and 0x0F shl 12) or (byte2 and 0x3F shl 6) or (byte3 and 0x3F)).toChar())
                }

                // If the bits are `1111` or `10xx`, then we throw because neither is allowed for a char's first byte.
                else -> throwForBadPrimaryByte(byte1)
            }
        }
    }

    @Throws(UTFDataFormatException::class)
    private fun throwForTruncatedChar(charSize: Int, bytesLeft: Int): Nothing =
        throw UTFDataFormatException("A ${charSize}-byte character was started with${if (bytesLeft > 0) "only" else ""} $bytesLeft bytes left in the string")

    @Throws(UTFDataFormatException::class)
    private fun throwForBadPrimaryByte(byte: Int): Nothing =
        throw UTFDataFormatException("Byte #1 of a character has the bits ${byte.toBinaryOctet()}₂ which doesn't match any of the expected patterns: 0xxxxxxx₂ or 110xxxxx₂ or 1110xxxx₂")

    @Throws(UTFDataFormatException::class)
    private fun throwForBadSecondaryByte(byte: Int, charSize: Int, byteOffset: Int): Nothing =
        throw UTFDataFormatException("Byte #${byteOffset + 1} of a ${charSize}-byte character has the bits ${byte.toBinaryOctet()}₂ which don't match the expected pattern 10xxxxxx₂")

    private fun Int.toBinaryOctet(): String =
        (this and 0xFF).toString(radix = 2).padStart(length = 8, padChar = '0')

    private companion object {
        private val EMPTY_CHAR_ARRAY: CharArray by lazy {
            CharArray(size = 0)
        }
    }
}