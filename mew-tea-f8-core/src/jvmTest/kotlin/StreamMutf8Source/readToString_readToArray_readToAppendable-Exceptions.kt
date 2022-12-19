package me.nullicorn.mewteaf8.StreamMutf8Source

import me.nullicorn.mewteaf8.Mutf8Source.ReadCharactersExceptionallyTests
import me.nullicorn.mewteaf8.StreamMutf8Source
import java.io.ByteArrayInputStream

class ReadCharactersExceptionallyTests : ReadCharactersExceptionallyTests<StreamMutf8Source>() {
    override fun Source(input: List<Byte>) = 
        StreamMutf8Source(stream = ByteArrayInputStream(input.toByteArray()))
}