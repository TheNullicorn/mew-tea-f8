package me.nullicorn.mewteaf8

import okio.BufferedSource
import okio.Source
import okio.buffer

/**
 * A [Mutf8Source] that reads its data from a [Source] from the okio library.
 *
 * @param[source] The okio [Source] to read from.
 */
class OkioMutf8Source(source: Source) : Mutf8Source() {

    private val source: BufferedSource = (source as? BufferedSource) ?: source.buffer()

    override fun readLength(): Int =
        source.readShort().toInt() and 0xFFFF

    override fun readBytes(amount: Int): ByteArray {
        require(amount >= 0) { "amount must be at least 0, not $amount" }

        val bytes = ByteArray(size = amount)
        source.readFully(bytes)
        return bytes
    }
}