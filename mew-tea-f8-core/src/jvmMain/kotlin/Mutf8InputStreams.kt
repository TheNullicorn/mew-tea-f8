@file:JvmName("Mutf8InputStreams")

package me.nullicorn.mewteaf8

import java.io.InputStream

/**
 * Creates a [StreamMutf8Source] around the receiver, reads the length & contents of a Modified UTF-8 string, and then
 * returns the contents as a [String].
 *
 * In code, that would be:
 * ```kotlin
 * val source = StreamMutf8Source(stream = this)
 * val mutf8Length = source.readLength()
 * return source.readToString(mutf8Length)
 * ```
 *
 * All [IOException]s, [EOFException]s, and [UTFDataFormatException]s documented on those methods apply to this function
 * too.
 *
 * @see[StreamMutf8Source]
 * @see[StreamMutf8Source.readLength]
 * @see[StreamMutf8Source.readToString]
 */
@Throws(IOException::class, EOFException::class, UTFDataFormatException::class)
@JvmName("readStringFrom")
fun InputStream.readMutf8String(): String {
    val source = StreamMutf8Source(stream = this)
    val mutf8Length = source.readLength()
    return source.readToString(mutf8Length)
}

/**
 * Creates a [StreamMutf8Source] around the receiver, reads the length & contents of a Modified UTF-8 string, and then
 * returns the contents as a [CharArray].
 *
 * In code, that would be:
 * ```kotlin
 * val source = StreamMutf8Source(stream = this)
 * val mutf8Length = source.readLength()
 * return source.readToArray(mutf8Length)
 * ```
 *
 * All [IOException]s, [EOFException]s, and [UTFDataFormatException]s documented on those methods apply to this function
 * too.
 *
 * @see[StreamMutf8Source]
 * @see[StreamMutf8Source.readLength]
 * @see[StreamMutf8Source.readToArray]
 */
@Throws(IOException::class, EOFException::class, UTFDataFormatException::class)
@JvmName("readArrayFrom")
fun InputStream.readMutf8Array(): CharArray {
    val source = StreamMutf8Source(stream = this)
    val mutf8Length = source.readLength()
    return source.readToArray(mutf8Length)
}

/**
 * Creates a [StreamMutf8Source] around the receiver, reads the length & contents of a Modified UTF-8 string, and then
 * writes those contents to the supplied [destination], such as a [StringBuilder].
 *
 * In code, that would be:
 * ```kotlin
 * val source = StreamMutf8Source(stream = this)
 * val mutf8Length = source.readLength()
 * return source.readToAppendable(mutf8Length, destination)
 * ```
 *
 * All [IOException]s, [EOFException]s, and [UTFDataFormatException]s documented on those methods apply to this function
 * too.
 *
 * @see[StreamMutf8Source]
 * @see[StreamMutf8Source.readLength]
 * @see[StreamMutf8Source.readToAppendable]
 */
@Throws(IOException::class, EOFException::class, UTFDataFormatException::class)
@JvmName("readToAppendableFrom")
fun InputStream.readMutf8ToAppendable(destination: Appendable) {
    val source = StreamMutf8Source(stream = this)
    val mutf8Length = source.readLength()
    return source.readToAppendable(mutf8Length, destination)
}