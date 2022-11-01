package me.nullicorn.mewteaf8

import io.ktor.utils.io.core.*

/**
 * A [Mutf8Source] that reads its data from an [Input] from the ktor library.
 *
 * @param[input] The ktor [Input] to read from.
 */
class InputMutf8Source(private val input: Input) : Mutf8Source() {

    override fun readLength(): Int =
        input.readShort(byteOrder = ByteOrder.BIG_ENDIAN).toInt() and 0xFFFF

    override fun readBytes(amount: Int): ByteArray {
        require(amount >= 0) { "amount must be at least 0, not $amount" }

        val bytes = ByteArray(size = amount)
        input.readFully(bytes, offset = 0, length = amount)
        return bytes
    }
}