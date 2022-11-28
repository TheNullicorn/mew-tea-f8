package me.nullicorn.mewteaf8

///////////////////////////////////////////////////////////////////////////
// NOTICE:
// This file is not included in the library at compile-time or run-time; it
// merely holds sample code included in the documentation for the library.
///////////////////////////////////////////////////////////////////////////

class Mutf8LengthSamples {

    /**
     * Sample code snippet for the documentation of [Char.mutf8Length].
     */
    fun Char_mutf8Length() {
        /*
         * Because 'A' is an ASCII character, its `mutf8Length` will always be 1. This is the case for all characters
         * whose codes (`Char.code`) are in the range `1 .. 127` (`0x01 .. 0x7F`). 'A' is in this range because its code
         * is 65 (0x41).
         *
         * Output:
         * 1
         */
        println('A'.mutf8Length)

        /*
         * The `mutf8Length` of '§' always be 2. This is the case for all characters whose codes are in the range
         * `128 .. 2047` (`0x80 .. 0x7FF`), and the ASCII NUL character ('\u0000'), whose code is 0. '§' is in this
         * range because its code is 167 (0xA7).
         *
         * Output:
         * 2
         */
        println('§'.mutf8Length)

        /*
         * The `mutf8Length` of '✨' always be 3. This is the case for all characters whose codes are in the range
         * `2048 .. 65535` (`0x800 .. 0xFFFF`). '✨' is in this range because its code is 10024 (0x2728).
         *
         * Output:
         * 3
         */
        println('✨'.mutf8Length)
    }

    /**
     * Sample code snippet for the documentation of the [CharSequence.mutf8Length] property.
     */
    fun CharSequence_mutf8Length_entire() {
        /*
         * The string "Hello, World!" is made up of 13 ASCII characters whose individual `mutf8Length` values are each
         * 1. So, the `mutf8Length` of the entire string is 13.
         *
         * Output:
         * 13
         */
        println("""Hello, World!""".mutf8Length)

        /*
         * The string below is made up of the following characters:
         * - 'ツ' (aka '\u30C4') (x1) - `mutf8Length` is 3 each
         * - '¯'  (aka '\u00AF') (x2) - `mutf8Length` is 2 each
         * - '_'  (aka '\u005F') (x2) - `mutf8Length` is 1 each
         * - '\'  (aka '\u005C') (x1) - `mutf8Length` is 1 each
         * - '/'  (aka '\u002F') (x1) - `mutf8Length` is 1 each
         * - '('  (aka '\u0028') (x1) - `mutf8Length` is 1 each
         * - ')'  (aka '\u0029') (x1) - `mutf8Length` is 1 each
         *
         * Given the `mutf8Length` of each individual character and how many times each is found in the string, the
         * `mutf8Length` of the entire thing is 13.
         *
         * Output:
         * 13
         */
        println("""¯\_(ツ)_/¯""".mutf8Length)
    }

    /**
     * Sample code snippet for the documentation of the [CharSequence.mutf8Length] function.
     */
    fun CharSequence_mutf8Length_range() {
        /*
         * The string below is made up of the following characters:
         * - 'ツ' (aka '\u30C4') (x1) - `mutf8Length` is 3 each
         * - '¯'  (aka '\u00AF') (x2) - `mutf8Length` is 2 each
         * - '_'  (aka '\u005F') (x2) - `mutf8Length` is 1 each
         * - '\'  (aka '\u005C') (x1) - `mutf8Length` is 1 each
         * - '/'  (aka '\u002F') (x1) - `mutf8Length` is 1 each
         * - '('  (aka '\u0028') (x1) - `mutf8Length` is 1 each
         * - ')'  (aka '\u0029') (x1) - `mutf8Length` is 1 each
         */
        val string = """¯\_(ツ)_/¯"""

        /*
         * By default, `mutf8Length()` returns that of the entire string. The `startIndex` is 0 to tell it to start
         * from (and including) the first character, and `endIndex` is the `lastIndex + 1` (aka `length`) to tell it to
         * end after the last character. All 3 lines below will produce the same output.
         *
         * Output:
         * 13
         * 13
         * 13
         */
        println(string.mutf8Length(startIndex = 0, endIndex = string.lastIndex + 1))
        println(string.mutf8Length())
        println(string.mutf8Length)

        /*
         * Now, we'll get the `mutf8Length` of a smaller range of the string's characters. Specifically, our range
         * includes the parentheses and the face inside. In other words, "(ツ)".
         *
         * Given the `mutf8Length` of each of those characters and that they each occur just once in that range of the
         * string, the `mutf8Length` of the range is 5.
         *
         * Output:
         * 5
         */
        println(string.mutf8Length(startIndex = 3, endIndex = 6))
    }
}