@file:Suppress("unused", "FunctionName")

package me.nullicorn.mewteaf8

import kotlinx.benchmark.*
import kotlinx.benchmark.BenchmarkTimeUnit.*
import java.util.concurrent.TimeUnit.NANOSECONDS
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.experimental.and
import kotlin.math.ceil
import kotlin.random.Random
import kotlin.random.nextInt

/**
 * Benchmarks the performance of various implementations of [mutf8Length].
 */
@State(Scope.Benchmark)
@OutputTimeUnit(NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 3, time = 5, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = SECONDS)
class Mutf8LengthBenchmark {

    private val samples: List<String> = listOf(
        // Some short sample strings.
        "mew-tea-f8",
        "Nullicorn\u00A7f: \u00A78Hello, World!",

        // 20 characters from each of the 3 ranges (1-byte, 2-byte, 3-byte).
        randomString(20, '\u0001'..'\u007F'),
        randomString(20, '\u0080'..'\u07FF'),
        randomString(20, '\u0800'..'\uFFFF'),

        // 1,000 characters from each of the 3 ranges.
        randomString(1000, '\u0001'..'\u007F'),
        randomString(1000, '\u0080'..'\u07FF'),
        randomString(1000, '\u0800'..'\uFFFF'),

        // A mostly-ASCII string with some 3-byte characters sprinkled in.
        randomString(1000, '\u0001'..'\u007F').sprinkledWith('\u0800'..'\uFFFF', frequency = 0.3f),

        // Completely random strings of characters from all 3 ranges.
        randomString(100),
        randomString(500),
        randomString(1000),
        randomString(5000),
        randomString(10000),
        randomString(25000)
    )

    @Param("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14")
    var sampleIndex = -1
    var sample = ""
    var sampleLength = -1

    @Setup
    fun setUp() {
        sample = samples[sampleIndex]
        sampleLength = sample.length
    }

    //region Benchmarks

    @Benchmark
    fun mutf8Length_with_greaterThan(blackhole: Blackhole) {
        var mutf8Length = sampleLength.toLong()
        var i = 0

        // Implementation that uses greater-than comparisons to determine the size of each `Char`.
        while (i != sampleLength) {
            val char = sample[i++]

            // Characters in the range `'\u0800' .. '\uFFFF'` add `3` to the sum
            if (char >= '\u0800') mutf8Length += 2

            // Characters in the range `'\u0080' .. '\u07FF'` add `2` to the sum
            // The character `'\u0000'` adds `2` to the sum
            else if (char >= '\u0080' || char == '\u0000') mutf8Length++
        }

        blackhole.consume(mutf8Length)
    }

    @Benchmark
    fun mutf8Length_with_countLeadingZeroBits(blackhole: Blackhole) {
        var mutf8Length = sampleLength.toLong()
        var i = 0

        // Implementation that uses `countLeadingZeroBits()` to determine the size of each `Char`.
        while (i != sampleLength)
            when (sample[i++].code.toShort().countLeadingZeroBits()) {
                // Characters in the range `'\u0080' .. '\u07FF'` add `2` to the sum
                // The character `'\u0000'` adds `2` to the sum
                16, 8, 7, 6, 5 -> mutf8Length++

                // Characters in the range `'\u0800' .. '\uFFFF'` add `3` to the sum
                4, 3, 2, 1, 0 -> mutf8Length += 2
            }

        blackhole.consume(mutf8Length)
    }

    @Benchmark
    fun mutf8Length_with_takeHighestOneBit(blackhole: Blackhole) {
        var mutf8Length = sampleLength.toLong()
        var i = 0

        // Implementation that uses `takeHighestOneBit()` to determine the size of each `Char`.
        while (i != sampleLength)
            when (sample[i++].code.toShort().takeHighestOneBit()) {
                // Characters in the range `'\u0080' .. '\u07FF'` add `2` to the sum
                // The character `'\u0000'` adds `2` to the sum
                0x0000.toShort(),
                0x0080.toShort(),
                0x0100.toShort(),
                0x0200.toShort(),
                0x0400.toShort() -> mutf8Length++

                // Characters in the range `'\u0800' .. '\uFFFF'` add `3` to the sum
                0x8000.toShort(),
                0x4000.toShort(),
                0x2000.toShort(),
                0x1000.toShort(),
                0x0800.toShort() -> mutf8Length += 2
            }

        blackhole.consume(mutf8Length)
    }

    @Benchmark
    fun mutf8Length_with_bitwise_and(blackhole: Blackhole) {
        var mutf8Length = sampleLength.toLong()
        var i = 0

        // Implementation that uses the bitwise `and` operator to determine the size of each `Char`.
        while (i != sampleLength) {
            val code = sample[i++].code.toShort()

            // Characters in the range `'\u0001` .. '\u007F'` add `1` to the sum
            // This is already accounted for when `mutf8Length` is initialized.
            if (code and 0x7F == code && code != 0.toShort()) continue

            // Characters in the range `'\u0080' .. '\u07FF'` add `2` to the sum
            // The character `'\u0000'` adds `2` to the sum
            else if (code and 0x7FF == code) mutf8Length++

            // Characters in the range `'\u0800' .. '\uFFFF'` add `3` to the sum
            else mutf8Length += 2
        }

        blackhole.consume(mutf8Length)
    }

    @Benchmark
    fun mutf8Length_with_bitwise_shift(blackhole: Blackhole) {
        var mutf8Length = sampleLength.toLong()
        var i = 0

        // Implementation that uses bit shifting to determine the size of each `Char`.
        while (i != sampleLength) {
            val code = sample[i++].code

            // Characters in the range `'\u0800' .. '\uFFFF'` add `3` to the sum
            if (code ushr 11 != 0) mutf8Length += 2

            // Characters in the range `'\u0080' .. '\u07FF'` add `2` to the sum
            // The character `'\u0000'` adds `2` to the sum
            else if (code ushr 7 != 0 || code == 0) mutf8Length++
        }


        blackhole.consume(mutf8Length)
    }

    //endregion
    //region Helpers

    private fun createReproducibleRandom(): Random = Random(seed = "mew-tea-f8".hashCode())

    private fun randomString(length: Int, range: CharRange? = null): String {
        val random = createReproducibleRandom()

        return buildString(capacity = length) {
            for (i in 0 until length) {
                val bits =
                    if (range != null) random.nextInt(range.first.code..range.last.code)
                    else random.nextBits(Char.SIZE_BITS)

                append(bits.toChar())
            }
        }
    }

    private fun String.sprinkledWith(chars: Iterable<Char>, frequency: Float): String {
        require(frequency in 0.0..1.0) { "frequency must be between 0 and 1, not $frequency" }

        val random = createReproducibleRandom()
        val charsToSprinkle = chars.toList()
        val result = this.toCharArray()

        // Using the `frequency` as a percentage of the string's `length`, choose that many indices to replace the
        // string's characters with the sprinkled ones.
        val indicesToReplace = buildSet {
            val indicesNeeded = ceil(result.size * frequency)

            while (size < indicesNeeded)
                add(random.nextInt(until = result.size))
        }

        // Insert the sprinkled characters at the randomly chosen indices.
        for (index in indicesToReplace)
            result[index] = charsToSprinkle.random(random)

        // Turn the array back into a string & return it.
        return result.concatToString()
    }

    //endregion
}