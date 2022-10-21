@file:JvmName("HelpersKtJvm")

package me.nullicorn.mewteaf8

import me.nullicorn.mewteaf8.Mutf8Sink.doubleByteOutputSamples
import me.nullicorn.mewteaf8.Mutf8Sink.singleByteOutputSamples
import me.nullicorn.mewteaf8.Mutf8Sink.tripleByteOutputSamples

internal val sampleStrings = setOf(
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