@file:Suppress("PackageName")

package me.nullicorn.mewteaf8.Mutf8Source

import me.nullicorn.mewteaf8.createReproducableRandom
import me.nullicorn.mewteaf8.internal.InternalMutf8Api
import me.nullicorn.mewteaf8.internal.Mutf8Source
import me.nullicorn.mewteaf8.internal.Mutf8EOFException

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

@InternalMutf8Api
internal fun buildTestSource(builder: MutableList<Byte>.() -> Unit): TestingMutf8Source =
    TestingMutf8Source(bytes = buildList(builder).toByteArray())

@InternalMutf8Api
internal class TestingMutf8Source(bytes: ByteArray) : Mutf8Source {

    private val bytes = bytes.copyOf()
    private var index = 0

    val size: Int
        get() = bytes.size

    override fun readBytes(amount: UShort): ByteArray {
        if (index + amount.toInt() > bytes.size)
            throw Mutf8EOFException("Not enough bytes left to read $amount")

        val chunk = bytes.copyOfRange(index, index + amount.toInt())
        index += amount.toInt()
        return chunk
    }

    override fun readLength(): UShort {
        val byte1 = bytes[index++].toInt() and 0xFF
        val byte2 = bytes[index++].toInt() and 0xFF
        return ((byte1 shl 8) and byte2).toUShort()
    }
}