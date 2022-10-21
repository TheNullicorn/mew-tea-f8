package me.nullicorn.mewteaf8

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadMutf8ContentsSuccessfullyTests {

    @Test
    fun `readMutf8Contents should successfully decode strings encoded by Java's writeUTF method in DataOutput`() {
        for (string in sampleStrings) {
            val encoded = ByteArrayOutputStream()
            val dataOut = DataOutputStream(encoded)

            // Use Java's built-in method to encode the string into the `ByteArrayOutputStream`.
            dataOut.writeUTF(string)

            val stream: InputStream = ByteArrayInputStream(encoded.toByteArray())

            // writeUTF also writes the 2-byte "UTF Length" before the string, so we read that manually first.
            val mutf8Length: Int = (stream.read() and 0xFF shl 8) or (stream.read() and 0xFF)

            assertEquals(expected = string, actual = stream.readMutf8Contents(mutf8Length.toUShort()))
        }
    }
}