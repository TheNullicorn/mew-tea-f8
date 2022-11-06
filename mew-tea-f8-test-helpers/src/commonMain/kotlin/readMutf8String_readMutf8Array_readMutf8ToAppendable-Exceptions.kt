package me.nullicorn.mewteaf8

import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertFailsWith

abstract class ReadMutf8ExceptionallySharedTests<Input> : ReadMutf8Tests<Input> {

    @Test
    @JsName("A")
    fun `readMutf8 should throw an EOFException if there are not enough bytes to read the 2-byte mutf8Length`() {
        for (numberOfBytes in 0 until 2)
            assertAllMethodsFailWith<EOFException>(populateInput = {
                for (i in 0 until numberOfBytes)
                    add(0)
            })
    }

    @Test
    @JsName("B")
    fun `readMutf8 should throw an EOFException if there are fewer bytes after the mutf8Length than expected`() {
        for (mutf8Length in setOf(1, 2, 100, 65535))
            for (actualNumberOfBytes in setOf(0, mutf8Length - 1))
                assertAllMethodsFailWith<EOFException>(populateInput = {
                    // Write the `mutf8Length` as a 2-byte unsigned integer.
                    add((mutf8Length shr 8).toByte())
                    add(mutf8Length.toByte())

                    // Write the actual # of bytes, which is less than the `mutf8Length` indicates.
                    addAll(Array(size = actualNumberOfBytes, init = { 0 }))
                })
    }

    @Test
    @JsName("C")
    fun `readMutf8 should throw a UTFDataFormatException if the bytes that make up the characters are malformed`() {
        for (charArray in sampleStrings)
            for (malformedCharBytes in malformedMutf8Bytes)
                for (malformedCharIndex in setOf(0, charArray.size / 2, charArray.lastIndex + 1)) {
                    assertAllMethodsFailWith<UTFDataFormatException>(populateInput = {
                        // Add the correct bytes of the array's characters, and the malformed bytes at the chosen index.
                        for (i in charArray.indices) {
                            addAll(malformedCharBytes)
                            addAllBytesOf(charArray[i])
                        }

                        // If the chosen index was at the very end of the array, add them now because the loop didn't.
                        if (malformedCharIndex == charArray.lastIndex + 1)
                            addAll(malformedCharBytes)

                        // Before all the characters, insert the 2-byte `mutf8Length` now that we know its value.
                        val mutf8Length = size
                        add(index = 0, element = (mutf8Length shr 8).toByte())
                        add(index = 1, element = mutf8Length.toByte())
                    })
                }
    }

    private inline fun <reified E : Throwable> assertAllMethodsFailWith(noinline populateInput: MutableList<Byte>.() -> Unit) {
        var input = Input(populateInput)
        assertFailsWith<E> {
            input.readMutf8Array()
        }

        input = Input(populateInput)
        assertFailsWith<E> {
            input.readMutf8String()
        }

        val destination = StringBuilder()
        input = Input(populateInput)
        assertFailsWith<E> {
            input.readMutf8ToAppendable(destination)
        }
    }
}