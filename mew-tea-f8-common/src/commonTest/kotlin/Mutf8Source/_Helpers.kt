package me.nullicorn.mewteaf8.Mutf8Source

import me.nullicorn.mewteaf8.EOFException
import me.nullicorn.mewteaf8.Mutf8Source
import me.nullicorn.mewteaf8.createReproducibleRandom
import kotlin.test.assertTrue

// Characters that can be represented using 1 byte, including '\u0000' which is normally represented as 2 bytes but can
// be decoded if it's just 1 too.
internal val singleByteInputChars: Iterable<Char> = '\u0000'..'\u007F'

// Characters that can be represented using 2 bytes, including ones that are usually encoded as just 1 byte but can be
// still be decoded as 2.
internal val doubleByteInputChars: Iterable<Char> = '\u0000' .. '\u07FF'

// Some characters that can be represented using 3 bytes, including ones that are usually encoded as just 1 or 2 bytes
// but can be still be decoded as 3. We only include 1000 actual 3-byte characters because the range is way too wide to
// test them all.
internal val tripleByteInputChars: Iterable<Char> = ('\u0000' .. '\u07FF') + buildSet {
    // Always include the upper & lower bounds of the 3-byte char range.
    add('\u0800')
    add('\uFFFF')

    // Add 998 other randomly selected characters. `random` has a constant seed, so these will always be the same.
    val random = createReproducibleRandom()
    while (size < 1000) {
        val char = random.nextBits(bitCount = 16).toChar()
        if (char in '\u0800'..'\uFFFF') add(char)
    }
}

/**
 * Creates a [ByteListMutf8Source] whose [bytes][ByteListMutf8Source.bytes] are initialized by the supplied [populate]
 * function.
 */
internal inline operator fun ByteListMutf8Source.Companion.invoke(populate: MutableList<Byte>.() -> Unit) =
    ByteListMutf8Source(bytes = ArrayList<Byte>().apply(populate))

/**
 * A [ByteListMutf8Source] whose [bytes] come from an in-memory [List] that's supplied on construction. This is intended
 * purely for testing purposes.
 */
internal class ByteListMutf8Source(private val bytes: List<Byte> = emptyList()) : Mutf8Source() {

    // Allows us to create "static" extension functions off this class.
    internal companion object;

    private var index = 0

    override fun readBytes(amount: Int): ByteArray {
        // Assert that the `amount` is
        // 1. At least `0`, because negative amounts wouldn't make sense
        // 2. At most `65535`, because no Modified UTF-8 string can be longer than that many bytes.
        assertTrue(amount in 0..65535, "amount=$amount")

        if (index + amount > bytes.size)
            throw EOFException("$amount bytes were expected, but only ${bytes.size - index} are left")

        val firstIndex = index
        index += amount
        return bytes.subList(fromIndex = firstIndex, toIndex = index).toByteArray()
    }

    override fun readLength(): Int {
        if (index + 2 > bytes.size)
            throw EOFException("2 bytes were expected, but only ${bytes.size - index} are left")

        val byte1 = bytes[index++].toInt() and 0xFF
        val byte2 = bytes[index++].toInt() and 0xFF
        return (byte1 shl 8) or byte2
    }
}