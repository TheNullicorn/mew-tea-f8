@file:JvmName("Mutf8Length")

package me.nullicorn.mewteaf8

import kotlin.jvm.JvmName

/**
 * The number of bytes needed to encode the entire sequence of characters in Modified UTF-8.
 *
 * This is an alias for [CharSequence.mutf8Length] with `startIndex = 0` and `endIndex = length`. The selected indices
 * are guaranteed to be valid, so all [IllegalArgumentException]s documented at [CharSequence.mutf8Length] are
 * guaranteed not to occur unless also documented on this property. Other than that, the contract & documentation for
 * [CharSequence.mutf8Length] applies here as well.
 *
 * @throws[IllegalArgumentException] if the [length][CharSequence.length] of the [CharSequence] is a negative number.
 *
 * @see[CharSequence.mutf8Length]
 */
@get:JvmName("of")
val CharSequence.mutf8Length: Long
    get() = mutf8Length(startIndex = 0, endIndex = length)

/**
 * The number of bytes needed to encode the entire array of characters in Modified UTF-8.
 *
 * This is an alias for [CharArray.mutf8Length] with `startIndex = 0` and `endIndex = size`. The selected indices are
 * guaranteed to be valid, so all [IllegalArgumentException]s documented at [CharArray.mutf8Length] are guaranteed not
 * to occur. Other than that, the contract & documentation for [CharArray.mutf8Length] applies here as well.
 *
 * @see[CharArray.mutf8Length]
 */
@get:JvmName("of")
val CharArray.mutf8Length: Long
    get() = mutf8Length(startIndex = 0, endIndex = size)

/**
 * The number of bytes needed to encode a range of the sequence's characters in Modified UTF-8.
 *
 * The "range" is defined as a [startIndex] and [endIndex], which behave the same as the first and second operands of
 * Kotlin's infix [until] function. Both indexes must be at least `0`, and the [startIndex] cannot be greater than the
 * [endIndex], though they can be equal to select `0` characters. [startIndex] must be less than the sequence's
 * [length][CharSequence.length] and [endIndex] must be less than *or* equal to [length][CharSequence.length]. If the
 * above conditions are not met, an [IllegalArgumentException] is thrown.
 *
 * If the returned [Long] exceeds `65535` ([UShort.MAX_VALUE]), then the sequence is too long to be encoded. This is
 * because a Modified UTF-8 string's length is encoded as an unsigned 16-bit (2-byte) integer. `65535` is the largest
 * value that can be represented given those limitations.
 *
 * The returned [Long] is equal to the sum of each character in the specified range, where their contributions to the
 * sum are as follows:
 * - Characters in the range `'\u0001' .. '\u007F'` add `1` to the sum
 * - Characters in the range `'\u0080' .. '\u07FF'` add `2` to the sum
 * - Characters in the range `'\u0800' .. '\uFFFF'` add `3` to the sum
 * - The character `'\u0000'` adds `2` to the sum
 *
 * That list covers the entire range of a [Char], so there are no other values to note.
 *
 * The returned [Long] will always be at least `0`, and does not account for the 2-byte length that typically precedes
 * Modified UTF-8 strings.
 *
 * To call this from Java sources:
 * ```java
 * Mutf8Length.of(chars, 0, chars.length())
 * ```
 *
 * @receiver The characters to determine the collective length of, in bytes.
 * @param[startIndex] The inclusive index in the [CharSequence] of the first character to be added to the sum.
 * Characters before this index will not be counted towards the sum, but ones at or after it (until [endIndex]) will be.
 * This must be at least equal to `0`, and at most equal to [CharSequence.lastIndex].
 * @param[endIndex] The exclusive index in the [CharSequence] of the last character to be added to the sum. Characters
 * at or after this index will not be counted toward the sum. It must be at least equal to [startIndex], and at most
 * equal to the [length][CharSequence.length] of the [CharSequence].
 * @return the number of bytes needed to represent the range of characters as a Modified UTF-8 sequence.
 *
 * @throws[IllegalArgumentException] if the [length][CharSequence.length] of the [CharSequence] is a negative number.
 * @throws[IllegalArgumentException] if the [startIndex] is a negative number.
 * @throws[IllegalArgumentException] if the [startIndex] exceeds the [lastIndex][CharSequence.lastIndex].
 * @throws[IllegalArgumentException] if the [endIndex] is a negative number.
 * @throws[IllegalArgumentException] if the [endIndex] exceeds the [length][CharSequence.length].
 * @throws[IllegalArgumentException] if the [startIndex] is greater than the [endIndex].
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
 * The number of bytes needed to encode a range of the array's characters in Modified UTF-8.
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
    while (hasNext()) {
        val char = nextChar()

        // "Characters in the range `'\u0800' .. '\uFFFF'` add `3` to the sum" (see KDoc comment)
        if (char >= '\u0800') lengthInBytes += 2

        // "Characters in the range `'\u0080' .. '\u07FF'` add `2` to the sum" (see KDoc comment)
        // "The character `'\u0000'` adds `2` to the sum"                      (see KDoc comment)
        else if (char >= '\u0080') lengthInBytes++
    }
    return lengthInBytes
}

private fun checkStartAndEndIndices(startIndex: Int, endIndex: Int, lengthInChars: Int) {
    require(lengthInChars >= 0) { "length must be at least 0, not $lengthInChars" }

    require(startIndex >= 0) { "startIndex must be at least 0, not $startIndex" }
    require(startIndex <= lengthInChars - 1) { "startIndex, $startIndex, must not exceed lastIndex, ${lengthInChars - 1}" }

    require(endIndex >= 0) { "endIndex must be at least 0, not $endIndex" }
    require(endIndex <= lengthInChars) { "endIndex, $endIndex, must not exceed length, $lengthInChars" }

    require(startIndex <= endIndex) { "startIndex, $startIndex, must not exceed endIndex, $endIndex" }
}