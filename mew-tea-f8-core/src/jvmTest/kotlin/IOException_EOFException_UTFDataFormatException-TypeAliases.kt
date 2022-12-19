package me.nullicorn.mewteaf8

import org.junit.Test
import kotlin.test.assertEquals

class ExceptionTypeAliasesTests {

    @Test
    fun `IOException typealias should point to java_io_IOException`() {
        assertEquals(
            expected = java.io.IOException::class.java,
            actual = me.nullicorn.mewteaf8.IOException::class.java
        )
    }

    @Test
    fun `EOFException typealias should point to java_io_EOFException`() {
        assertEquals(
            expected = java.io.EOFException::class.java,
            actual = me.nullicorn.mewteaf8.EOFException::class.java
        )
    }

    @Test
    fun `UTFDataFormatException typealias should point to java_io_UTFDataFormatException`() {
        assertEquals(
            expected = java.io.UTFDataFormatException::class.java,
            actual = me.nullicorn.mewteaf8.UTFDataFormatException::class.java
        )
    }
}