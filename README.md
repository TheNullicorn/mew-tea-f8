# 😽 mew-tea-f8 🍵

A tiny library for reading & writing Modified UTF-8 binary sequences in Kotlin, be it with Java's [streams][api-java],
Okio's [sinks and sources][api-okio], or Ktor's [inputs and outputs][api-ktor].

**<p align="center"><u><a href="#installation">
[Click here for installation steps]
</a></u></p>**

[api-java]: https://docs.oracle.com/javase/8/docs/api/java/io/package-summary.html

[api-okio]: https://square.github.io/okio/3.x/okio/okio/okio

[api-ktor]: https://api.ktor.io/ktor-io/io.ktor.utils.io.core/index.html

---

## Usage

**Examples**

- [Reading Strings](#reading-strings)
    - [Kotlin](#in-kotlin)
    - [Java](#in-java)
- [Writing Strings](#writing-strings)
    - [Kotlin](#in-kotlin-1)
    - [Java](#in-java-1)
- [Calculating Binary Sizes](#calculating-binary-sizes)
    - [Kotlin](#in-kotlin-2)
    - [Java](#in-java-2)
- [Using Sources & Destinations](#using-sources--destinations)
    - [Kotlin](#in-kotlin-3)
      - [Using a Mutf8Source](#using-a-mutf8source)
    - [Java](#in-java-3)
      - [Using a Mutf8Source](#using-a-mutf8source-1)
- [Custom Sources & Destination](#custom-sources--destinations)
    - [Kotlin](#in-kotlin-4)
        - [Mutf8Source Implementation](#mutf8source-implementation)
        - [Mutf8Sink Implementation](#mutf8sink-implementation)
    - [Java](#in-java-4)
        - [Mutf8Source Implementation](#mutf8source-implementation-1)
        - [Mutf8Sink Implementation](#mutf8sink-implementation-1)

### Reading Strings

The examples below use `java.io.InputStream`, but if you're working with the okio or ktor equivalents, those same
functions work with the `Source` and `Input` classes from those libraries respectively.

**NOTE:** If you're using this library from `.java` source files with a different I/O library, you'll need to change
`me.nullicorn.mewteaf8.Mutf8InputStreams` to the appropriate class:

- For okio, that would be `me.nullicorn.mewteaf8.Mutf8OkioSources`
- For ktor, that would be `me.nullicorn.mewteaf8.Mutf8KtorInputs`

#### In Kotlin

```kotlin
import me.nullicorn.mewteaf8.readMutf8Array
import me.nullicorn.mewteaf8.readMutf8String
import me.nullicorn.mewteaf8.readMutf8ToAppendable

//...

// The next bytes in this hypothetical stream are, in order, as hexadecimal:
// 00 0D C2 AF 5C 5F 28 E3 83 84 29 5F 2F C2 AF
// That's the binary Modified UTF-8 for our shrugging friend, """¯\_(ツ)_/¯"""
val stream: InputStream // = ...

// To read the entire string, you have 3 choices:

// 1. To read the characters to a regular String:
val string: String = stream.readMutf8String()
//  string == """¯\_(ツ)_/¯"""

// 2. To read it to a new CharArray (or char[] in Java):
val chars: CharArray = stream.readMutf8Array()
//  chars contentEquals """¯\_(ツ)_/¯""".toCharArray()

// 3. To append the characters to an existing Appendable (like a StringBuilder):
val charCollector: Appendable = StringBuilder()
stream.readMutf8ToAppendable(charCollector)
//  charCollector.toString() == """¯\_(ツ)_/¯"""
```

[(back to top ↑)](#usage)

#### In Java

```java
import me.nullicorn.mewteaf8.Mutf8InputStreams;
import java.io.InputStream;

final class Mutf8Demo {
    public static void main(String... args) {
        // The next bytes in this hypothetical stream are, in order, as hexadecimal:
        // 00 0D C2 AF 5C 5F 28 E3 83 84 29 5F 2F C2 AF
        InputStream stream;

        // To read the entire string, you have 3 choices:

        // 1. To read the characters to a regular String:
        String string = Mutf8InputStreams.readStringFrom(stream);
        //     string.equals("¯\\_(ツ)_/¯")

        // 2. To read it to a new char[]:
        char[] chars = Mutf8InputStreams.readArrayFrom(stream); // == new char[]{ '¯', '\\', '_', '(', 'ツ', ')', '_', '/', '¯' }
        //     java.util.Arrays.equals(chars, "¯\\_(ツ)_/¯".toCharArray())

        // 3. To append the characters to an existing Appendable (like a StringBuilder):
        Appendable charCollector = new StringBuilder();
        Mutf8InputStreams.readToAppendableFrom(stream, charCollector);
        //     charCollector.toString().equals("¯\\_(ツ)_/¯")
    }
}
```

[(back to top ↑)](#usage)

### Writing Strings

The examples below use `java.io.OutputStream`, but if you're working with the okio or ktor equivalents, those same
functions work with the `Sink` and `Output` classes from those libraries respectively.

**NOTE:** If you're using this library from `.java` source files with a different I/O library, you'll need to change
`me.nullicorn.mewteaf8.Mutf8OutputStreams` to the appropriate class:

- For okio, that would be `me.nullicorn.mewteaf8.Mutf8OkioSinks`
- For ktor, that would be `me.nullicorn.mewteaf8.Mutf8KtorOutputs`

#### In Kotlin

```kotlin
import java.io.OutputStream
import me.nullicorn.mewteaf8.writeMutf8Array
import me.nullicorn.mewteaf8.writeMutf8Sequence

// ...

// The stream we'll be writing to. If you want to mess around with this on your own, make it a
// java.io.ByteArrayOutputStream and see how the result of stream.toByteArray() changes with different strings.
val stream: OutputStream // = ...

val shrugString: CharSequence = """¯\_(ツ)_/¯"""
val shrugArray: CharArray = shrugString.toCharArray()

// Modified UTF-8 data can be written to the stream in a few ways. For our shrugString/shrugArray above, these will all
// produce the following bytes to the stream in order, as hexadecimal:
// 00 0D C2 AF 5C 5F 28 E3 83 84 29 5F 2F C2 AF

// 1. If you're starting with a String (or some other CharSequence, like a StringBuilder), use writeMutf8Sequence():
stream.writeMutf8Sequence(shrugString)

// 2. If you're starting with a CharArray, use writeMutf8Array():
stream.writeMutf8Array(shrugArray)

// If you only want to write a portion of the CharSequence/CharArray, like just the "(ツ)" of our shrugString/shrugArray,
// you can define a specific range, either as two Ints or a single IntRange. All 4 lines below will produce the
// following bytes to the stream in order, as hexadecimal:
// 00 05 28 E3 83 84 29
stream.writeMutf8Sequence(shrugString, range = 3 until 6)
stream.writeMutf8Sequence(shrugString, startIndex = 3, endIndex = 6)
stream.writeMutf8Array(shrugArray, range = 3 until 6)
stream.writeMutf8Array(shrugArray, startIndex = 3, endIndex = 6)

// For fine-tuning your I/O efficiency (if you're working in a situation where that's critical), you can specify the
// bytesPerWrite, which is the (maximum) number of bytes the functions will flush to the stream at a time. In other
// words, it's the size of the in-memory buffer characters are serialized to before being flushed/written to the stream.
// If you know you'll be writing smaller strings, you can set this lower to save memory. On the other hand, if you know
// you'll be writing long strings, you can set this higher to reduce the number of flushes/writes. Currently, this
// defaults arbitrarily to 1 KiB (1024 bytes), though that is not concrete and could change in future releases at the
// discretion of contributors.
//
// That parameter can be used with both writeMutf8Array() and writeMutf8Sequence(), with and without specifying a range.
//
// For example, to write the "(ツ)" part of the string to the stream 1 byte at-a-time (not recommended, purely for the
// sake of the example):
stream.writeMutf8Sequence(shrugString, range = 3 until 6, bytesPerWrite = 1)
// That call produces the following separate flushes/writes:
// 00 05
// 28
// E3
// 83
// 84
// 29
```

[(back to top ↑)](#usage)

#### In Java

```java
import java.io.OutputStream;
import me.nullicorn.mewteaf8.Mutf8OutputStreams;

final class Mutf8Demo {
    public static void main(String... args) {
        // The stream we'll be writing to. If you want to mess around with this on your own, make it a
        // java.io.ByteArrayOutputStream and see how the result of stream.toByteArray() changes with different strings.
        OutputStream stream; // = ...

        CharSequence shrugString = "¯\\_(ツ)_/¯";
        char[] shrugArray = shrugString.toString().toCharArray();

        // Modified UTF-8 data can be written to the stream in a few ways. For our shrugString/shrugArray above, these
        // will all produce the following bytes to the stream in order, as hexadecimal:
        // 00 0D C2 AF 5C 5F 28 E3 83 84 29 5F 2F C2 AF

        // 1. If you're starting with a String (or some other CharSequence, like a StringBuilder), use
        // Mutf8OutputStreams.writeSequenceTo():
        Mutf8OutputStreams.writeSequenceTo(stream, shrugString);

        // 2. If you're starting with a CharArray, use Mutf8OutputStreams.writeArrayTo():
        Mutf8OutputStreams.writeArrayTo(stream, shrugArray);

        // If you only want to write a portion of the CharSequence/CharArray, like just the "(ツ)" of our
        // shrugString/shrugArray, you can define a specific range as two Ints . Both lines below will produce the
        // following bytes to the stream in order, as hexadecimal:
        // 00 05 28 E3 83 84 29
        Mutf8OutputStreams.writeSequenceTo(stream, shrugString, /* startIndex = */ 3, /* endIndex = */ 6);
        Mutf8OutputStreams.writeArrayTo(stream, shrugArray, /* startIndex = */ 3, /* endIndex = */ 6);

        // For fine-tuning your I/O efficiency (if you're working in a situation where that's critical), you can specify
        // the bytesPerWrite, which is the (maximum) number of bytes the functions will flush to the stream at a time.
        // In other words, it's the size of the in-memory buffer characters are serialized to before being
        // flushed/written to the stream. If you know you'll be writing smaller strings, you can set this lower to save
        // memory. On the other hand, if you know you'll be writing long strings, you can set this higher to reduce the
        // number of flushes/writes. Currently, this defaults arbitrarily to 1 KiB (1024 bytes), though that is not
        // concrete and could change in future releases at the discretion of contributors.
        //
        // That parameter can be used with both writeArrayTo() and writeSequenceTo(), with and without specifying a
        // range.
        //
        // For example, to write the "(ツ)" part of the string to the stream 1 byte at-a-time (not recommended, purely
        // for the sake of the example):
        Mutf8OutputStreams.writeSequenceTo(stream, shrugString, /* startIndex = */ 3, /* endIndex = */ 6, /* bytesPerWrite = */ 1);
        // That call produces the following separate flushes/writes:
        // 00 05
        // 28
        // E3
        // 83
        // 84
        // 29
    }
}
```

[(back to top ↑)](#usage)

### Calculating Binary Sizes

The `length` of a String (often) can differ from the actual number of bytes used to encode it as Modified UTF-8 binary.
But if you're the one serializing those strings, there may be times when you'll need to know the string's encoded size
without having to actually encode it, such as to calculate the serialized size of an in-memory structure (that contains
the strings) beforehand.

We refer to that binary size as the `mutf8Length` (Modified UTF-8 Length). It **does not** account for the 2-byte length
field that comes before most Modified UTF-8 strings' characters—the `mutf8Length` *is* the value of those 2 bytes. If
you need to account for those additional 2 bytes, just add `2` to your `mutf8Length`.

#### In Kotlin

```kotlin
// You can use the `mutf8Length` extension property directly on a Char to get its individual binary size, or the number
// of bytes that each instance of the character would take up in an encoded string. It will always be either 1, 2, or 3:
//
// Returns `1` because the codepoint of 'A' is U+0041, which is inside the range defined for 1-byte characters,
// `'\u0001' .. '\u007F'`.
'A'.mutf8Length
//
// Returns `2` because the codepoint of '§' is U+00A7, which is inside the range defined for 2-byte characters,
// `('\u0080' .. '\u07FF') + '\u0000'`.
'§'.mutf8Length
//
// Returns `3` because the codepoint of '✨' is U+2728, which is inside the range defined for 3-byte characters,
// `'\u0800' .. '\uFFFF'`.
'✨'.mutf8Length

// Most likely you won't need the sizes of individual characters, but entire Strings or CharArrays. For that, the same
// extension function can be used to efficiently sum up the mutf8Length's of a sequence of characters. Specifically,
// this property exists for Chars, CharSequences, and CharArrays.
//
// Returns 3 because each character's mutf8Length is 1:
"XYZ".mutf8Length
//
// Returns 6 because each character's mutf8Length is 2:
"§§§".mutf8Length
//
// Returns 9 because each character's mutf8Length is 3:
"✨✨✨".mutf8Length
//
// Returns 6 because the characters' mutf8Lengths are 1, 2 and 3 respectively, for a total of 6:
"A§✨".mutf8Length

// As with writing sequences, you can also call mutf8Length() as a function to specify a specific range of indices to
// calculate a sequence's mutf8Length for. This only works for CharSequence and CharArray, not Char, because a Char is
// individual; it has no range to select from.
//
// With no arguments, mutf8Length() behaves the same as the property (see previous examples). Both of these return 13
// because:
// - The "hands" (first and last chars) are 2 bytes each because their codes are U+00A7
// - The slashes are 1 byte each because their codes are U+005C and U+002F for '\\' and '/' respectively
// - The underscores are 1 byte each because their code is U+005F
// - The parentheses are 1 byte each because their codes are U+0028 and U+0029 for '(' and ')' respectively
// - The face/symbol in the middle is 3 bytes because its code is U+30C4
"""¯\_(ツ)_/¯""".mutf8Length
"""¯\_(ツ)_/¯""".mutf8Length()
//
// Now let's get the mutf8Length of just the "(ツ)" part of the string, which should be 5 (1 for each parenthesis, 3 for
// the symbol/face inside):
"""¯\_(ツ)_/¯""".mutf8Length(startIndex = 3, endIndex = 6)
```

[(back to top ↑)](#usage)

#### In Java

```java
import me.nullicorn.mewteaf8.Mutf8Length;

final class Mutf8Demo {
    public static void main(String... args) {
        // You can use the `Mutf8Length.of()` function on a Char to get its individual binary size, or the number of
        // bytes that each instance of the character would take up in an encoded string. It will always be either 1, 2,
        // or 3:
        //
        // Returns `1` because the codepoint of 'A' is U+0041, which is inside the range defined for 1-byte characters,
        // '\u0001' through '\u007F'.
        Mutf8Length.of('A');
        //
        // Returns `2` because the codepoint of '§' is U+00A7, which is inside the range defined for 2-byte characters,
        // '\u0080' through '\u07FF', and also '\u0000'.
        Mutf8Length.of('§');
        //
        // Returns `3` because the codepoint of '✨' is U+2728, which is inside the range defined for 3-byte characters,
        // '\u0800' through '\uFFFF'.
        Mutf8Length.of('✨');

        // Most likely you won't need the sizes of individual characters, but entire Strings or CharArrays. For that,
        // the same function can be used to efficiently sum up the mutf8Length's of a sequence of characters.
        // Specifically, this property exists for Chars, CharSequences, and CharArrays.
        //
        // Returns 3 because each character's mutf8Length is 1:
        Mutf8Length.of("XYZ");
        //
        // Returns 6 because each character's mutf8Length is 2:
        Mutf8Length.of("§§§");
        //
        // Returns 9 because each character's mutf8Length is 3:
        Mutf8Length.of("✨✨✨");
        //
        // Returns 6 because the characters' mutf8Lengths are 1, 2 and 3 respectively, for a total of 6:
        Mutf8Length.of("A§✨");

        // As with writing sequences, you can also call mutf8Length() as a function to specify a specific range of
        // indices to calculate a sequence's mutf8Length for. This only works for CharSequence and CharArray, not Char,
        // because a Char is individual; it has no range to select from.
        //
        // Take our shrugging friend from our other examples; their full mutf8Length is 13 because:
        // - The "hands" (first and last chars) are 2 bytes each because their codes are U+00A7
        // - The slashes are 1 byte each because their codes are U+005C and U+002F for '\\' and '/' respectively
        // - The underscores are 1 byte each because their code is U+005F
        // - The parentheses are 1 byte each because their codes are U+0028 and U+0029 for '(' and ')' respectively
        // - The face/symbol in the middle is 3 bytes because its code is U+30C4
        Mutf8Length.of("¯\\_(ツ)_/¯");
        //
        // Now let's get the mutf8Length of just the "(ツ)" part of the string. The result will be 5: 1 for each
        // parenthesis, 3 for the symbol/face inside):
        Mutf8Length.of("¯\\_(ツ)_/¯", /* startIndex = */ 3, /* endIndex = */ 6);
    }
}
```

[(back to top ↑)](#usage)

### Using Sources & Destinations

The library's primary classes for encoding & decoding are `Mutf8Source` (for reading/decoding/receiving data) and
`Mutf8Sink` (for writing/encoding/sending data). In fact, the extension functions shown previously use those classes
under the hood. If you take a look at their source code, you'll see that all they do is create a source/sink, write/read
a string's length, and then write/read a string's characters.

Public implementations of those classes exist for working with java.io, okio, and ktor, and you can find those classes
in the respective module's documentation. For example, the java.io interface includes the classes `StreamMutf8Sink` and
`StreamMutf8Source`.

Using those classes gives you slightly more control over what gets read/written, plus allows you to create custom
implementations so that you can use our encoder/decoder with your own I/O library
([see here for examples](#custom-sources--destinations)).

#### In Kotlin

##### Using a Mutf8Source

```kotlin
import java.io.InputStream
import me.nullicorn.mewteaf8.Mutf8Source
import me.nullicorn.mewteaf8.StreamMutf8Source

// ...

// The next bytes in this hypothetical stream are, in order, as hexadecimal:
// 00 0D C2 AF 5C 5F 28 E3 83 84 29 5F 2F C2 AF
val inputStream: InputStream // = ...
// We can then pass that stream to a StreamMutf8Source to start reading the Modified UTF-8 data it contains.
val source: Mutf8Source = StreamMutf8Source(stream = inputStream)

// First, we'll need to read the length of the string, or the first 2 bytes of data. We need this number so that we know
// how many bytes we'll need to read to get the entire encoded string.
//
// This is its own method to allow for flexibility in the encoding/decoding process. For example, if you want to have
// strings' lengths in a table at a different place in the data, then associate lengths to strings on your own. Or, if
// all your strings are the same size, encoding/decoding the length would be wasted time & space, so it could be omitted
// altogether.
//
// In our case, the bytes are 00 0D. If we combine those in big-endian order into an unsigned integer, we get 13. If you
// look back at the bytes, that's exactly how many come after the first 2.
val mutf8Length: Int = source.readLength()
//  mutf8Length == 13

// Now, we can read the contents of the string: its characters. Mutf8Sources can be read from in 3 different ways:
//
// 1. As a plain String:
val decodedString: String = source.readToString(mutf8Length)
//  decodedString == """¯\_(ツ)_/¯"""
//
// 2. As a CharArray (or char[] in Java):
val decodedChars: CharArray = source.readToArray(mutf8Length)
//  decodedChars contentEquals """¯\_(ツ)_/¯""".toCharArray()
//
// 3. To the end of an existing Appendable, like a StringBuilder:
val charCollector: Appendable = StringBuilder()
source.readToAppendable(mutf8Length, destination = charCollector)
//  charCollector.toString() == """¯\_(ツ)_/¯"""
```

[(back to top ↑)](#usage)

##### Using a Mutf8Sink

```kotlin
import java.io.OutputStream
import me.nullicorn.mewteaf8.mutf8Length
import me.nullicorn.mewteaf8.Mutf8Sink
import me.nullicorn.mewteaf8.StreamMutf8Sink

// ...

val outputStream: OutputStream // = ...
val sink = StreamMutf8Sink(stream = outputStream)

val shrugString = """¯\_(ツ)_/¯"""
val shrugArray = shrugString.toCharArray()

// If you're writing Modified UTF-8 data that should be readable by receivers other than your own app, you'll need to
// write each sequence's mutf8Length directly before the characters themselves.
//
// This is its own method to allow for flexibility in the encoding/decoding process. For example, if you want to have
// strings' lengths in a table at a different place in the data, then associate lengths to strings on your own. Or, if
// all your strings are the same size, encoding/decoding the length would be wasted time & space, so it could be omitted
// altogether.
//
// Anyway, we can use the `mutf8Length` extension property to get our shrugString's entire binary size. The same
// extension would also work on our shrugArray.
sink.writeLength(shrugString.mutf8Length.toInt())
// The mutf8Length of our string/array is 13. In big-endian hexadecimal, that would be 00 0D, so those are the 2
// bytes written by that call.

// Next is to write the characters themselves. Mutf8Sink gives us 2 main methods of doing that, both of which
// will write the same bytes to our stream. In order, as hexadecimal, those bytes are:
// C2 AF 5C 5F 28 E3 83 84 29 5F 2F C2 AF
//
// 1. Writing from a CharSequence, usually a String:
sink.writeFromSequence(shrugString)
// Like other output-related areas of this library, you can also specify a range of indices in the sequence to actually
// be written; characters outside that range will be ignored. The range can either be specified as 2 Ints, startIndex
// (inclusive) & endIndex (exclusive), or as a single IntRange. If you do specify a range, be sure you use the same
// range with `mutf8Length()` too, if applicable; otherwise, your mutf8Length may indicate the wrong number of bytes for
// other decoders to read. Not specifying a range means the entire sequence is encoded.
//
// 2. Writing from a CharArray (char[] in Java):
sink.writeFromArray(shrugArray)
// This function & its overloads also accept a range in the same way as writeFromSequence(). Other than the characters
// coming from a CharArray, the behaviour is identical.
```

[(back to top ↑)](#usage)

#### In Java

##### Using a Mutf8Source

```java
import java.io.InputStream;
import me.nullicorn.mewteaf8.Mutf8Source;
import me.nullicorn.mewteaf8.StreamMutf8Source;

final class Mutf8Demo {
    public static void main(String... args) {
        // The next bytes in this hypothetical stream are, in order, as hexadecimal:
        // 00 0D C2 AF 5C 5F 28 E3 83 84 29 5F 2F C2 AF
        InputStream inputStream; // = ...;
        
        // We can then pass that stream to a StreamMutf8Source to start reading the Modified UTF-8 data it contains.
        Mutf8Source source = new StreamMutf8Source(/* stream = */ inputStream);

        // First, we'll need to read the length of the string, or the first 2 bytes of data. We need this number so that
        // we know how many bytes we'll need to read to get the entire encoded string.
        //
        // This is its own method to allow for flexibility in the encoding/decoding process. For example, if you want to
        // have strings' lengths in a table at a different place in the data, then associate lengths to strings on your
        // own. Or, if all your strings are the same size, encoding/decoding the length would be wasted time & space, so
        // it could be omitted altogether.
        //
        // In our case, the bytes are 00 0D. If we combine those in big-endian order into an unsigned integer, we get 13. If you
        // look back at the bytes, that's exactly how many come after the first 2.
        int mutf8Length = source.readLength();
        //  mutf8Length == 13

        // Now, we can read the contents of the string: its characters. Mutf8Sources can be read from in 3 different
        // ways:
        //
        // 1. As a plain String:
        String decodedString = source.readToString(mutf8Length);
        //     decodedString.equals("¯\\_(ツ)_/¯")
        //
        // 2. As a char array (char[])_:
        char[] decodedChars = source.readToArray(mutf8Length);
        //     java.util.Arrays.equals(decodedChars, "¯\\_(ツ)_/¯".toCharArray())
        //
        // 3. To the end of an existing Appendable, like a StringBuilder:
        Appendable charCollector = StringBuilder();
        source.readToAppendable(mutf8Length, /* destination = */ charCollector);
        //         charCollector.toString().equals("¯\\_(ツ)_/¯")
    }
}
```

[(back to top ↑)](#usage)

##### Using a Mutf8Sink

```java
import java.io.OutputStream;
import me.nullicorn.mewteaf8.Mutf8Length;
import me.nullicorn.mewteaf8.Mutf8Sink;
import me.nullicorn.mewteaf8.StreamMutf8Sink;

final class Mutf8Demo {
    public static void main(String... args) {
        OutputStream outputStream; // = ...
        Mutf8Sink sink = StreamMutf8Sink(/* stream = */ outputStream);

        CharSequence shrugString = "¯\\_(ツ)_/¯";
        char[] shrugArray = shrugString.toString().toCharArray();

        // If you're writing Modified UTF-8 data that should be readable by receivers other than your own app, you'll
        // need to write each sequence's mutf8Length directly before the characters themselves.
        //
        // This is its own method to allow for flexibility in the encoding/decoding process. For example, if you want to
        // have strings' lengths in a table at a different place in the data, then associate lengths to strings on your
        // own. Or, if all your strings are the same size, encoding/decoding the length would be wasted time & space, so
        // it could be omitted altogether.
        //
        // Anyway, we can use the static Mutf8Length helper class to get our shrugString's entire binary size. The same
        // function would also work on our shrugArray.
        sink.writeLength(Mutf8Length.of(shrugString));
        // The mutf8Length of our string/array is 13. In big-endian hexadecimal, that would be 00 0D, so those are the 2
        // bytes written by that call.

        // Next is to write the characters themselves. Mutf8Sink gives us 2 main methods of doing that, both of which
        // will write the same bytes to our stream. In order, as hexadecimal, those bytes are:
        // C2 AF 5C 5F 28 E3 83 84 29 5F 2F C2 AF
        //
        // 1. Writing from a CharSequence, usually a String:
        sink.writeFromSequence(shrugString);
        // Like other output-related areas of this library, you can also specify a range of indices in the sequence to
        // actually be written; characters outside that range will be ignored. The range must be specified as 2 ints,
        // startIndex (inclusive) & endIndex (exclusive). If you do specify a range, be sure you use the same range with
        // `MutfLength.of()` too, if applicable; otherwise, your mutf8Length may indicate the wrong number of bytes for
        // other decoders to read. Not specifying a range means the entire sequence is encoded.
        //
        // 2. Writing from a char array (char[]):
        sink.writeFromArray(shrugArray);
        // This function & its overloads also accept a range in the same way as writeFromSequence(). Other than the characters
        // coming from a CharArray, the behaviour is identical.
    }
}
```

[(back to top ↑)](#usage)

### Custom Sources & Destinations

If you're not using okio, ktor, or java.io for your I/O operations, you can still take advantage of the library's
encoding & decoding algorithm by defining your own `Mutf8Source` and/or `Mutf8Sink` implementation. Those classes simply
act as a middleman between your I/O library's sources/destinations and mew-tea-f8's encoders/decoders.

For example, let's create an implementation of each class where the sources & destinations are just a simple ByteArray
(`byte[]` in Java).

#### In Kotlin

##### Mutf8Source Implementation

```kotlin
import me.nullicorn.mewteaf8.Mutf8Source

class ByteArrayMutf8Source(private val bytes: ByteArray) : Mutf8Source() {

    // Increments by 1 for each byte that we read, allowing us to iterate over the array's bytes on an as-needed basis.
    // NOTE: Normally, before each function, you'd want to check if by reading more bytes, this index would wind up
    //       outside the `bytes` array's bounds. For the simplicity of this example, we won't be doing that here.
    private var index = 0
    
    // This function's job is to read the next 2 bytes & combine them into an unsigned integer in big-endian order.
    override fun readLength(): Int {
        // Read the next 2 bytes, in order.
        val byte1 = bytes[index++]
        val byte2 = bytes[index++]
        
        // This formula can be found in the docs for `Mutf8Source.readLength()`. What it's doing is combining the next
        // 2 bytes from the array into a single positive integer. They're combined in big-endian order and are 16 bits
        // total, meaning the result can be anywhere from 0 through 65,535; that's the same range as a UShort in Kotlin.
        return (byte1.toInt() and 0xFF shl 8) or (byte2.toInt() and 0xFF)
    }
    
    // This function's job is to read the bytes that make up a string's characters. Given an `amount`, it's expected to
    // return an array of that many bytes, the next ones in the underlying source (the `bytes` array in our case).
    override fun readBytes(amount: Int): ByteArray {
        // Reads the next few (or many) bytes from the array and returns them as one big array. The returned array's
        // size (and the size of the range we're copying) must be equal to the `amount`.
        val requestedBytes = bytes.copyOfRange(fromIndex = index, toIndex = index + amount)
        index += amount
        return requestedBytes
    }
}
```

[(back to top ↑)](#usage)

##### Mutf8Sink Implementation

```kotlin
import me.nullicorn.mewteaf8.Mutf8Sink

class ByteArrayMutf8Sink : Mutf8Sink() {
    
    private var bytesWritten = 0
    private var buffer: ByteArray? = null
    
    // Lets you get a copy of the written bytes once you're done writing them.
    fun toByteArray(): ByteArray =
        buffer?.copyOf(newSize = bytesWritten) ?: ByteArray(size = 0)
    
    private fun ensureCapacity(requiredCapacity: Int) {
        // If the buffer hasn't been created yet, initialize it with its size being the required capacity.
        if (buffer == null)
            buffer = ByteArray(size = requiredCapacity)
        
        // If the buffer is already created but is too small, copy its current contents into a new, big enough array.
        else if (buffer.size < requiredCapacity)
            buffer = buffer.copyOf(newSize = requiredCapacity)
    }
    
    override fun writeLength(mutf8Length: Int) {
        // Validate the `mutf8Length`.
        require(mutf8Length in 0 .. 65535) { "mutf8Length does not fit in an unsigned 16-bit integer: $mutf8Length" }
        
        // Ensure we have room to add 2 more bytes into our buffer.
        ensureCapacity(bytesWritten + 2)
        
        // Write the `mutf8Length` as 2 bytes in big-endian order.
        buffer[bytesWritten++] = (mutf8Length shr 8).toByte()
        buffer[bytesWritten++] = mutf8Length.toByte()
    }
    
    override fun writeBytes(bytes: ByteArray, untilIndex: Int) {
        // Validate the `untilIndex` parameter against the `bytes` array.
        if (untilIndex !in 0 .. bytes.size) 
            throw IndexOutOfBoundsException("untilIndex, $untilIndex, is out of bounds for array with size of ${bytes.size}")
        
        // Make room for all the bytes being written.
        ensureCapacity(bytesWritten + untilIndex)
        
        // Copy the provided array's contents (up until the `untilIndex`) into the end of our buffer.
        bytes.copyInto(destination = this.buffer, destinationOffset = bytesWritten, startIndex = 0, endIndex = untilIndex)
        
        // Shift our working index so that new bytes are inserted after the ones we just wrote, not in the same place.
        bytesWritten += untilIndex
    }
}
```

[(back to top ↑)](#usage)

#### In Java

##### Mutf8Source Implementation

```java
import java.util.Arrays;
import me.nullicorn.mewteaf8.Mutf8Source;

public class ByteArrayMutf8Source extends Mutf8Source {

    private final byte[] bytes;

    // Increments by 1 for each byte that we read, allowing us to iterate over the array's bytes on an as-needed basis.
    // NOTE: Normally, before each function, you'd want to check if by reading more bytes, this index would wind up
    //       outside the `bytes` array's bounds. For the simplicity of this example, we won't be doing that here.
    private int index = 0;

    public ByteArrayMutf8Source(byte[] bytes) {
        this.bytes = bytes;
    }

    // This function's job is to read the next 2 bytes & combine them into an unsigned integer in big-endian order.
    @Override
    public int readLength() {
        // Read the next 2 bytes, in order.
        byte byte1 = bytes[index++];
        byte byte2 = bytes[index++];

        // This formula can be found in the docs for `Mutf8Source.readLength()`. What it's doing is combining the next
        // 2 bytes from the array into a single positive integer. They're combined in big-endian order and are 16 bits
        // total, meaning the result can be anywhere from 0 through 65,535; that's the same range as a UShort in Kotlin.
        return (byte1 & 0xFF << 8) | (byte2 & 0xFF);
    }

    // This function's job is to read the bytes that make up a string's characters. Given an `amount`, it's expected to
    // return an array of that many bytes, the next ones in the underlying source (the `bytes` array in our case).
    @Override
    protected byte[] readBytes(int amount) {
        // Reads the next few (or many) bytes from the array and returns them as one big array. The returned array's
        // size (and the size of the range we're copying) must be equal to the `amount`.
        byte[] requestedBytes = Arrays.copyOfRange(bytes, /* from = */ index, /* to = */ index + amount);
        index += amount;
        return requestedBytes;
    }
}
```

[(back to top ↑)](#usage)

##### Mutf8Sink Implementation

```java
import java.util.Arrays;
import me.nullicorn.mewteaf8.Mutf8Sink;

public class ByteArrayMutf8Sink extends Mutf8Sink {

    private int bytesWritten = 0;
    private byte[] buffer = null;

    public byte[] toByteArray() {
        if (buffer == null) {
            return new byte[0];
        }
        return Arrays.copyOf(buffer, bytesWritten);
    }

    // Resizes the `buffer` as we write data so that there's always enough room.
    private void ensureCapacity(int requiredCapacity) {
        if (requiredCapacity < 0) {
            throw new IllegalArgumentException("requiredCapacity must be positive, not " + requiredCapacity);
        }

        // If the buffer hasn't been created yet, initialize it with its size being the required capacity.
        if (buffer == null) {
            buffer = new byte[requiredCapacity];
        }

        // If the buffer is already created but is too small, copy its current contents into a new, big enough array.
        else if (buffer.size < requiredCapacity) {
            buffer = Arrays.copyOf(buffer, requiredCapacity);
        }
    }

    @Override
    public void writeLength(int mutf8Length) {
        // Validate the `mutf8Length`.
        if (0 > mutf8Length || mutf8Length > 65535) {
            throw new IllegalArgumentException("mutf8Length does not fit in an unsigned 16-bit integer:" + mutf8Length);
        }

        // Ensure we have room to add 2 more bytes into our buffer.
        ensureCapacity(bytesWritten + 2);

        // Write the `mutf8Length` as 2 bytes in big-endian order.
        buffer[bytesWritten++] = (byte) (mutf8Length >> 8);
        buffer[bytesWritten++] = (byte) mutf8Length;
    }

    @Override
    protected void writeBytes(byte[] bytes, int untilIndex) {
        // Validate the `untilIndex` parameter against the `bytes` array.
        if (0 > untilIndex || untilIndex >= bytes.length) {
            throw new IndexOutOfBoundsException("untilIndex, " + untilIndex + ", is out of bounds for array with size of " + bytes.length);
        }
        
        // Make room for all the bytes being written.
        ensureCapacity(bytesWritten + untilIndex);

        // Copy the provided array's contents (up until the `untilIndex`) into the end of our buffer.
        System.arraycopy(/* src = */ bytes, /* srcPos = */ 0, /* dest = */ buffer, /* destPos = */ bytesWritten, /* length = */ untilIndex);

        // Shift our working index so that new bytes are inserted after the ones we just wrote, not in the same place.
        bytesWritten += untilIndex;
    }
}
```

[(back to top ↑)](#usage)

---

## Installation

Depending on how you're already doing reading & writing in your project, this library comes with a couple of submodules
for common I/O libraries.

1. Click on the dependency(ies) your project needs in the table below
    1. If your project uses the Kotlin gradle plugin, use the links in the *"Dependency (for Kotlin Projects)"* column
    2. Otherwise, if your project is Java (or another JVM language), use the links in the *"Dependency (for Java
       Projects)"* column
2. On the `mvnrepository.com` page that loads, select the latest version of the dependency
3. On the next page, select the tab for your project's build tool (Maven, Gradle, etc)
4. Underneath the tab you selected, copy the snippet into the relevant part of your build file
    1. With Maven, for example, paste it inside the `<dependencies> </dependencies>` tag of `pom.xml`
    2. With Gradle, for example, past it inside the `dependencies { }` block of `build.gradle` or `build.gradle.kts` (for
       Groovy or Kotlin buildscripts respectively)

| I/O Interface                                           | Dependency (for Kotlin Projects)   | Dependency (for Java Projects)             | 
|---------------------------------------------------------|------------------------------------|--------------------------------------------|
| java.io (`InputStream`/`OutputStream`)                  | [mew-tea-f8-core][mvnrepo-core]    | [mew-tea-f8-core-jvm][mvnrepo-core-jvm]    |
| okio (`Source`/`Sink`, `BufferedSource`/`BufferedSink`) | [mew-tea-f8-okio][mvnrepo-okio]    | [mew-tea-f8-okio-jvm][mvnrepo-okio-jvm]    |
| ktor / ktor-io (`Input`/`Output`)                       | [mew-tea-f8-ktor-io][mvnrepo-ktor] | [mew-tea-f8-ktor-io-jvm][mvnrepo-ktor-jvm] |
| Other / Custom                                          | [mew-tea-f8-core][mvnrepo-core]    | [mew-tea-f8-core-jvm][mvnrepo-core-jvm]    |

[mvnrepo-core]: https://mvnrepository.com/artifact/me.nullicorn/mew-tea-f8-core

[mvnrepo-okio]: https://mvnrepository.com/artifact/me.nullicorn/mew-tea-f8-okio

[mvnrepo-ktor]: https://mvnrepository.com/artifact/me.nullicorn/mew-tea-f8-ktor-io

[mvnrepo-core-jvm]: https://mvnrepository.com/artifact/me.nullicorn/mew-tea-f8-core-jvm

[mvnrepo-okio-jvm]: https://mvnrepository.com/artifact/me.nullicorn/mew-tea-f8-okio-jvm

[mvnrepo-ktor-jvm]: https://mvnrepository.com/artifact/me.nullicorn/mew-tea-f8-ktor-io-jvm