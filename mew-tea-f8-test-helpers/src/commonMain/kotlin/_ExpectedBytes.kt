package me.nullicorn.mewteaf8

fun MutableList<Byte>.add1stOf1Bytes(char: Char) = add((char.code /*      */ and 0x7F).toByte())

fun MutableList<Byte>.add1stOf2Bytes(char: Char) = add((char.code shr 6 /**/ and 0x1F or 0xC0).toByte())
fun MutableList<Byte>.add2ndOf2Bytes(char: Char) = add((char.code /*      */ and 0x3F or 0x80).toByte())

fun MutableList<Byte>.add1stOf3Bytes(char: Char) = add((char.code shr 12 /**/ and 0x0F or 0xE0).toByte())
fun MutableList<Byte>.add2ndOf3Bytes(char: Char) = add((char.code shr 6 /* */ and 0x3F or 0x80).toByte())
fun MutableList<Byte>.add3rdOf3Bytes(char: Char) = add((char.code /*       */ and 0x3F or 0x80).toByte())

fun MutableList<Byte>.addAllBytesOf(char: Char) =
    when (char) {
        in '\u0001'..'\u007F' -> {
            add1stOf1Bytes(char)
        }

        in '\u0080'..'\u07FF', '\u0000' -> {
            add1stOf2Bytes(char)
            add2ndOf2Bytes(char)
        }

        else -> {
            add1stOf3Bytes(char)
            add2ndOf3Bytes(char)
            add3rdOf3Bytes(char)
        }
    }