package me.nullicorn.mewteaf8

import io.ktor.utils.io.core.*
import io.ktor.utils.io.errors.EOFException as KtorEOFException
import io.ktor.utils.io.errors.IOException as KtorIOException
import me.nullicorn.mewteaf8.EOFException as Mutf8EOFException
import me.nullicorn.mewteaf8.IOException as Mutf8IOException

/**
 * A [Mutf8Source] that reads its data from an [Input] from the ktor library.
 *
 * @param[input] The ktor [Input] to read from.
 */
class InputMutf8Source(private val input: Input) : Mutf8Source() {

    override fun readLength(): Int = tryOrThrowMutf8Exception {
        input.readShort(byteOrder = ByteOrder.BIG_ENDIAN).toInt() and 0xFFFF
    }

    override fun readBytes(amount: Int): ByteArray = tryOrThrowMutf8Exception {
        require(amount >= 0) { "amount must be at least 0, not $amount" }

        val bytes = ByteArray(size = amount)
        input.readFully(bytes, offset = 0, length = amount)

        return@tryOrThrowMutf8Exception bytes
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private inline fun <T> tryOrThrowMutf8Exception(block: () -> T): T =
        try {
            block()

        } catch (cause: KtorEOFException) {
            throw cause as? Mutf8EOFException
                ?: Mutf8EOFException(cause.message ?: "Data ended prematurely")

        } catch (cause: KtorIOException) {
            throw cause as? Mutf8IOException
                ?: Mutf8IOException(cause.message ?: "An I/O issue occurred while reading", cause)
        }
}