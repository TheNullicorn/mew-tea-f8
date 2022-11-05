package me.nullicorn.mewteaf8.Mutf8Source

import me.nullicorn.mewteaf8.addRandomlyWhile

// Characters that can be represented using 1 byte, including '\u0000' which is normally represented as 2 bytes but can
// be decoded if it's just 1 too.
val singleByteInputChars: Iterable<Char> = '\u0000'..'\u007F'

// Characters that can be represented using 2 bytes, including ones that are usually encoded as just 1 byte but can be
// still be decoded as 2.
val doubleByteInputChars: Iterable<Char> = '\u0000'..'\u07FF'

// Some characters that can be represented using 3 bytes, including ones that are usually encoded as just 1 or 2 bytes
// but can be still be decoded as 3. We only include 1000 actual 3-byte characters because the range is way too wide to
// test them all.
val tripleByteInputChars: Iterable<Char> = ('\u0000'..'\u07FF') + buildSet {
    // Always include the upper & lower bounds of the 3-byte char range.
    add('\u0800')
    add('\uFFFF')

    // Add 998 other randomly selected characters. `random` has a constant seed, so these will always be the same.
    addRandomlyWhile({ size < 1000 }, { random ->
        random.nextInt(from = '\u0800'.code, until = '\uFFFF'.code + 1).toChar()
    })
}