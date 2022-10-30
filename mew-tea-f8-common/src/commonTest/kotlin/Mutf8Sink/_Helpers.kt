package me.nullicorn.mewteaf8.Mutf8Sink

import me.nullicorn.mewteaf8.Mutf8Sink
import kotlin.test.assertTrue

internal object BlackHoleMutf8Sink : Mutf8Sink() {
    override fun writeBytes(bytes: ByteArray, untilIndex: Int) {
        assertTrue(untilIndex >= 0, message = "untilIndex=$untilIndex")
        assertTrue(untilIndex <= bytes.size, message = "untilIndex=$untilIndex,bytes.size=${bytes.size}")

        // Do nothing with the bytes.
    }
}

internal class ByteListMutf8Sink(bytesPerWrite: Int = 1024) : Mutf8Sink(bytesPerWrite) {

    val bytes: List<Byte>
        get() = _bytes

    private val _bytes = ArrayList<Byte>()

    override fun writeBytes(bytes: ByteArray, untilIndex: Int) {
        assertTrue(untilIndex >= 0, message = "untilIndex=$untilIndex")
        assertTrue(untilIndex <= bytes.size, message = "untilIndex=$untilIndex,bytes.size=${bytes.size}")

        // Add the bytes to our list.
        val bytesToWrite = bytes.copyOf(newSize = untilIndex).toList()
        this._bytes += bytesToWrite
    }
}