package me.nullicorn.mewteaf8

import okio.BufferedSource
import okio.Source
import okio.buffer
import me.nullicorn.mewteaf8.EOFException as Mutf8EOFException
import me.nullicorn.mewteaf8.IOException as Mutf8IOException
import okio.EOFException as OkioEOFException
import okio.IOException as OkioIOException

/**
 * A [Mutf8Source] that reads its data from a [Source] from the okio library.
 *
 * @param[source] The okio [Source] to read from.
 */
class OkioMutf8Source(source: Source) : Mutf8Source() {

    private val source: BufferedSource = (source as? BufferedSource) ?: source.buffer()

    override fun readLength(): Int =
        tryOrThrowMutf8Exception {
            source.readShort().toInt() and 0xFFFF
        }

    override fun readBytes(amount: Int): ByteArray =
        tryOrThrowMutf8Exception {
            require(amount >= 0) { "amount must be at least 0, not $amount" }

            val bytes = ByteArray(size = amount)
            source.readFully(bytes)
            return bytes
        }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private inline fun <T> tryOrThrowMutf8Exception(block: () -> T): T =
        try {
            block()

        } catch (cause: OkioEOFException) {
            throw cause as? Mutf8EOFException
                ?: Mutf8EOFException(cause.message ?: "Data ended prematurely")

        } catch (cause: OkioIOException) {
            throw cause as? Mutf8IOException
                ?: Mutf8IOException(cause.message ?: "An I/O issue occurred while reading", cause)
        }
}