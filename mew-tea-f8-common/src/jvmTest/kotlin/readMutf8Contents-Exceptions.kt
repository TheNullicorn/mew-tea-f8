package me.nullicorn.mewteaf8

import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.UTFDataFormatException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ReadMutf8ContentsExceptionsTests {

    @Test
    fun `readMutf8Contents should throw an IOException if the InputStream throws one`() {
        for (mutf8Length in 1..20) {
            val stream = object : InputStream() {
                override fun read(): Int {
                    throw IOException()
                }
            }

            assertFailsWith<IOException> {
                stream.readMutf8Contents(mutf8Length.toUShort())
            }
        }
    }

    @Test
    fun `readMutf8Contents should throw an EOFException if the InputStream throws one`() {
        for (mutf8Length in 1..20) {
            val stream = object : InputStream() {
                override fun read(): Int {
                    throw EOFException()
                }
            }

            assertFailsWith<EOFException> {
                stream.readMutf8Contents(mutf8Length.toUShort())
            }
        }
    }

    @Test
    fun `readMutf8Contents should throw a UTFDataFormatException if the string's contents are malformed`() {
        for (bytes in malformedMutf8Bytes) {
            val stream = ByteArrayInputStream(bytes.toByteArray())

            assertFailsWith<UTFDataFormatException> {
                stream.readMutf8Contents(bytes.size.toUShort())
            }
        }
    }
}