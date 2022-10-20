@file:Suppress("PackageName")

package me.nullicorn.mewteaf8.ModifiedUtfByteSource

import me.nullicorn.mewteaf8.createReproducableRandom
import me.nullicorn.mewteaf8.internal.InternalMewTeaF8Api
import me.nullicorn.mewteaf8.internal.ModifiedUtf8ByteSource
import me.nullicorn.mewteaf8.internal.ModifiedUtf8EOFException

internal val singleByteSamples = '\u0000'..'\u007F'
internal val doubleByteSamples = '\u0000'..'\u07FF'
internal val tripleByteSamples = ('\u0000'..'\u07FF') + buildSet {
    add('\u0800')
    add('\uFFFF')

    val random = createReproducableRandom()
    while (size < 1000) {
        val threeByteChar = random.nextBits(Char.SIZE_BITS).toChar()
        if (threeByteChar in '\u0800'..'\uFFFF') add(threeByteChar)
    }
}

@InternalMewTeaF8Api
internal fun buildTestSource(builder: MutableList<Byte>.() -> Unit): TestingModifiedUtf8ByteSource =
    TestingModifiedUtf8ByteSource(bytes = buildList(builder).toByteArray())

@InternalMewTeaF8Api
internal class TestingModifiedUtf8ByteSource(bytes: ByteArray) : ModifiedUtf8ByteSource {

    private val bytes = bytes.copyOf()
    private var index = 0

    val size: Int
        get() = bytes.size

    override fun readBytes(amount: UShort): ByteArray {
        if (index + amount.toInt() > bytes.size)
            throw ModifiedUtf8EOFException("Not enough bytes left to read $amount")

        val chunk = bytes.copyOfRange(index, index + amount.toInt())
        index += amount.toInt()
        return chunk
    }

    override fun readUtfLength(): UShort {
        val byte1 = bytes[index++].toInt() and 0xFF
        val byte2 = bytes[index++].toInt() and 0xFF
        return ((byte1 shl 8) and byte2).toUShort()
    }
}