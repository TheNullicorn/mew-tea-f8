@file:JvmName("Mutf8KtorInputs")

package me.nullicorn.mewteaf8

import io.ktor.utils.io.core.*
import kotlin.jvm.JvmName

/**
 * Creates a [InputMutf8Source] around the receiver, reads the length & contents of a Modified UTF-8 string, and then
 * returns the contents as a [String].
 *
 * In code, that would be:
 * ```kotlin
 * val source = InputMutf8Source(input = this)
 * val mutf8Length = source.readLength()
 * return source.readToString(mutf8Length)
 * ```
 *
 * All [IOException]s, [EOFException]s, and [UTFDataFormatException]s documented on those methods apply to this function
 * too.
 *
 * @see[InputMutf8Source]
 * @see[InputMutf8Source.readLength]
 * @see[InputMutf8Source.readToString]
 */
@Throws(IOException::class, EOFException::class, UTFDataFormatException::class)
@JvmName("readStringFrom")
fun Input.readMutf8String(): String {
    val source = InputMutf8Source(input = this)
    val mutf8Length = source.readLength()
    return source.readToString(mutf8Length)
}

/**
 * Creates a [InputMutf8Source] around the receiver, reads the length & contents of a Modified UTF-8 string, and then
 * returns the contents as a [CharArray].
 *
 * In code, that would be:
 * ```kotlin
 * val source = InputMutf8Source(input = this)
 * val mutf8Length = source.readLength()
 * return source.readToArray(mutf8Length)
 * ```
 *
 * All [IOException]s, [EOFException]s, and [UTFDataFormatException]s documented on those methods apply to this function
 * too.
 *
 * @see[InputMutf8Source]
 * @see[InputMutf8Source.readLength]
 * @see[InputMutf8Source.readToArray]
 */
@Throws(IOException::class, EOFException::class, UTFDataFormatException::class)
@JvmName("readArrayFrom")
fun Input.readMutf8Array(): CharArray {
    val source = InputMutf8Source(input = this)
    val mutf8Length = source.readLength()
    return source.readToArray(mutf8Length)
}

/**
 * Creates a [InputMutf8Source] around the receiver, reads the length & contents of a Modified UTF-8 string, and then
 * writes those contents to the supplied [destination], such as a [StringBuilder].
 *
 * In code, that would be:
 * ```kotlin
 * val source = InputMutf8Source(input = this)
 * val mutf8Length = source.readLength()
 * return source.readToAppendable(mutf8Length, destination)
 * ```
 *
 * All [IOException]s, [EOFException]s, and [UTFDataFormatException]s documented on those methods apply to this function
 * too.
 *
 * @see[InputMutf8Source]
 * @see[InputMutf8Source.readLength]
 * @see[InputMutf8Source.readToAppendable]
 */
@Throws(IOException::class, EOFException::class, UTFDataFormatException::class)
@JvmName("readToAppendableFrom")
fun Input.readMutf8ToAppendable(destination: Appendable) {
    val source = InputMutf8Source(input = this)
    val mutf8Length = source.readLength()
    return source.readToAppendable(mutf8Length, destination)
}