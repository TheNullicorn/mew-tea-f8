@file:JvmName("Mutf8OkioSources")

package me.nullicorn.mewteaf8

import okio.Source
import kotlin.jvm.JvmName

/**
 * Creates a [OkioMutf8Source] around the receiver, reads the length & contents of a Modified UTF-8 string, and then
 * returns the contents as a [String].
 *
 * In code, that would be:
 * ```kotlin
 * val source = OkioMutf8Source(source = this)
 * val mutf8Length = source.readLength()
 * return source.readToString(mutf8Length)
 * ```
 *
 * All [IOException]s, [EOFException]s, and [UTFDataFormatException]s documented on those methods apply to this function
 * too.
 *
 * @see[OkioMutf8Source]
 * @see[OkioMutf8Source.readLength]
 * @see[OkioMutf8Source.readToString]
 */
@Throws(IOException::class, EOFException::class, UTFDataFormatException::class)
@JvmName("readStringFrom")
fun Source.readMutf8String(): String {
    val source = OkioMutf8Source(source = this)
    val mutf8Length = source.readLength()
    return source.readToString(mutf8Length)
}

/**
 * Creates a [OkioMutf8Source] around the receiver, reads the length & contents of a Modified UTF-8 string, and then
 * returns the contents as a [CharArray].
 *
 * In code, that would be:
 * ```kotlin
 * val source = OkioMutf8Source(source = this)
 * val mutf8Length = source.readLength()
 * return source.readToArray(mutf8Length)
 * ```
 *
 * All [IOException]s, [EOFException]s, and [UTFDataFormatException]s documented on those methods apply to this function
 * too.
 *
 * @see[OkioMutf8Source]
 * @see[OkioMutf8Source.readLength]
 * @see[OkioMutf8Source.readToArray]
 */
@Throws(IOException::class, EOFException::class, UTFDataFormatException::class)
@JvmName("readArrayFrom")
fun Source.readMutf8Array(): CharArray {
    val source = OkioMutf8Source(source = this)
    val mutf8Length = source.readLength()
    return source.readToArray(mutf8Length)
}

/**
 * Creates a [OkioMutf8Source] around the receiver, reads the length & contents of a Modified UTF-8 string, and then
 * writes those contents to the supplied [destination], such as a [StringBuilder].
 *
 * In code, that would be:
 * ```kotlin
 * val source = OkioMutf8Source(source = this)
 * val mutf8Length = source.readLength()
 * return source.readToAppendable(mutf8Length, destination)
 * ```
 *
 * All [IOException]s, [EOFException]s, and [UTFDataFormatException]s documented on those methods apply to this function
 * too.
 *
 * @see[OkioMutf8Source]
 * @see[OkioMutf8Source.readLength]
 * @see[OkioMutf8Source.readToAppendable]
 */
@Throws(IOException::class, EOFException::class, UTFDataFormatException::class)
@JvmName("readToAppendableFrom")
fun Source.readMutf8ToAppendable(destination: Appendable) {
    val source = OkioMutf8Source(source = this)
    val mutf8Length = source.readLength()
    return source.readToAppendable(mutf8Length, destination)
}