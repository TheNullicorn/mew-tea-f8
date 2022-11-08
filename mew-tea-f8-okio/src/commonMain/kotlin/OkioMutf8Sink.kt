package me.nullicorn.mewteaf8

import okio.BufferedSink
import okio.Sink
import okio.buffer
import me.nullicorn.mewteaf8.IOException as Mutf8IOException
import okio.IOException as OkioIOException

/**
 * A [Mutf8Sink] that writes its data to a [Sink] from the okio library.
 *
 * @param[sink] The okio [Sink] to write to.
 * @param[bytesPerWrite] The maximum number of bytes that will be copied to the sink's underlying destination at a time.
 * This must be at least `1`, though higher is recommended.
 *
 * @throws[IllegalArgumentException] if [bytesPerWrite] is less than `1`.
 */
class OkioMutf8Sink(sink: Sink, bytesPerWrite: Int = 1024) : Mutf8Sink(bytesPerWrite) {

    private val sink: BufferedSink = (sink as? BufferedSink) ?: sink.buffer()

    override fun writeLength(mutf8Length: Int): Unit =
        tryOrThrowMutf8Exception {
            require(mutf8Length >= 0) { "mutf8Length must be at least 0, not $mutf8Length" }
            require(mutf8Length <= 65535) { "mutf8Length must be at most 65535, not $mutf8Length" }

            sink.writeShort(mutf8Length)
        }

    override fun writeBytes(bytes: ByteArray, untilIndex: Int): Unit =
        tryOrThrowMutf8Exception {
            require(untilIndex >= 0) { "untilIndex must be at least 0, not $untilIndex" }
            require(untilIndex <= bytes.size) { "untilIndex, $untilIndex, must not exceed bytes.size, ${bytes.size}" }

            sink.write(bytes, offset = 0, byteCount = untilIndex)
        }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private inline fun <T> tryOrThrowMutf8Exception(block: () -> T): T =
        try {
            block()

        } catch (cause: OkioIOException) {
            throw cause as? Mutf8IOException
                ?: Mutf8IOException(cause.message ?: "An I/O issue occurred while writing", cause)
        }
}