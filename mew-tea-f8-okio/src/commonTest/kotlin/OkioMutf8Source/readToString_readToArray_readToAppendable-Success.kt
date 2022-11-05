package me.nullicorn.mewteaf8.OkioMutf8Source

import me.nullicorn.mewteaf8.Mutf8Source.ReadCharactersSuccessfullyTests
import me.nullicorn.mewteaf8.OkioMutf8Source
import okio.Buffer

class ReadCharactersSuccessfullyTests : ReadCharactersSuccessfullyTests<OkioMutf8Source>() {

    override fun Source(input: List<Byte>) =
        OkioMutf8Source(
            source = Buffer().apply {
                write(input.toByteArray())
            }
        )
}