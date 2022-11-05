package me.nullicorn.mewteaf8.OkioMutf8Source

import me.nullicorn.mewteaf8.Mutf8Source.ReadCharactersExceptionallyTests
import me.nullicorn.mewteaf8.OkioMutf8Source
import okio.Buffer

class ReadCharactersExceptionallyTests : ReadCharactersExceptionallyTests<OkioMutf8Source>() {

    override fun Source(input: List<Byte>) =
        OkioMutf8Source(
            source = Buffer().apply {
                write(input.toByteArray())
            }
        )
}