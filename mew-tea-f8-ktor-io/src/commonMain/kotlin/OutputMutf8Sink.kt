package me.nullicorn.mewteaf8

import io.ktor.utils.io.core.*
import io.ktor.utils.io.errors.IOException as KtorIOException
import me.nullicorn.mewteaf8.IOException as Mutf8IOException

/**
 * A [Mutf8Sink] that writes its data to an [Output] from the ktor library.
 *
 * @param[output] The ktor [Output] to write to.
 * @param[bytesPerWrite] The maximum number of bytes that will be copied to the sink's underlying destination at a time.
 * This must be at least `1`, though higher is recommended.
 *
 * @throws[IllegalArgumentException] if [bytesPerWrite] is less than `1`.
 */
class OutputMutf8Sink(private val output: Output, bytesPerWrite: Int = 1024) : Mutf8Sink(bytesPerWrite) {

    override fun writeLength(mutf8Length: Int): Unit =
        tryOrThrowMutf8Exception {
            require(mutf8Length >= 0) { "mutf8Length must be at least 0, not $mutf8Length" }
            require(mutf8Length <= 65535) { "mutf8Length must be at most 65535, not $mutf8Length" }

            output.writeShort(mutf8Length.toShort(), byteOrder = ByteOrder.BIG_ENDIAN)
        }

    override fun writeBytes(bytes: ByteArray, untilIndex: Int): Unit =
        tryOrThrowMutf8Exception {
            require(untilIndex >= 0) { "untilIndex must be at least 0, not $untilIndex" }
            require(untilIndex <= bytes.size) { "untilIndex, $untilIndex, must not exceed bytes.size, ${bytes.size}" }

            output.writeFully(bytes, offset = 0, length = untilIndex)
        }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private inline fun <T> tryOrThrowMutf8Exception(block: () -> T): T =
        try {
            block()

        } catch (cause: KtorIOException) {
            throw cause as? Mutf8IOException
                ?: Mutf8IOException(cause.message ?: "An I/O issue occurred while writing", cause)
        }
}