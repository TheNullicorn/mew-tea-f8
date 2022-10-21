package me.nullicorn.mewteaf8

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.IllegalArgumentException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class WriteMutf8ContentsTests {

    @Test
    fun `writeMutf8Contents should throw an IllegalArgumentException if bytesPerWrite is less than 1`() {
        for (string in sampleStrings)
            for (bytesPerWrite in 0 downTo -20) {
                // We're not examining the output in this test, so we create a stream that discards all bytes.
                val stream = OutputStream.nullOutputStream()

                assertFailsWith<IllegalArgumentException> {
                    stream.writeMutf8Contents(string, bytesPerWrite)
                }
            }
    }

    @Test
    fun `writeMutf8Contents should throw an IOException if the OutputStream does`() {
        // Only use non-empty strings. Empty ones don't write anything, thus the exception is never thrown.
        for (string in sampleStrings.filter { it.isNotEmpty() }) {
            val stream = object : OutputStream() {
                override fun write(b: Int) {
                    throw IOException()
                }
            }

            assertFailsWith<IOException> {
                stream.writeMutf8Contents(string)
            }
        }
    }

    @Test
    fun `writeMutf8Contents should produce an output identical to the writeUTF method of Java's DataOutput`() {
        for (string in sampleStrings) {
            val actualEncoded = ByteArrayOutputStream().apply {
                writeMutf8Contents(string)
            }.toByteArray()

            // Encode the string using Java's built-in method. We skip the first 2 bytes because `writeUTF` encodes the
            // `mutf8Length` before the string, whereas `writeMutf8Contents` doesn't write the `mutf8Length` at all.
            val expectedEncoded = ByteArrayOutputStream().apply {
                DataOutputStream(this).writeUTF(string)
            }.toByteArray().let { it.copyOfRange(fromIndex = 2, toIndex = it.size) }

            assertContentEquals(expectedEncoded, actualEncoded)
        }
    }
}