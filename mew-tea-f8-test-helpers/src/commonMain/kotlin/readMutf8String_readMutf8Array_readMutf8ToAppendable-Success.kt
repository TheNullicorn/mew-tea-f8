package me.nullicorn.mewteaf8

import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

abstract class ReadMutf8SuccessfullySharedTests<Input> : ReadMutf8Tests<Input> {

    @Test
    @JsName("A")
    fun `readMutf8 should correctly decode Modified UTF-8 strings preceded by their mutf8Lengths`() {
        for (charArray in sampleStrings)
            assertAllMethodsProduce(charArray, populateInput = {
                // Add the string's 2-byte `mutf8Length`.
                val mutf8Length = charArray.mutf8Length
                add((mutf8Length shr 8).toByte())
                add(mutf8Length.toByte())

                // Add the bytes of each character, in order.
                for (char in charArray)
                    addAllBytesOf(char)
            })
    }

    private fun assertAllMethodsProduce(characters: CharArray, populateInput: MutableList<Byte>.() -> Unit) {
        var input = Input(populateInput)
        assertContentEquals(expected = characters, actual = input.readMutf8Array())

        input = Input(populateInput)
        assertEquals(expected = characters.concatToString(), actual = input.readMutf8String())

        val destination = StringBuilder()
        input = Input(populateInput)
        input.readMutf8ToAppendable(destination)
        assertEquals(expected = characters.concatToString(), actual = destination.toString())
    }
}