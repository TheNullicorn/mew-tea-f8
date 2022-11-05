package me.nullicorn.mewteaf8.OkioMutf8Sink

import me.nullicorn.mewteaf8.Mutf8Sink.WriteCharactersExceptionallyTests
import me.nullicorn.mewteaf8.OkioMutf8Sink
import okio.Buffer

class WriteCharactersExceptionallyTests : WriteCharactersExceptionallyTests<OkioMutf8Sink, Buffer>() {

    override fun createSinkAndOutput(bytesPerWrite: Int): Pair<OkioMutf8Sink, Buffer> {
        val buffer = Buffer()
        val sink = OkioMutf8Sink(buffer, bytesPerWrite)
        return Pair(sink, buffer)
    }

    override val Buffer.bytes: ByteArray
        get() = this.readByteArray()
}