package me.nullicorn.mewteaf8.Mutf8Sink

import me.nullicorn.mewteaf8.Mutf8Sink
import me.nullicorn.mewteaf8.addRandomlyWhile
import kotlin.js.JsName
import kotlin.test.*

abstract class WriteLengthTests<Sink : Mutf8Sink, Destination> : Mutf8SinkTests<Sink, Destination>() {

    @Test
    @JsName("A")
    fun `writeLength should throw an IllegalArgumentException if mutf8Length is negative`() {
        for (negativeMutf8Length in (-16..-1) + Int.MIN_VALUE) {
            val sink = Sink()

            assertFailsWith<IllegalArgumentException> {
                sink.writeLength(negativeMutf8Length)
            }
        }
    }

    @Test
    @JsName("B")
    fun `writeLength should throw an IllegalArgumentException if mutf8Length exceeds 65535`() {
        for (tooHighMutf8Length in (65536..65551) + Int.MAX_VALUE) {
            val sink = Sink()

            assertFailsWith<IllegalArgumentException> {
                sink.writeLength(tooHighMutf8Length)
            }
        }
    }

    @Test
    @JsName("C")
    fun `writeLength should not throw an IllegalArgumentException if mutf8Length is exactly 65535`() {
        val sink = Sink()

        try {
            sink.writeLength(mutf8Length = 65535)
        } catch (cause: IllegalArgumentException) {
            fail("writeLength() threw an IAE for mutf8Length=65535, which should've been acceptable", cause)
        } catch (cause: Throwable) {
            fail("writeLength() threw a ${cause::class.simpleName}, but only IAE was being tested", cause)
        }
    }

    @Test
    @JsName("D")
    fun `writeLength should write the mutf8Length as 2 bytes in big-endian order`() {
        // Choose 200 random `mutf8Length`s to test. This always includes `0` and `65535`, our upper and lower limits.
        val mutf8Lengths = buildSet {
            add(0)
            add(65535)
            addRandomlyWhile({ size < 200 }, { random ->
                random.nextInt(from = 0, until = 65536)
            })
        }

        for (mutf8Length in mutf8Lengths) {
            val sink = Sink()
            sink.writeLength(mutf8Length)

            assertContentEquals(
                expected = byteArrayOf(
                    (mutf8Length shr 8 and 0xFF).toByte(),
                    (mutf8Length /* */ and 0xFF).toByte()
                ),
                actual = sink.bytes
            )
        }
    }
}