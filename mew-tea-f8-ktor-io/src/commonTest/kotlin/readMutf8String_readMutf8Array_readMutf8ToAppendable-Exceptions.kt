package me.nullicorn.mewteaf8

import io.ktor.utils.io.core.*
import kotlin.test.AfterTest
import me.nullicorn.mewteaf8.readMutf8Array as actual_readMutf8Array
import me.nullicorn.mewteaf8.readMutf8String as actual_readMutf8String
import me.nullicorn.mewteaf8.readMutf8ToAppendable as actual_readMutf8ToAppendable

class ReadMutf8ExceptionallyTests : ReadMutf8ExceptionallySharedTests<Input>() {

    private val inputs = HashSet<ByteReadPacket>()

    override val readMutf8String = Input::actual_readMutf8String

    override val readMutf8Array = Input::actual_readMutf8Array

    override val readMutf8ToAppendable = Input::actual_readMutf8ToAppendable

    override fun Input(bytes: List<Byte>): Input {
        val packet = ByteReadPacket(bytes.toByteArray())
        inputs += packet
        return packet
    }

    @AfterTest
    fun cleanUpPacketsAfterTest() {
        inputs.forEach { it.release() }
        inputs.clear()
    }
}