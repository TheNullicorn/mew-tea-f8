package me.nullicorn.mewteaf8.InputMutf8Source

import io.ktor.utils.io.core.*
import me.nullicorn.mewteaf8.InputMutf8Source
import me.nullicorn.mewteaf8.Mutf8Source.ReadCharactersSuccessfullyTests
import kotlin.test.AfterTest

class ReadCharactersSuccessfullyTests : ReadCharactersSuccessfullyTests<InputMutf8Source>() {

    private val inputs = HashSet<ByteReadPacket>()

    override fun Source(input: List<Byte>): InputMutf8Source {
        val packet = ByteReadPacket(input.toByteArray())
        inputs += packet

        return InputMutf8Source(packet)
    }

    @AfterTest
    fun cleanUpPacketsAfterTest() {
        inputs.forEach { it.release() }
        inputs.clear()
    }
}