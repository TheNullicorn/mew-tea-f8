@file:JvmName("Mutf8Length")

package me.nullicorn.mewteaf8

import kotlin.jvm.JvmName

/**
 * The number of bytes used to encode the character in a Modified UTF-8 sequence.
 *
 * - For characters in the range `'\u0001' .. '\u007F'`, this is `1`
 * - For characters in the range `'\u0080' .. '\u07FF'`, this is `2`
 * - For characters in the range `'\u0800' .. '\uFFFF'`, this is `3`
 * - For the character `'\u0000'`, this is `2`
 *
 * That list covers the entire range of a [Char], so there are no other values to note.
 *
 * This can be used from Java sources as follows:
 * ```java
 * import me.nullicorn.mewteaf8.Mutf8Length;
 * /* ... */
 * Mutf8Length.of(char)
 * ```
 *
 * @receiver The character to check the Modified UTF-8 length of.
 */
@get:JvmName("of")
val Char.mutf8Length: Int
    get() = when (this.code.toShort().countLeadingZeroBits()) {
        // Characters in the range `'\u0001' .. '\u007F'` are encoded using `1` byte
        15, 14, 13, 12, 11, 10, 9 -> 1

        // Characters in the range `'\u0080' .. '\u07FF'` are encoded using `2` bytes
        // The character `'\u0000'` is encoded using `2` bytes
        16, 8, 7, 6, 5 -> 2

        // Characters in the range `'\u0800' .. '\uFFFF'` are encoded using `3` bytes
        else -> 3
    }

/**
 * The summed [mutf8Length][Char.mutf8Length] for all the characters in the sequence.
 *
 * This is an alias for [CharSequence.mutf8Length] with `startIndex = 0` and `endIndex = length`. The selected indices
 * are guaranteed to be valid, so all [IllegalArgumentException]s documented at [CharSequence.mutf8Length] are
 * guaranteed not to occur unless also documented on this property. Other than that, the contract & documentation for
 * [CharSequence.mutf8Length] applies here as well.
 *
 * This can be used from Java sources as follows:
 * ```java
 * import me.nullicorn.mewteaf8.Mutf8Length;
 * /* ... */
 * Mutf8Length.of(charSequence)
 * ```
 *
 * @throws[IllegalArgumentException] if the [length][CharSequence.length] of the [CharSequence] is a negative number.
 *
 * @see[CharSequence.mutf8Length]
 */
@get:JvmName("of")
val CharSequence.mutf8Length: Long
    get() = mutf8Length(startIndex = 0, endIndex = length)

/**
 * The summed [mutf8Length][Char.mutf8Length] for all the characters in the array.
 *
 * This is an alias for [CharArray.mutf8Length] with `startIndex = 0` and `endIndex = size`. The selected indices are
 * guaranteed to be valid, so all [IllegalArgumentException]s documented at [CharArray.mutf8Length] are guaranteed not
 * to occur. Other than that, the contract & documentation for [CharArray.mutf8Length] applies here as well.
 *
 * This can be used from Java sources as follows:
 * ```java
 * import me.nullicorn.mewteaf8.Mutf8Length;
 * /* ... */
 * Mutf8Length.of(charArray)
 * ```
 *
 * @see[CharArray.mutf8Length]
 */
@get:JvmName("of")
val CharArray.mutf8Length: Long
    get() = mutf8Length(startIndex = 0, endIndex = size)

/**
 * The summed [mutf8Length][Char.mutf8Length] for the characters in a subsequence of this one.
 *
 * If that sum exceeds `65535`, or [UShort.MAX_VALUE], then it will still be returned, but note that the selected range
 * is too long to encode as a single Modified UTF-8 sequence; it must be split up, compressed, or encoded differently.
 *
 * Assuming the [startIndex] and [endIndex] form a valid range in a sequence, `charSequence`, then this will yield the
 * same sum as:
 * ```kotlin
 * charSequence.subSequence(startIndex, endIndex).mutf8Length
 * ```
 *
 * This can be used from Java sources as follows:
 * ```java
 * import me.nullicorn.mewteaf8.Mutf8Length;
 * /* ... */
 * Mutf8Length.of(charSequence, startIndex, endIndex)
 * ```
 *
 * @receiver The characters to determine the collective length of, in bytes.
 * @param[startIndex] The index in the sequence to start counting from.
 *
 * If the sequence is [empty][CharSequence.isEmpty], this must be `0`. Otherwise, it should be a valid index in the
 * sequence, such that it appears in the sequence's [indices][CharSequence.indices]. This can never be less than `0`.
 * @param[endIndex] The index in the sequence to stop counting at.
 *
 * This cannot exceed the [length][CharSequence.length] of the sequence, and cannot be less than the [startIndex].
 * @return the number of bytes needed to represent the range of characters as a Modified UTF-8 sequence.
 *
 * @throws[IllegalArgumentException] if the [length][CharSequence.length] of the [CharSequence] is a negative number.
 * @throws[IndexOutOfBoundsException] if the [startIndex] is negative.
 * @throws[IndexOutOfBoundsException] if the [startIndex] exceeds the [length][CharSequence.length], or if they are
 * equal to each other at a value other than `0`.
 * @throws[IndexOutOfBoundsException] if [endIndex] exceeds the [length][CharSequence.length].
 * @throws[IllegalArgumentException] if [startIndex] is greater than the [endIndex].
 */
@JvmName("of")
fun CharSequence.mutf8Length(startIndex: Int = 0, endIndex: Int = length): Long {
    val lengthInChars = length
    checkStartAndEndIndices(startIndex, endIndex, lengthInChars)

    var i = startIndex
    return calculateMutf8Length(
        lengthInChars = endIndex - startIndex,
        hasNext = { i != endIndex },
        nextChar = { this[i++] })
}

/**
 * The summed [mutf8Length][Char.mutf8Length] for the characters in a subsection of this array.
 *
 * The contract of this method is identical to that of [CharSequence.mutf8Length], except...
 * - [CharSequence] is replaced with [CharArray]
 * - [CharSequence.length] is replaced with [CharArray.size]
 * - [CharSequence.lastIndex] is replaced with [CharArray.lastIndex]
 * - No [IllegalArgumentException] will be thrown if the array's [size][CharArray.size] is negative because that is not
 * possible
 *
 * All parameters, exceptions, constraints, and notes from [CharSequence.mutf8Length] apply to this function as well.
 *
 * This can be used from Java sources as follows:
 * ```java
 * import me.nullicorn.mewteaf8.Mutf8Length;
 * /* ... */
 * Mutf8Length.of(charArray, startIndex, endIndex)
 * ```
 *
 * @see[CharSequence.mutf8Length]
 */
@JvmName("of")
fun CharArray.mutf8Length(startIndex: Int = 0, endIndex: Int = size): Long {
    val lengthInChars = size
    checkStartAndEndIndices(startIndex, endIndex, lengthInChars)

    var i = startIndex
    return calculateMutf8Length(
        lengthInChars = endIndex - startIndex,
        hasNext = { i != endIndex },
        nextChar = { this[i++] })
}

/**
 * Determines how many bytes are needed to encode a sequence of characters.
 *
 * @param[lengthInChars] The number of characters that will be included.
 * @param[hasNext] A function that checks if [nextChar] should be called again. If `false`, the function will exit and
 * return its current sum at that point in time. If `true`, [hasNext] will be called, and its result will be added to
 * the sum. This is identical in concept to [Iterator.hasNext].
 * @param[nextChar] A function that returns the next character in the sequence. This will be called once after each time
 * [hasNext] returns `true`.
 * @return the total number of bytes needed to encode the sequence's characters in Modified UTF-8.
 */
private inline fun calculateMutf8Length(lengthInChars: Int, hasNext: () -> Boolean, nextChar: () -> Char): Long {
    if (lengthInChars == 0) return 0
    require(lengthInChars > 0) { "length cannot be a negative number: $lengthInChars" }

    var lengthInBytes = lengthInChars.toLong()
    while (hasNext())
        when (nextChar().code.toShort().countLeadingZeroBits()) {
            // Characters in the range `'\u0080' .. '\u07FF'` add `2` to the sum
            // The character `'\u0000'` adds `2` to the sum
            16, 8, 7, 6, 5 -> lengthInBytes++

            // Characters in the range `'\u0800' .. '\uFFFF'` add `3` to the sum
            4, 3, 2, 1, 0 -> lengthInBytes += 2
        }
    return lengthInBytes
}

private fun checkStartAndEndIndices(startIndex: Int, endIndex: Int, lengthInChars: Int) {
    if (lengthInChars < 0)
        throw IllegalArgumentException("length must be at least 0, not $lengthInChars")

    if (startIndex < 0)
        throw IndexOutOfBoundsException("startIndex must be at least 0, not $startIndex")

    if (startIndex > lengthInChars || (startIndex == lengthInChars && lengthInChars != 0))
        throw IndexOutOfBoundsException("startIndex, $startIndex, must be less than length, $lengthInChars")

    if (endIndex > lengthInChars)
        throw IndexOutOfBoundsException("endIndex, $endIndex, must not exceed length, $lengthInChars")

    if (startIndex > endIndex)
        throw IllegalArgumentException("startIndex, $startIndex, must not exceed endIndex, $endIndex")
}