@file:Suppress("PackageName")

package me.nullicorn.mewteaf8.Mutf8Sink

import me.nullicorn.mewteaf8.createReproducableRandom
import me.nullicorn.mewteaf8.internal.InternalMutf8Api
import me.nullicorn.mewteaf8.internal.Mutf8Sink

internal val singleByteOutputSamples = '\u0001'..'\u007F'
internal val doubleByteOutputSamples = ('\u0080'..'\u07FF') + '\u0000'
internal val tripleByteOutputSamples = buildSet {
    add('\u0800')
    add('\uFFFF')

    val random = createReproducableRandom()
    while (size < 1000) {
        val threeByteChar = random.nextBits(Char.SIZE_BITS).toChar()
        if (threeByteChar in '\u0800'..'\uFFFF') add(threeByteChar)
    }
}

@InternalMutf8Api
object BlackHoleMutf8Sink : Mutf8Sink {
    override fun writeBytes(bytes: ByteArray, untilIndex: Int) {
        require(untilIndex >= 0) { "untilIndex must be at least 0, not $untilIndex" }
        require(untilIndex <= bytes.size) { "untilIndex must be at most bytes.size, ${bytes.size}, not $untilIndex" }

        // Don't do anything with the bytes.
    }

    override fun writeLength(length: UShort) {
        // Don't write anything.
    }

}

@InternalMutf8Api
internal class TestingMutf8Sink : Mutf8Sink {

    val bytes: List<Byte>
        get() = _bytes

    private val _bytes = mutableListOf<Byte>()

    override fun writeBytes(bytes: ByteArray, untilIndex: Int) {
        require(untilIndex >= 0) { "untilIndex must be at least 0, not $untilIndex" }
        require(untilIndex <= bytes.size) { "untilIndex must be at most bytes.size, ${bytes.size}, not $untilIndex" }

        this._bytes += bytes.copyOfRange(0, untilIndex).toList()
    }

    override fun writeLength(length: UShort) {
        this._bytes += (length.toInt() shr 8 and 0xFF).toByte()
        this._bytes += (length.toInt() /* */ and 0xFF).toByte()
    }
}