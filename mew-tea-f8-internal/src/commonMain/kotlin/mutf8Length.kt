@file:JvmName("ModifiedUtf8Length")

package me.nullicorn.mewteaf8

import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * The number of bytes needed to encode the string's characters in Modified UTF-8.
 *
 * If this exceeds `65535` ([UShort.MAX_VALUE]), then the string is too long to be encoded. This is because a Modified
 * UTF-8 string's length is encoded as an unsigned 16-bit/2-byte integer. The aforementioned number is the largest that
 * can be represented with that many bits.
 *
 * This is equal to the sum of each of the string's characters, where their contributions to the sum are as follows:
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
 * ModifiedUtf8Length.of(chars)
 * ```
 *
 * @receiver The characters to determine the collective length of, in bytes.
 */
@get:JvmName("of")
@get:JvmSynthetic
val CharSequence.mutf8Length: Long
    get() {
        // Prevent custom `CharSource` implementations from messing us up with weird `length` values.
        val lengthInChars = this.length
        if (lengthInChars < 0)
            throw IllegalArgumentException("length cannot be a negative number: $lengthInChars")

        // Each character will use at least 1 byte, so we start with that to minimize the amount of operations.
        var lengthInBytes = lengthInChars.toLong()

        for (i in 0 until lengthInChars) {
            val char: Char = this[i]

            // "Characters in the range `'\u0800' .. '\uFFFF'` add `3` to the sum" (see KDoc comment)
            if (char >= '\u0800') lengthInBytes += 2

            // "Characters in the range `'\u0080' .. '\u07FF'` add `2` to the sum" (see KDoc comment)
            // "The character `'\u0000'` adds `2` to the sum"                      (see KDoc comment)
            else if (char >= '\u0080' || char == '\u0000') lengthInBytes++
        }

        return lengthInBytes
    }