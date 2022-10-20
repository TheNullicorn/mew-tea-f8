package me.nullicorn.mewteaf8

import kotlin.random.Random

fun createReproducableRandom() = Random(seed = "mew-tea-f8".hashCode())

fun MutableList<Byte>.add1stOf1Bytes(char: Char) = add((char.code and 0x7F).toByte())

fun MutableList<Byte>.add1stOf2Bytes(char: Char) = add((char.code shr 6 and 0x1F or 0xC0).toByte())
fun MutableList<Byte>.add2ndOf2Bytes(char: Char) = add((char.code and 0x3F or 0x80).toByte())
fun MutableList<Byte>.add1stOf3Bytes(char: Char) = add((char.code shr 12 and 0x0F or 0xE0).toByte())

fun MutableList<Byte>.add2ndOf3Bytes(char: Char) = add((char.code shr 6 and 0x3F or 0x80).toByte())
fun MutableList<Byte>.add3rdOf3Bytes(char: Char) = add((char.code and 0x3F or 0x80).toByte())