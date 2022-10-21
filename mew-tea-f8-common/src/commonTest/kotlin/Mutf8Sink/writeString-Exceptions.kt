@file:Suppress("PackageName")

package me.nullicorn.mewteaf8.Mutf8Sink

import me.nullicorn.mewteaf8.createReproducableRandom
import me.nullicorn.mewteaf8.internal.InternalMutf8Api
import me.nullicorn.mewteaf8.internal.Mutf8IOException
import me.nullicorn.mewteaf8.internal.Mutf8Sink
import me.nullicorn.mewteaf8.mutf8Length
import kotlin.js.JsName
import kotlin.jvm.JvmName
import kotlin.test.Test
import kotlin.test.assertFailsWith

@OptIn(InternalMutf8Api::class)
class WriteStringExceptionsTests {

    @Test
    @JsName("A")
    fun `writeName should throw an IllegalArgumentException if bytesPerWrite is less than 1`() {
        val string = "mew-tea-f8"

        // Ensure that it throws when it's `0` or below.
        for (bytesPerWrite in (0 downTo -20) + Int.MIN_VALUE) {
            val sink = TestingMutf8Sink()

            assertFailsWith<IllegalArgumentException> {
                sink.writeString(string, bytesPerWrite)
            }
        }

        // Ensure that it does NOT throw when it's `1`.
        val sink = TestingMutf8Sink()
        sink.writeString(string, bytesPerWrite = 1)
    }

    @Test
    @JsName("B")
    fun `writeName should throw an IllegalArgumentException if the string's mutf8Length exceeds 65535`() {
        val maxMutf8Length = UShort.MAX_VALUE.toInt()
        val singleByteChar = '\u0041'
        val doubleByteChar = '\u00A7'
        val tripleByteChar = '\u2500'

        // Strings whose `mutf8Length`s are all equal to UShort.MAX_VALUE (65535).
        val longestStrings = setOf(
            // A string made up of the same 1-byte character.
            singleByteChar.toString().repeat(maxMutf8Length),

            // A string made up of the same 2-byte character.
            doubleByteChar.toString().repeat(maxMutf8Length / 2),

            // A string made up of the same 3-byte character.
            tripleByteChar.toString().repeat(maxMutf8Length / 3),

            // A random combination of all 3 characters.
            buildString {
                val charPool = setOf(singleByteChar, doubleByteChar, tripleByteChar)
                var mutf8Length = 0

                while (mutf8Length < maxMutf8Length) {
                    val char = when (maxMutf8Length - mutf8Length) {
                        1 -> singleByteChar
                        2 -> doubleByteChar
                        3 -> tripleByteChar
                        else -> charPool.random(random = createReproducableRandom())
                    }

                    mutf8Length += when (char) {
                        singleByteChar -> 1
                        doubleByteChar -> 2
                        else -> 3
                    }
                }
            }
        ).map {
            // If any of those strings fell short of `maxMutf8Length`, pad the end using the 1-byte character.
            it + singleByteChar.toString().repeat((maxMutf8Length - it.mutf8Length).toInt())
        }

        for (longestString in longestStrings) {
            // This SHOULD NOT throw an exception because the string's `mutf8Length` is exactly on the limit.
            BlackHoleMutf8Sink.writeString(longestString)

            val tooLongString = longestString + singleByteChar

            // This SHOULD throw an exception because the string's `mutf8Length` is now 1 byte over the limit.
            assertFailsWith<IllegalArgumentException> {
                BlackHoleMutf8Sink.writeString(tooLongString)
            }
        }
    }

    @Test
    @JsName("C")
    fun `writeString should propogate any Mutf8IOExceptions thrown by the writeBytes`() {
        val sink = object : Mutf8Sink {

            override fun writeBytes(bytes: ByteArray, untilIndex: Int) {
                throw Mutf8IOException("Test; this should NOT be caught by writeString()")
            }

            override fun writeLength(length: UShort) {
                throw UnsupportedOperationException("This method is not being tested")
            }
        }

        assertFailsWith<Mutf8IOException> {
            sink.writeString("mew-tea-f8")
        }
    }
}