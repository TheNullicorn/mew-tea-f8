package me.nullicorn.mewteaf8.internal

import me.nullicorn.mewteaf8.mutf8Length
import kotlin.math.min

/**
* A sequence of bytes that can be written to in bulk, specifically for the purpose of writing Modified UTF-8 strings.
*/
@InternalMutf8Api
interface Mutf8Sink {

    /**
     * Writes all [bytes] whose indices are less than [untilIndex] to the sink.
     *
     * @param[bytes] The array whose bytes should be written to the sink, in order.
     * @param[untilIndex] The index in the [bytes] array to exclude all bytes at & after.
     *
     * This must be at least `0`, in which case nothing will be written. At most, it must be `bytes.size`, in which case
     * all bytes in the array will be written.
     *
     * @throws[IllegalArgumentException] if [untilIndex] is less than `0` or greater than `bytes.size`.
     * @throws[Mutf8IOException] if an I/O related issue occurs while trying to write the [bytes].
     */
    fun writeBytes(bytes: ByteArray, untilIndex: Int)

    /**
    * Writes the length, or "UTF Length" as Java refers to it, of a Modified UTF-8 string to the sink.
    *
    * For a simple stream, this should behave as if the [length] were split into two bytes and written in big-endian
    * order. In code, that would be:
    * ```kotlin
    * write((length.toInt() shr 8 and 0xFF).toByte())
    * write((length.toInt() and 0xFF).toByte())
    * ```
    * ...where `write` is a function that writes an individual byte to the underlying destination.
    *
    * @throws[Mutf8IOException] if an I/O related issue occurs while trying to write those bytes.
    */
    fun writeLength(length: UShort)

    /**
     * Writes a Modified UTF-8 string to the sink.
     *
     * This does not write the [mutf8Length] of the [string]. That should be done separately using [writeLength].
     *
     * @param[string] The characters to write to the sink.
     * @param[bytesPerWrite] The maximum number of bytes to write to the sink at once when writing in bulk. This must
     * be at least `1`.
     *
     * @throws[IllegalArgumentException] if [bytesPerWrite] is less than `1`.
     * @throws[IllegalArgumentException] if the [mutf8Length] of the string exceeds [UShort.MAX_VALUE].
     * @throws[Mutf8IOException] if an I/O related issue occurs while trying to write the string's bytes.
     */
    fun writeString(string: String, bytesPerWrite: Int) {
        require(bytesPerWrite >= 1) { "bytesPerWrite must be at least 1" }

        val bytesNeeded = string.mutf8Length
        require(bytesNeeded <= UShort.MAX_VALUE.toLong()) { "String is too long to be encoded as Modified UTF-8 data; it would take up $bytesNeeded bytes, but the maximum allowed is ${UShort.MAX_VALUE}" }

        val buffer = ByteArray(size = min(bytesNeeded, bytesPerWrite.toLong()).toInt())
        var b = 0

        /**
         * [Writes][writeBytes] any bytes to the sink that haven't already been flushed from the `buffer`, then resets `b` to `0`.
         */
        fun flush() {
            if (b > 0) {
                writeBytes(buffer, untilIndex = b)
                b = 0
            }
        }

        /**
         * Inserts the [byte] into the next available position in the `buffer`, `b`, and increments `b` by `1`.
         *
         * If `b` is already at the end of the `buffer`, it is reset to `0` before inserting the byte.
         */
        fun writeByte(byte: Int) {
            if (b == buffer.size) flush()
            buffer[b++] = byte.toByte()
        }

        for (char in string)
            if (char in '\u0001'..'\u007F') {
                writeByte(char.code)

            } else if (char < '\u0800') {
                writeByte(char.code shr 6 and 0x1F or 0xC0)
                writeByte(char.code /* */ and 0x3F or 0x80)

            } else {
                writeByte(char.code shr 12 /**/ and 0x0F or 0xE0)
                writeByte(char.code shr 6 /* */ and 0x3F or 0x80)
                writeByte(char.code /*       */ and 0x3F or 0x80)
            }

        // Flush any remaining bytes that weren't already written to the sink.
        if (b > 0) flush()
    }
}