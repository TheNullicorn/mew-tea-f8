@file:JvmName("Mutf8InputStreams")

package me.nullicorn.mewteaf8

import me.nullicorn.mewteaf8.internal.*
import java.io.DataInput
import java.io.DataOutput
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UTFDataFormatException
import java.lang.IllegalArgumentException
import kotlin.jvm.Throws

//region Readers

/**
 * Reads a Modified UTF-8 string from the [InputStream], including the 2-byte `mutf8Length` field preceding the string's
 * contents.
 *
 * First, this method reads the next 2 bytes from the stream, combining them into a single [UShort] in big-endian
 * order. Then, if that number was not `0`, then exactly that many further bytes are read.
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
@OptIn(InternalMutf8Api::class)
fun InputStream.readMutf8String(): String {
    val source = InputStreamMutf8Source(this)

    try {
        val mutf8Length = source.readLength()
        return source.readString(mutf8Length)
    } catch (cause: Mutf8Exception) {
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
@OptIn(InternalMutf8Api::class)
fun InputStream.readMutf8Length(): UShort = try {
    InputStreamMutf8Source(this).readLength()
} catch (cause: Mutf8Exception) {
    throw cause.toBuiltInJavaException()
}

/**
 * Reads the next *[mutf8Length]* number of bytes from the stream, and decodes them from Modified UTF-8 into a string.
 *
 * Unlike [readMutf8String], this does not read the 2-byte length value that precedes the string's encoded characters.
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
@OptIn(InternalMutf8Api::class)
fun InputStream.readMutf8Contents(mutf8Length: UShort): String = try {
    InputStreamMutf8Source(this).readString(mutf8Length)
} catch (cause: Mutf8Exception) {
    throw cause.toBuiltInJavaException()
}

//endregion
//region Writers

/**
 * Writes a Modified UTF-8 string to the [OutputStream], including the 2-byte [mutf8Length] field preceding the string's
 * contents.
 *
 * First, this method calculates the [mutf8Length] of the [string] using [this][mutf8Length] extension property. If that
 * number exceeds [UShort.MAX_VALUE], then the string is too long to be written as Modified UTF-8 data. Otherwise, the
 * [mutf8Length] is written to the stream as 2 bytes in big-endian order. Finally, using exactly the number of bytes
 * determined by the [mutf8Length], the string's contents are written to the stream as Modified UTF-8 data.
 *
 * If the receiving stream is known to be a [DataOutputStream], then Java's own [DataOutput.writeUTF] method can be used
 * instead. This method simply exists as a multiplatform alternative.
 *
 * @receiver The stream to write the [mutf8Length] and [string] to.
 * @param[string] The string whose [mutf8Length] and characters will be written to the receiving [OutputStream].
 * @param[bytesPerWrite] The maximum number of bytes to write to the [OutputStream] at once when writing the string's
 * contents in bulk. This must be at least `1`.
 *
 * @throws[IllegalArgumentException] if [bytesPerWrite] is less than `1`.
 * @throws[IllegalArgumentException] if the string's [mutf8Length] exceeds [UShort.MAX_VALUE].
 * @throws[IOException] if an I/O issue occurs while trying to write the string's length or its contents.
 *
 * @see[writeMutf8Length]
 * @see[writeMutf8Contents]
 */
@JvmOverloads
@JvmName("writeStringTo")
@Throws(IllegalArgumentException::class, IOException::class)
@OptIn(InternalMutf8Api::class)
fun OutputStream.writeMutf8String(string: String, bytesPerWrite: Int = 1024) {
    require(bytesPerWrite >= 1) { "bytesPerWrite must be at least 1" }

    // Ensure the string's length (in bytes) is small enough to fit in an unsigned 16-bit integer.
    val mutf8Length = string.mutf8Length
    require(mutf8Length <= UShort.MAX_VALUE.toLong()) { "String is too long to be encoded as Modified UTF-8 data; it would take up $mutf8Length bytes, but the maximum allowed is ${UShort.MAX_VALUE}" }

    val sink = OutputStreamMutf8Sink(this)

    try {
        sink.writeLength(mutf8Length.toUShort())
        sink.writeString(string, bytesPerWrite)
    } catch (cause: Mutf8Exception) {
        throw cause.toBuiltInJavaException()
    }
}

/**
 * Writes the [mutf8Length] of a Modified UTF-8 string to the stream using 2 bytes in big-endian order.
 *
 * If the string itself is also going to be written, and it's encoded normally where its contents immediately follow
 * the 2-byte length, then [writeMutf8String] can be used instead. That way the length & contents don't need to be
 * written separately.
 *
 * The [mutf8Length] is written as 2 bytes in big-endian order, like so:
 * ```kotlin
 * write(mutf8Length.toInt() shr 8 and 0xFF)
 * write(mutf8Length.toInt() and 0xFF)
 * ```
 *
 * @receiver The stream to write the [mutf8Length] to.
 * @param[mutf8Length] The length of the Modified UTF-8 string in question, in bytes.
 *
 * @throws[IOException] if an I/O issue occurs while trying to write either byte.
 *
 * @see[mutf8Length]
 * @see[writeMutf8Contents]
 * @see[writeMutf8String]
 */
@JvmName("writeLengthTo")
@Throws(IOException::class)
@OptIn(InternalMutf8Api::class)
fun OutputStream.writeMutf8Length(mutf8Length: UShort) = try {
    OutputStreamMutf8Sink(this).writeLength(mutf8Length)
} catch (cause: Mutf8Exception) {
    throw cause.toBuiltInJavaException()
}

/**
* Writes the characters of a [string] to the [OutputStream] as a Modified UTF-8 sequence.
*
* Unlike [writeMutf8String], this does not write the 2-byte length value that precedes the string's encoded characters.
* That value must be known by the caller in advance and should, for typical use-cases, be written using
* [writeMutf8Length].
*
* For strings encoded normally, where the 2-byte length *is* immediately before the string's contents,
* [writeMutf8String] can be used instead. That way the caller doesn't have to manually
* [write the string's length][writeMutf8Length].
*
* @throws[IllegalArgumentException] if the string's [mutf8Length] exceeds [UShort.MAX_VALUE].
* @throws[IOException] if an I/O issue occurs while trying to write the string.
*
* @see[writeMutf8Length]
* @see[writeMutf8String]
*/
@JvmOverloads
@JvmName("writeContentsTo")
@Throws(IllegalArgumentException::class, IOException::class)
@OptIn(InternalMutf8Api::class)
fun OutputStream.writeMutf8Contents(string: String, bytesPerWrite: Int = 1024) = try {
    OutputStreamMutf8Sink(this).writeString(string, bytesPerWrite)
} catch (cause: Mutf8Exception) {
    throw cause.toBuiltInJavaException()
}

//endregion
//region Private Helpers

/**
 * Converts a [Mutf8Exception] into whatever type of exception [DataInput.readUTF] would throw if the same issue
 * arose.
 *
 * This method does not throw the resulting exception, but returns it. It is the caller's responsibility to throw it.
 *
 * @receiver The exception to get Java's built-in equivalent for.
 * @return
 * - [IOException] if the receiver is a [Mutf8IOException]
 * - [EOFException] if the receiver is a [Mutf8EOFException]
 * - [UTFDataFormatException] if the receiver is a [Mutf8MalformedPrimaryByteException], [Mutf8MalformedSecondaryByteException],
 * or [Mutf8TruncatedCharacterException]
 */
@OptIn(InternalMutf8Api::class)
private fun Mutf8Exception.toBuiltInJavaException(): IOException =
    when (this) {
        // NOTE: `UTFDataFormatException` and `EOFException` don't have a `cause` parameters, so we just replace the
        //       `stackTrace` with the original exception's `stackTrace` manually.

        is Mutf8MalformedPrimaryByteException,
        is Mutf8MalformedSecondaryByteException,
        is Mutf8TruncatedCharacterException ->
            (cause as? UTFDataFormatException) ?: UTFDataFormatException(message)
                .also { it.stackTrace = (cause ?: this).stackTrace }

        is Mutf8EOFException ->
            (cause as? EOFException) ?: EOFException(message)
                .also { it.stackTrace = (cause ?: this).stackTrace }

        is Mutf8IOException ->
            (cause as? IOException) ?: IOException(message, cause ?: this)
    }

/**
 * A [Mutf8Source] that reads from an [InputStream].
 */
@OptIn(InternalMutf8Api::class)
private class InputStreamMutf8Source(private val stream: InputStream) : Mutf8Source {

    override fun readBytes(amount: UShort): ByteArray {
        val array = ByteArray(size = amount.toInt())
        val bytesRead = try {
            stream.read(array)
        } catch (cause: IOException) {
            throw Mutf8IOException("Failed to read the contents of a Modified UTF-8 string", cause)
        }

        if (bytesRead < 0)
            throw Mutf8EOFException("Data ended before the contents of a Modified UTF-8 string")

        if (bytesRead < array.size)
            throw Mutf8EOFException("Data only had $bytesRead bytes left for a Modified UTF-8 string that expected $amount bytes")

        return array
    }

    override fun readLength(): UShort {
        val byte1: Int
        val byte2: Int

        try {
            byte1 = stream.read()
            byte2 = stream.read()
        } catch (cause: EOFException) {
            throw Mutf8EOFException("Data ended while reading the length of a Modified UTF-8 string", cause)
        } catch (cause: IOException) {
            throw Mutf8IOException("Failed to read the length of a Modified UTF-8 string", cause)
        }

        if (byte1 == -1 || byte2 == -1)
            throw Mutf8EOFException("Data ended while reading the length of a Modified UTF-8 string")

        return ((byte1 and 0xFF shl 8) or (byte2 and 0xFF)).toUShort()
    }
}

/**
 * A [Mutf8Sink] that writes to an [OutputStream].
 */
@OptIn(InternalMutf8Api::class)
private class OutputStreamMutf8Sink(private val stream: OutputStream) : Mutf8Sink {

    override fun writeBytes(bytes: ByteArray, untilIndex: Int) {
        require(untilIndex >= 0) { "untilIndex must be at least 0, not $untilIndex" }
        require(untilIndex <= bytes.size) { "untilIndex must be at most bytes.size, ${bytes.size}, not $untilIndex" }

        try {
            stream.write(bytes, 0, untilIndex)
        } catch (cause: IOException) {
            throw Mutf8IOException("Failed to write a ${bytes.size}-byte chunk to the OutputStream", cause)
        }
    }

    override fun writeLength(length: UShort) {
        val lengthInt = length.toInt()

        try {
            stream.write(lengthInt shr 8 and 0xFF)
            stream.write(lengthInt /* */ and 0xFF)
        } catch (cause: IOException) {
            throw Mutf8IOException("Failed to write a Modified UTF-8 string's length to the OutputStream", cause)
        }
    }
}

//endregion