package me.nullicorn.mewteaf8

import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

class Mutf8LengthWithRangeSuccessTests {

    @Test
    @JsName("A")
    fun `mutf8Length should return 0 if the startIndex and endIndex are equal`() {
        for (charArray in sampleStrings)
            for (index in charArray.indices.toSet() + 0) {
                assertEquals(
                    expected = 0,
                    actual = charArray.mutf8Length(startIndex = index, endIndex = index)
                )

                assertEquals(
                    expected = 0,
                    actual = charArray.concatToString().mutf8Length(startIndex = index, endIndex = index)
                )
            }
    }

    @Test
    @JsName("B")
    fun `mutf8Length should return the mutf8Length of the entire sequence if startIndex is 0 and endIndex equals its size or length`() {
        for (charArray in sampleStrings) {
            assertEquals(
                expected = charArray.mutf8Length,
                actual = charArray.mutf8Length(startIndex = 0, endIndex = charArray.size)
            )

            val string = charArray.concatToString()
            assertEquals(
                expected = string.mutf8Length,
                actual = string.mutf8Length(startIndex = 0, endIndex = string.length)
            )
        }
    }

    @Test
    @JsName("C")
    fun `mutf8Length should only count characters whose indices are at least the startIndex and at most the endIndex - 1`() {
        val random = createReproducibleRandom()

        for (charArray in sampleStrings.filter { it.isNotEmpty() })
            for (i in 0..20) {
                val startIndex = random.nextInt(until = charArray.size)
                val endIndex = random.nextInt(from = startIndex, until = charArray.size + 1)
                val charsInRange = charArray.copyOfRange(startIndex, endIndex)

                // The `mutf8Length()` of the range should be the same as if we isolated that range (`copyOfRange()`)
                // and took its entire `mutf8Length` using the property-getter instead of the function overload.
                assertEquals(
                    expected = charsInRange.mutf8Length,
                    actual = charArray.mutf8Length(startIndex, endIndex)
                )

                // Same, but with the `CharSequence` overload.
                assertEquals(
                    expected = charsInRange.concatToString().mutf8Length,
                    actual = charArray.concatToString().mutf8Length(startIndex, endIndex)
                )
            }
    }
}