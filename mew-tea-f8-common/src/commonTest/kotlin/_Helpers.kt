package me.nullicorn.mewteaf8

import kotlin.random.Random

fun createReproducableRandom() = Random(seed = "mew-tea-f8".hashCode())

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