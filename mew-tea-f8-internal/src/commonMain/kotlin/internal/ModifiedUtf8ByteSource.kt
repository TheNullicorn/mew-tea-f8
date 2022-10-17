package me.nullicorn.mewteaf8.internal

/**
 * A sequence of bytes that can be read in bulk, specifically for the purpose of reading Modified UTF-8 strings.
 */
@InternalMewTeaF8Api
interface ModifiedUtf8ByteSource {

    /**
     * Reads a specific [amount] of bytes from the source and returns them in order as a [ByteArray].
     *
     * @param[amount] The number of bytes to read.
     * @return an array, whose size equals [amount], of the next bytes that appear in the source in order.
     *
     * @throws[ModifiedUtf8IOException] if the source does not have enough bytes left in it to supply the requested [amount].
     * @throws[ModifiedUtf8IOException] if an I/O related issue occurs while trying to read those bytes.
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
     * @throws[ModifiedUtf8IOException] if the source does not have enough bytes left in it to read the length.
     * @throws[ModifiedUtf8IOException] if an I/O related issue occurs while trying to read those bytes.
     */
    fun readUtfLength(): UShort
}