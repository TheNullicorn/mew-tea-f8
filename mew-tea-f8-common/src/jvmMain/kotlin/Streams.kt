@file:JvmName("ModifiedUtf8Streams")
@file:OptIn(InternalMewTeaF8Api::class)

package me.nullicorn.mewteaf8

import me.nullicorn.mewteaf8.internal.*
import java.io.DataInput
import java.io.DataInputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.UTFDataFormatException
import kotlin.jvm.Throws

/**
 * Reads a Modified UTF-8 string from the [InputStream], including the 2-byte `mutf8Length` field preceding the string's
 * contents.
 *
 * First, this method reads the next 2 bytes from the stream, combining them into a single [UShort]. Then, if it was not
 * `0`, exactly that many further bytes are read.
 *
 * If the receiving stream is known to be a [DataInputStream], then Java's own [DataInput.readUTF] method can be used
 * instead. This method simply exists as a multiplatform alternative.
 *
 * @receiver The stream to read the length and characters from.
 * @return the string whose length and characters are next read from the [InputStream] in Modified UTF-8 encoding.
 *
 * @throws[UTFDataFormatException] if the string's contents are not correctly encoded as Modified UTF-8.
 * @throws[EOFException] if the end of the stream is reached before the string's entire length and contents can be read.
 * @throws[IOException] if an I/O issue occurs while trying to read the string's length or its contents.
 *
 * @see[readMutf8Length]
 * @see[readMutf8Contents]
 */
@JvmName("readStringFrom")
@Throws(UTFDataFormatException::class, EOFException::class, IOException::class)
fun InputStream.readMutf8String(): String {
    val source = InputStreamMutf8ByteSource(this)

    try {
        val mutf8Length = source.readUtfLength()
        return source.readModifiedUtf8(mutf8Length)
    } catch (cause: ModifiedUtf8Exception) {
        throw cause.toBuiltInJavaException()
    }
}

/**
 * Reads & interprets the next 2 bytes from the stream as the length of a Modified UTF-8 string.
 *
 * If the string itself is also going to be read, and it's encoded normally where its contents immediately follow the
 * 2-byte length, then [readMutf8String] can be used instead. That way the length & contents don't need to be read
 * separately.
 *
 * The 2 bytes are combined into a [UShort] in big-endian order, like so:
 * ```kotlin
 * val byte1 = read() and 0xFF
 * val byte2 = read() and 0xFF
 * return ((byte1 shl 8) or byte2).toUShort()
 * ```
 *
 * @receiver The stream to read the length from.
 * @return the number of bytes that were used to encode the string in question, not including the 2-byte length being
 * read here.
 *
 * @throws[EOFException] if the end of the stream is reached before both bytes can be read.
 * @throws[IOException] if an I/O issue occurs while trying to read either byte.
 *
 * @see[mutf8Length]
 * @see[readMutf8Contents]
 * @see[readMutf8String]
 */
@JvmName("readLengthFrom")
@Throws(EOFException::class, IOException::class)
fun InputStream.readMutf8Length(): UShort = try {
    InputStreamMutf8ByteSource(this).readUtfLength()
} catch (cause: ModifiedUtf8Exception) {
    throw cause.toBuiltInJavaException()
}

/**
 * Reads the next *[mutf8Length]* number of bytes from the stream, and decodes them from Modified UTF-8 into a string.
 *
 * Unlike [readMutf8Contents], this does not read the 2-byte length value that precedes the string's encoded characters.
 * That value must be known by the caller in advance, and supplied via the [mutf8Length].
 *
 * For strings encoded normally, where the 2-byte length *is* immediately before the string's contents,
 * [readMutf8String] can be used instead. That way the caller doesn't have to manually
 * [read the string's length][readMutf8Length].
 *
 * @throws[UTFDataFormatException] if the string is not correctly encoded as Modified UTF-8.
 * @throws[EOFException] if the end of the stream is reached before the entire string can be read.
 * @throws[IOException] if an I/O issue occurs while trying to read the string.
 *
 * @see[readMutf8Length]
 * @see[readMutf8String]
 */
@JvmName("readContentsFrom")
@Throws(UTFDataFormatException::class, EOFException::class, IOException::class)
fun InputStream.readMutf8Contents(mutf8Length: UShort): String = try {
    InputStreamMutf8ByteSource(this).readModifiedUtf8(mutf8Length)
} catch (cause: ModifiedUtf8Exception) {
    throw cause.toBuiltInJavaException()
}

//region Private Helpers

/**
 * Converts a [ModifiedUtf8Exception] into whatever type of exception [DataInput.readUTF] would throw if the same issue
 * arose.
 *
 * This method does not throw the resulting exception, but returns it. It is the caller's responsibility to throw it.
 *
 * @receiver The exception to get Java's built-in equivalent for.
 * @return
 * - [IOException] if the receiver is a [ModifiedUtf8IOException]
 * - [EOFException] if the receiver is a [ModifiedUtf8EOFException]
 * - [UTFDataFormatException] if the receiver is a [MalformedPrimaryByteException], [MalformedSecondaryByteException],
 * or [CharacterStartedTooLateException]
 */
private fun ModifiedUtf8Exception.toBuiltInJavaException(): IOException =
    when (this) {
        // NOTE: `UTFDataFormatException` and `EOFException` don't have a `cause` parameters, so we just replace the
        //       `stackTrace` with the original exception's `stackTrace` manually.

        is MalformedPrimaryByteException,
        is MalformedSecondaryByteException,
        is CharacterStartedTooLateException ->
            (cause as? UTFDataFormatException) ?: UTFDataFormatException(message)
                .also { it.stackTrace = (cause ?: this).stackTrace }

        is ModifiedUtf8EOFException ->
            (cause as? EOFException) ?: EOFException(message)
                .also { it.stackTrace = (cause ?: this).stackTrace }

        is ModifiedUtf8IOException ->
            (cause as? IOException) ?: IOException(message, cause ?: this)
    }

/**
 * A [ModifiedUtf8ByteSource] that reads from an [InputStream].
 */
private class InputStreamMutf8ByteSource(private val stream: InputStream) : ModifiedUtf8ByteSource {

    override fun readBytes(amount: UShort): ByteArray {
        val array = ByteArray(size = amount.toInt())
        val bytesRead = try {
            stream.read(array)
        } catch (cause: IOException) {
            throw ModifiedUtf8IOException("Failed to read the contents of a Modified UTF-8 string", cause)
        }

        if (bytesRead < 0)
            throw ModifiedUtf8EOFException("Data ended before the contents of a Modified UTF-8 string")

        if (bytesRead < array.size)
            throw ModifiedUtf8EOFException("Data only had $bytesRead bytes left for a Modified UTF-8 string that expected $amount bytes")

        return array
    }

    override fun readUtfLength(): UShort {
        val byte1: Int
        val byte2: Int

        try {
            byte1 = stream.read()
            byte2 = stream.read()
        } catch (cause: EOFException) {
            throw ModifiedUtf8EOFException("Data ended while reading the length of a Modified UTF-8 string", cause)
        } catch (cause: IOException) {
            throw ModifiedUtf8IOException("Failed to read the length of a Modified UTF-8 string", cause)
        }

        if (byte1 == -1 || byte2 == -1)
            throw ModifiedUtf8EOFException("Data ended while reading the length of a Modified UTF-8 string")

        return ((byte1 and 0xFF shl 8) or (byte2 and 0xFF)).toUShort()
    }
}

//endregion