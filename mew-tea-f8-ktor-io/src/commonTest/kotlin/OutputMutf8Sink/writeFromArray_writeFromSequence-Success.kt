package me.nullicorn.mewteaf8.OutputMutf8Sink

import io.ktor.utils.io.core.*
import me.nullicorn.mewteaf8.Mutf8Sink.WriteCharactersSuccessfullyTests
import me.nullicorn.mewteaf8.OutputMutf8Sink

class WriteCharactersSuccessfullyTests : WriteCharactersSuccessfullyTests<OutputMutf8Sink, BytePacketBuilder>() {

    override fun createSinkAndOutput(bytesPerWrite: Int): Pair<OutputMutf8Sink, BytePacketBuilder> {
        val output = BytePacketBuilder()
        val sink = OutputMutf8Sink(output, bytesPerWrite)
        return Pair(sink, output)
    }

    override val BytePacketBuilder.bytes: ByteArray
        get() = this.build().readBytes()
}