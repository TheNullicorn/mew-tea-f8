package me.nullicorn.mewteaf8

import kotlin.jvm.JvmSynthetic
import kotlin.math.min

/**
 * Something that Modified UTF-8 data can be written to.
 *
 * For convenience in various scenarios, said data can be written from several forms:
 * - [writeFromSequence]: The characters to write are taken a [CharSequence], such as a [String]
 * - [writeFromArray]: The characters to write are taken from a [CharArray]
 *
 * Both methods also have overloads for writing a specific range of characters from the sequences, rather than the
 * entire thing.
 *
 * @param[bytesPerWrite] The maximum number of bytes that will be copied to the sink's underlying destination at a time.
 * This must be at least `1`, though higher is recommended.
 *
 * @throws[IllegalArgumentException] if [bytesPerWrite] is less than `1`.
 */
abstract class Mutf8Sink(private val bytesPerWrite: Int = 1024) {

    init {
        require(bytesPerWrite > 0) { "bytesPerWrite must be at least 1, not $bytesPerWrite" }
    }

    /**
     * A reusable byte buffer to which characters' bytes can be added to while [encoding][write], and copied
     * from when [writing][writeBytes].
     *
     * Its [size][ByteArray.size] should never exceed [bytesPerWrite], but may be less if the only strings written so
     * far have taken up fewer bytes than that.
     */
    private var buffer: ByteArray = EMPTY_BUFFER

    /**
     * Writes the length of a Modified UTF-8 string to the sink's underlying destination.
     *
     * This is done by taking the [mutf8Length]'s 16 least-significant bits and splitting them into `2` bytes, which
     * are then written in big-endian order. That is, if `writeByte()` is a method for writing an individual byte to the
     * sink:
     * ```kotlin
     * writeByte((mutf8Length shr 8).toByte())
     * writeByte(mutf8Length.toByte())
     * ```
     *
     * The [mutf8Length] parameter is an [Int], though only the 16 least-significant bits represent the value. Every
     * other bit should be unset or else an [IllegalArgumentException] will be thrown. Thus, [mutf8Length] must be in
     * the range `0 .. 65535`.
     *
     * @param[mutf8Length] The length of the string in question, in bytes.
     *
     * @throws[IllegalArgumentException] if [mutf8Length] is negative.
     * @throws[IllegalArgumentException] if [mutf8Length] exceeds `65535`.
     * @throws[IOException] if an I/O issue occurs while trying to write either of the bytes.
     *
     * @see[me.nullicorn.mewteaf8.mutf8Length]
     */
    @Throws(IOException::class)
    abstract fun writeLength(mutf8Length: Int)

    /**
     * Writes all [bytes] in an array whose indices are less than [untilIndex] to the sink's underlying destination, in
     * order.
     *
     * @param[bytes] The bytes whose elements should be written.
     * @param[untilIndex] The index which no bytes at or after should be written from the [bytes] array.
     *
     * @throws[IOException] if an I/O issue occurs while trying to write any or all of the bytes.
     */
    @Throws(IOException::class)
    protected abstract fun writeBytes(bytes: ByteArray, untilIndex: Int)

    // region Ranged Overloads (startIndex, endIndex)

    /**
     * Encodes all the characters in a specific range of a [CharSequence] and writes them to the sink's underlying
     * destination.
     *
     * This method does not write the 2-byte "UTF length" that comes before standard Modified UTF-8 strings. That must
     * be written [separately][writeLength] if needed.
     *
     * The "range" is defined as a [startIndex] and [endIndex], which behave the same as the first and second operands
     * of Kotlin's infix [until] function. Both indexes must be at least `0`, and the [startIndex] cannot be greater
     * than the [endIndex], though they can be equal to select `0` characters. [startIndex] must be less than the
     * sequence's [length][CharSequence.length] and [endIndex] must be less than *or* equal to
     * [length][CharSequence.length]. If the above conditions are not met, an [IllegalArgumentException] is thrown.
     *
     * @param[characters] The sequence of characters to write from.
     * @param[startIndex] The first and lowest index from which [characters] will be written.
     * @param[endIndex] The index after [startIndex] to stop writing the [characters] at.
     *
     * This index is exclusive, meaning the character at this index will not be written, nor will any after it.
     *
     * @throws[IllegalArgumentException] if the [length][CharSequence.length] of the [CharSequence] is a negative
     * number.
     * @throws[IndexOutOfBoundsException] if the [startIndex] is negative.
     * @throws[IndexOutOfBoundsException] if the [startIndex] exceeds the [length][CharSequence.length], or if they are
     * equal to each other at a value other than `0`.
     * @throws[IndexOutOfBoundsException] if [endIndex] exceeds the [length][CharSequence.length].
     * @throws[IllegalArgumentException] if [startIndex] is greater than the [endIndex].
     * @throws[IllegalArgumentException] if the [mutf8Length][CharSequence.mutf8Length] of the [characters] in that
     * range exceeds `65535` ([UShort.MAX_VALUE]).
     * @throws[IOException] if an I/O issue occurs while trying to write any or all of the sequence's encoded bytes.
     */
    @Throws(IOException::class)
    fun writeFromSequence(characters: CharSequence, startIndex: Int, endIndex: Int) =
        write(
            characters,
            startIndex,
            endIndex,
            getMutf8Length = CharSequence::mutf8Length,
            getChar = { i -> characters[i] }
        )

    /**
     * Encodes all the characters in a specific range of a [CharArray] and writes them to the sink's underlying
     * destination.
     *
     * This method does not write the 2-byte "UTF length" that comes before standard Modified UTF-8 strings. That must
     * be written [separately][writeLength] if needed.
     *
     * The "range" is defined as a [startIndex] and [endIndex], which behave the same as the first and second operands
     * of Kotlin's infix [until] function. Both indexes must be at least `0`, and the [startIndex] cannot be greater
     * than the [endIndex], though they can be equal to select `0` characters. [startIndex] must be less than the
     * array's [size][CharArray.size] and [endIndex] must be less than *or* equal to [size][CharArray.size]. If the
     * above conditions are not met, an [IllegalArgumentException] is thrown.
     *
     * @param[characters] The array of characters to write from.
     * @param[startIndex] The first and lowest index from which [characters] will be written.
     * @param[endIndex] The index after [startIndex] to stop writing the [characters] at.
     *
     * This index is exclusive, meaning the character at this index will not be written, nor will any after it.
     *
     * @throws[IllegalArgumentException] if the [size][CharArray.size] of the [CharArray] is a negative number.
     * @throws[IndexOutOfBoundsException] if the [startIndex] is negative.
     * @throws[IndexOutOfBoundsException] if the [startIndex] exceeds the [size][CharArray.size], or if they are
     * equal to each other at a value other than `0`.
     * @throws[IndexOutOfBoundsException] if [endIndex] exceeds the [size][CharArray.size].
     * @throws[IllegalArgumentException] if [startIndex] is greater than the [endIndex].
     * @throws[IllegalArgumentException] if the [mutf8Length][CharArray.mutf8Length] of the [characters] in that range
     * exceeds `65535` ([UShort.MAX_VALUE]).
     * @throws[IOException] if an I/O issue occurs while trying to write any or all of the sequence's encoded bytes.
     */
    @Throws(IOException::class)
    fun writeFromArray(characters: CharArray, startIndex: Int, endIndex: Int) =
        write(
            characters,
            startIndex,
            endIndex,
            getMutf8Length = CharArray::mutf8Length,
            getChar = { i -> characters[i] }
        )

    //endregion
    //region Ranged Overloads (IntRange)

    /**
     * An alias for [writeFromSequence] that uses a single [range] parameter, rather than a separate `startIndex` and
     * `endIndex`.
     *
     * Other than that, the entire contract of [writeFromSequence] applies to this method as well.
     *
     * @see[writeFromSequence]
     */
    @JvmSynthetic
    @Throws(IOException::class)
    fun writeFromSequence(characters: CharSequence, range: IntRange) =
        writeFromSequence(characters, startIndex = range.first, endIndex = range.last + 1)

    /**
     * An alias for [writeFromArray] that uses a single [range] parameter, rather than a separate `startIndex` and
     * `endIndex`.
     *
     * Other than that, the entire contract of [writeFromArray] applies to this method as well.
     *
     * @see[writeFromArray]
     */
    @JvmSynthetic
    @Throws(IOException::class)
    fun writeFromArray(characters: CharArray, range: IntRange) =
        writeFromArray(characters, startIndex = range.first, endIndex = range.last + 1)

    //endregion
    //region No-Range Overloads

    /**
     * Encodes all the characters in a [CharSequence] and writes them to the sink's underlying destination.
     *
     * This is an alias for [writeFromSequence] with `startIndex = 0` and `endIndex = characters.length`. The selected
     * indices are guaranteed to be valid, so all [IllegalArgumentException]s documented at [writeFromSequence] are
     * guaranteed not to occur unless also documented on this function. Other than that, the contract & documentation
     * for [writeFromSequence] applies here as well.
     *
     * @throws[IllegalArgumentException] if the [length][CharSequence.length] of the [CharSequence] is a negative
     * number.
     * @throws[IllegalArgumentException] if the [mutf8Length][CharSequence.mutf8Length] of the [characters] exceeds
     * `65535` ([UShort.MAX_VALUE]).
     *
     * @see[writeFromSequence]
     */
    @Throws(IOException::class)
    fun writeFromSequence(characters: CharSequence) =
        writeFromSequence(characters, startIndex = 0, endIndex = characters.length)

    /**
     * Encodes all the characters in a [CharSequence] and writes them to the sink's underlying destination.
     *
     * This is an alias for [writeFromArray] with `startIndex = 0` and `endIndex = characters.size`. The selected
     * indices are guaranteed to be valid, so all [IllegalArgumentException]s and [IndexOutOfBoundsException]s
     * documented at [writeFromArray] are guaranteed not to occur unless also documented on this function. Other than
     * that, the contract & documentation for [writeFromArray] applies here as well.
     *
     * @throws[IllegalArgumentException] if the [mutf8Length][CharArray.mutf8Length] of the [characters] exceeds `65535`
     * ([UShort.MAX_VALUE]).
     *
     * @see[writeFromArray]
     */
    @Throws(IOException::class)
    fun writeFromArray(characters: CharArray) =
        writeFromArray(characters, startIndex = 0, endIndex = characters.size)

    //endregion

    /**
     * Writes all the [characters] in a specific range of an object, [T], given a [function][getChar] for retrieving
     * the object's [Chars][Char] by index.
     *
     * The "range" is defined as a [startIndex] and [endIndex], which behave the same as the first and second operands
     * of Kotlin's infix [until] function. Both indexes must be at least `0`, and the [startIndex] cannot be greater
     * than the [endIndex], though they can be equal to select `0` characters. [startIndex] must be less than the
     * `mutf8Length` (determined by [getMutf8Length]), and [endIndex] must be less than *or* equal to the `mutf8Length`.
     * If the above conditions are not met, an [IllegalArgumentException] is thrown.
     *
     * @param[T] The Kotlin type of the [characters] object.
     * @param[characters] The object to get the characters from.
     * @param[startIndex] The first and lowest index from which characters in the object will be written.
     * @param[endIndex] The index after [startIndex] to stop writing the object's characters at.
     *
     * This index is exclusive, meaning the character at this index will not be written, nor will any after it.
     * @param[getMutf8Length] A reference to the `mutf8Length` function for [T], such as [CharSequence.mutf8Length] or
     * [CharArray.mutf8Length].
     * @param[getChar] A function for retrieving one of the object's characters, given that character's index.
     *
     * @throws[IllegalArgumentException] if [characters] is a [CharSequence] whose [length][CharSequence.length] is a
     * negative number.
     * @throws[IndexOutOfBoundsException] if the [startIndex] is negative.
     * @throws[IndexOutOfBoundsException] if the [startIndex] exceeds the object's `length` (or `size`), or if they are
     * equal to each other at a value other than `0`.
     * @throws[IndexOutOfBoundsException] if [endIndex] exceeds the object's `length` (or `size`).
     * @throws[IllegalArgumentException] if [startIndex] is greater than the [endIndex].
     * @throws[IllegalArgumentException] if the length of the string, as calculated by [getMutf8Length], exceeds
     * `65535` ([UShort.MAX_VALUE]).
     * @throws[IOException] if an I/O issue occurs while trying to write any or all of the object's encoded bytes.
     */
    @Throws(IOException::class)
    private inline fun <T> write(
        characters: T,
        startIndex: Int,
        endIndex: Int,
        getMutf8Length: T.(startIndex: Int, endIndex: Int) -> Long,
        getChar: T.(index: Int) -> Char
    ) {
        // Calculate the number of bytes that will be used to encode the sequence.
        val mutf8Length = characters.getMutf8Length(startIndex, endIndex)
        require(mutf8Length <= UShort.MAX_VALUE.toLong()) { "String needs $mutf8Length bytes to be encoded, but the maximum is ${UShort.MAX_VALUE}" }

        // If no bytes will be used, then we can short-circuit because nothing needs to be written.
        val mutf8LengthInt = mutf8Length.toInt()
        if (mutf8Length == 0.toLong()) return

        // Otherwise, grow our existing buffer if it's too small. The largest it can be is the `bytesPerWrite` specified
        // in the constructor, but if `mutf8Length` is less than that, then we'll only make it that big to save memory.
        val bufferSize = min(bytesPerWrite, mutf8LengthInt)
        if (bufferSize > buffer.size) buffer = ByteArray(bufferSize)

        // c = our current index in the `characters` object
        // b = our current index in the `buffer`
        var c = startIndex
        var b = 0

        // Encode each character one-by-one.
        while (c != endIndex) {
            val char = characters.getChar(c++)

            // Determine how many bytes the `char` should be encoded in based on its binary magnitude, or the position
            // its most-significant set bit is.
            when (char.code.toShort().countLeadingZeroBits()) {
                // Characters whose codes start with 8 or more `0` bits (except '\u0000') are encoded using 1 byte.
                15, 14, 13, 12, 11, 10, 9 -> {
                    b = writeByte(b, char.code)
                }

                // Characters whose codes start with 4 through 7 `0` bits (and '\u0000') are encoded using 2 bytes.
                8, 7, 6, 5, 16 -> {
                    b = writeByte(b, char.code shr 6 and 0x1F or 0xC0)
                    b = writeByte(b, char.code /* */ and 0x3F or 0x80)
                }

                // Characters whose codes start with 0 through 3 `0` bits are encoded using 3 bytes.
                else -> {
                    b = writeByte(b, char.code shr 12 /**/ and 0x0F or 0xE0)
                    b = writeByte(b, char.code shr 6 /* */ and 0x3F or 0x80)
                    b = writeByte(b, char.code /*       */ and 0x3F or 0x80)
                }
            }
        }

        // If any bytes are left in the buffer that haven't been flushed, do that now.
        flush(b)
    }

    /**
     * Sets the [value] of the byte at a given [index] in the [buffer] and returns the index that the next byte, if any,
     * should be written to in the [buffer].
     *
     * If [index] is the [buffer's lastIndex][ByteArray.lastIndex], then [flush] will be called on the entire [buffer],
     * and the returned index will be `0`, indicating that the next byte, if any, should be the first element in the
     * buffer.
     *
     * For performance reasons, the [index] parameter is not checked. It is expected to be a valid index in the
     * [buffer]. If not, this method's behaviour is undefined.
     *
     * @param[index] The index to insert the [value] at in the [buffer].
     * @param[value] The value to insert at that [index] in the [buffer]. Only the 8 least-significant bits of this
     * number are used; in other words, `value and 0xFF`.
     * @return the index that the next byte, if any, should be written at.
     *
     * @throws[IOException] if the [buffer] is flushed but an I/O issue occurs while trying to do so.
     */
    @Throws(IOException::class)
    @Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")
    private inline fun writeByte(index: Int, value: Int): Int {
        var i = index

        // Set the element at the index `i` in the buffer to `value`, then increment the index.
        buffer[i++] = value.toByte()

        // If the incremented index is outside the buffer's bounds, flush the bytes & reset the index back to `0`.
        if (i == buffer.size) {
            flush(untilIndex = i)
            i = 0
        }

        return i
    }

    /**
     * Writes all bytes in the [buffer] up until a given [index][untilIndex] to the sink.
     *
     * @param[untilIndex] The exclusive index of the last byte to write in the [buffer]. This is also the number of
     * bytes that will be written.
     *
     * @throws[IOException] if an I/O issue occurs while trying to write the [buffer]'s bytes.
     */
    @Throws(IOException::class)
    @Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")
    private inline fun flush(untilIndex: Int) {
        if (untilIndex != 0)
            writeBytes(buffer, untilIndex)
    }

    private companion object {

        /**
         * An empty [ByteArray] used as the default value for [buffer], which is not nullable.
         */
        private val EMPTY_BUFFER = ByteArray(0)
    }
}