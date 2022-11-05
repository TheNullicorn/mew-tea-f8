package me.nullicorn.mewteaf8.StreamMutf8Sink

import me.nullicorn.mewteaf8.Mutf8Sink.WriteCharactersSuccessfullyTests
import me.nullicorn.mewteaf8.StreamMutf8Sink
import java.io.ByteArrayOutputStream

class WriteCharactersSuccessfullyTests : WriteCharactersSuccessfullyTests<StreamMutf8Sink, ByteArrayOutputStream>() {

    override fun createSinkAndOutput(bytesPerWrite: Int): Pair<StreamMutf8Sink, ByteArrayOutputStream> {
        val stream = ByteArrayOutputStream()
        val sink = StreamMutf8Sink(stream, bytesPerWrite)
        return Pair(sink, stream)
    }

    override val ByteArrayOutputStream.bytes: ByteArray
        get() = this.toByteArray()
}