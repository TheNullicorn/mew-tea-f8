package me.nullicorn.mewteaf8

// Characters encoded using 1 byte each. This range contains 127 characters.
val singleByteOutputChars: Iterable<Char> = '\u0001'..'\u007F'

// Characters encoded using 2 bytes each. This range contains 1,921 characters.
val doubleByteOutputChars: Iterable<Char> = ('\u0080'..'\u07FF') + '\u0000'

// Characters encoded using 3 bytes each. There's too many to test them all (63,488), so we choose 1,000 at random.
val tripleByteOutputChars: Iterable<Char> = buildSet {
    // Always include the upper & lower bounds of the 3-byte char range.
    add('\u0800')
    add('\uFFFF')

    // Add 998 other randomly selected characters. `random` has a constant seed, so these will always be the same.
    addRandomlyWhile({ size < 1000 }, { random ->
        random.nextInt(from = '\u0800'.code, until = '\uFFFF'.code + 1).toChar()
    })
}