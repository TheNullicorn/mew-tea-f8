package me.nullicorn.mewteaf8

import me.nullicorn.mewteaf8.Mutf8Sink.doubleByteOutputSamples
import me.nullicorn.mewteaf8.Mutf8Sink.singleByteOutputSamples
import me.nullicorn.mewteaf8.Mutf8Sink.tripleByteOutputSamples
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadMutf8ContentsSuccessfullyTests {

    @Test
    fun `readMutf8Contents should successfully decode strings encoded by Java's writeUTF method in DataOutput`() {
        val samples = setOf(
            // An empty string.
            CharArray(size = 0).concatToString(),

            // Generic string of 1-byte (ASCII) characters.
            "mew-tea-f8",

            // Our sample characters. We're using the "output" samples instead of the "input" ones because we first
            // have to *write* them via `DataOutputStream`.
            singleByteOutputSamples.shuffled(random = createReproducableRandom()).joinToString(separator = ""),
            doubleByteOutputSamples.shuffled(random = createReproducableRandom()).joinToString(separator = ""),
            tripleByteOutputSamples.shuffled(random = createReproducableRandom()).joinToString(separator = ""),
        )

        for (sample in samples) {
            val encoded = ByteArrayOutputStream()
            val dataOut = DataOutputStream(encoded)

            // Use Java's built-in method to encode the string into the `ByteArrayOutputStream`.
            dataOut.writeUTF(sample)

            val stream: InputStream = ByteArrayInputStream(encoded.toByteArray())

            // writeUTF also writes the 2-byte "UTF Length" before the string, so we read that manually first.
            val mutf8Length: Int = (stream.read() and 0xFF shl 8) or (stream.read() and 0xFF)

            assertEquals(expected = sample, actual = stream.readMutf8Contents(mutf8Length.toUShort()))
        }
    }
}