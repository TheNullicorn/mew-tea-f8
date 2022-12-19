package me.nullicorn.mewteaf8

import java.io.OutputStream

/**
 * A [Mutf8Sink] that writes its data to an [OutputStream].
 *
 * @param[stream] The [OutputStream] to write to.
 * @param[bytesPerWrite] The maximum number of bytes that will be copied to the sink's underlying destination at a time.
 * This must be at least `1`, though higher is recommended.
 *
 * @throws[IllegalArgumentException] if [bytesPerWrite] is less than `1`.
 */
class StreamMutf8Sink(private val stream: OutputStream, bytesPerWrite: Int = 1024) : Mutf8Sink(bytesPerWrite) {

    override fun writeLength(mutf8Length: Int) {
        require(mutf8Length >= 0) { "mutf8Length must be at least 0, not $mutf8Length" }
        require(mutf8Length <= 65535) { "mutf8Length must be at most 65535, not $mutf8Length" }

        // Write the `mutf8Length` as 2 bytes in big-endian order. The `and 0xFF` is done implicitly by `write()`.
        stream.write(mutf8Length shr 8)
        stream.write(mutf8Length)
    }

    override fun writeBytes(bytes: ByteArray, untilIndex: Int) {
        require(untilIndex >= 0) { "untilIndex must be at least 0, not $untilIndex" }
        require(untilIndex <= bytes.size) { "untilIndex, $untilIndex, must not exceed bytes.size, ${bytes.size}" }

        stream.write(bytes, /* off = */ 0, /* len = */ untilIndex)
    }
}