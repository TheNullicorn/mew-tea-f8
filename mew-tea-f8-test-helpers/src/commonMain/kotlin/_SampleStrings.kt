package me.nullicorn.mewteaf8

val sampleStrings = listOf(
    CharArray(size = 0),
    charArrayOf('\u0041'),
    charArrayOf('\u00A7'),
    charArrayOf('\u2500'),
    "mew-tea-f8".toCharArray(),
    singleByteOutputChars.toList().toCharArray().apply { shuffle(createReproducibleRandom()) },
    doubleByteOutputChars.toList().toCharArray().apply { shuffle(createReproducibleRandom()) },
    tripleByteOutputChars.toList().toCharArray().apply { shuffle(createReproducibleRandom()) },
    (singleByteOutputChars + doubleByteOutputChars + tripleByteOutputChars).toCharArray()
        .apply { shuffle(createReproducibleRandom()) }
)