package me.nullicorn.mewteaf8.Mutf8Sink

import me.nullicorn.mewteaf8.Mutf8Sink
import kotlin.test.AfterTest

abstract class Mutf8SinkTests<Sink : Mutf8Sink, Output> {

    private val sinksToDestinations = HashMap<Sink, Output>()

    protected abstract fun createSinkAndOutput(bytesPerWrite: Int): Pair<Sink, Output>

    protected abstract val Output.bytes: ByteArray

    protected fun Sink(bytesPerWrite: Int = 1024): Sink {
        val (sink, destination) = createSinkAndOutput(bytesPerWrite)
        sinksToDestinations[sink] = destination
        return sink
    }

    protected val Sink.bytes: ByteArray
        get() = sinksToDestinations[this@bytes]?.bytes
            ?: throw IllegalArgumentException("Test requested the bytes for a sink that wasn't created here, or one that was already used in a previous test")

    @AfterTest
    fun cleanUpTest() {
        sinksToDestinations.clear()
    }
}