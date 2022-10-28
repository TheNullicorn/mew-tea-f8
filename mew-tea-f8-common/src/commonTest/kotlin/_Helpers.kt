package me.nullicorn.mewteaf8

import kotlin.random.Random

// Characters encoded using 1 byte each. This range contains 127 characters.
internal val singleByteChars: Iterable<Char> = '\u0001'..'\u007F'

// Characters encoded using 2 bytes each. This range contains 1,920 characters.
internal val doubleByteChars: Iterable<Char> = '\u0080'..'\u07FF'

// Characters encoded using 3 bytes each. There's too many to test them all (63,488), so we choose 1,000 at random.
internal val tripleByteChars: Iterable<Char> = buildSet {
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

internal val samples = listOf(
    CharArray(size = 0),
    "mew-tea-f8".toCharArray(),
    singleByteChars.toList().toCharArray().apply { shuffle(createReproducibleRandom()) },
    doubleByteChars.toList().toCharArray().apply { shuffle(createReproducibleRandom()) },
    tripleByteChars.toList().toCharArray().apply { shuffle(createReproducibleRandom()) },
    (singleByteChars + doubleByteChars + tripleByteChars).toCharArray().apply { shuffle(createReproducibleRandom()) }
)

fun createReproducibleRandom() = Random(seed = "mew-tea-f8".hashCode())

internal fun MutableList<Byte>.add1stOf1Bytes(char: Char) = add((char.code and 0x7F).toByte())

internal fun MutableList<Byte>.add1stOf2Bytes(char: Char) = add((char.code shr 6 and 0x1F or 0xC0).toByte())
internal fun MutableList<Byte>.add2ndOf2Bytes(char: Char) = add((char.code and 0x3F or 0x80).toByte())
internal fun MutableList<Byte>.add1stOf3Bytes(char: Char) = add((char.code shr 12 and 0x0F or 0xE0).toByte())

internal fun MutableList<Byte>.add2ndOf3Bytes(char: Char) = add((char.code shr 6 and 0x3F or 0x80).toByte())
internal fun MutableList<Byte>.add3rdOf3Bytes(char: Char) = add((char.code and 0x3F or 0x80).toByte())

private const val doubleByteChar = '\u00A7'
private const val tripleByteChar = '\u2500'

internal val malformedMutf8Bytes: Iterable<List<Byte>> = setOf(
    // A 2-byte character that's missing its 2nd byte.
    buildList {
        add1stOf2Bytes(doubleByteChar)
    },

    // A 3-byte character that's missing its 2nd and 3rd bytes.
    buildList {
        add1stOf3Bytes(tripleByteChar)
    },

    // A 3-byte character that's missing its 3rd byte.
    buildList {
        add1stOf3Bytes(tripleByteChar)
        add2ndOf3Bytes(tripleByteChar)
    },

    // A 2-byte character with all 4 of its most-significant bits set in the 1st byte.
    buildList {
        add((doubleByteChar.code shr 6 and 0x1F or 0b1111_0000).toByte())
        add2ndOf2Bytes(doubleByteChar)
    },

    // A 3-byte character with all 4 of its most-significant bits set in the 1st byte.
    buildList {
        add((tripleByteChar.code shr 12 and 0x0F or 0b1111_0000).toByte())
        add2ndOf3Bytes(tripleByteChar)
        add3rdOf3Bytes(tripleByteChar)
    },

    // A 2-byte character whose 2nd byte doesn't have the most-sigificant bits `10`.
    buildList {
        add1stOf2Bytes(doubleByteChar)
        add((doubleByteChar.code and 0x3F or 0b01_000000).toByte())
    },

    // A 3-byte character whose 2nd byte doesn't have the most-sigificant bits `10`.
    buildList {
        add1stOf3Bytes(tripleByteChar)
        add((tripleByteChar.code shr 6 and 0x3F or 0b01_000000).toByte())
        add3rdOf3Bytes(tripleByteChar)
    },

    // A 3-byte character whose 3rd byte doesn't have the most-sigificant bits `10`.
    buildList {
        add1stOf3Bytes(tripleByteChar)
        add2ndOf3Bytes(tripleByteChar)
        add((tripleByteChar.code and 0x3F or 0b01_000000).toByte())
    }
)