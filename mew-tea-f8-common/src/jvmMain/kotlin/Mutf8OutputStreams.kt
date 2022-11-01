@file:JvmSynthetic

package me.nullicorn.mewteaf8

import java.io.OutputStream
import kotlin.jvm.Throws

private const val DEFAULT_BYTES_PER_WRITE = 1024
private const val MAX_MUTF8_LENGTH = 65535

/**
 * Creates a [StreamMutf8Sink] around the receiver, [writes][StreamMutf8Sink.writeLength] the
 * [mutf8Length][CharSequence.mutf8Length] of the [characters], and then [writes][StreamMutf8Sink.writeFromSequence] the
 * [characters] themselves.
 *
 * In code, that would be:
 * ```kotlin
 * val mutf8Length = characters.mutf8Length(startIndex, endIndex)
 * val sink = StreamMutf8Sink(stream = this, bytesPerWrite)
 * sink.writeLength(mutf8Length)
 * sink.writeFromSequence(characters, startIndex, endIndex)
 * ```
 *
 * See the documentation for those methods (linked below) for their limitations, exceptions, parameters, and their
 * meanings.
 *
 * @see[CharSequence.mutf8Length]
 * @see[StreamMutf8Sink]
 * @see[StreamMutf8Sink.writeLength]
 * @see[StreamMutf8Sink.writeFromSequence]
 */
@Throws(IOException::class)
fun OutputStream.writeMutf8Sequence(
    characters: CharSequence,
    startIndex: Int = 0,
    endIndex: Int = characters.length,
    bytesPerWrite: Int = DEFAULT_BYTES_PER_WRITE
) = writeMutf8(
    characters,
    bytesPerWrite,
    getMutf8Length = { characters.mutf8Length(startIndex, endIndex) },
    writeCharacters = { writeFromSequence(characters, startIndex, endIndex) }
)

/**
 * Creates a [StreamMutf8Sink] around the receiver, [writes][StreamMutf8Sink.writeLength] the
 * [mutf8Length][CharSequence.mutf8Length] of the [characters], and then [writes][StreamMutf8Sink.writeFromSequence] the
 * [characters] themselves.
 *
 * In code, that would be:
 * ```kotlin
 * val mutf8Length = characters.mutf8Length(startIndex = range.first, endIndex = range.last + 1)
 * val sink = StreamMutf8Sink(stream = this, bytesPerWrite)
 * sink.writeLength(mutf8Length)
 * sink.writeFromSequence(characters, range)
 * ```
 *
 * See the documentation for those methods (linked below) for their limitations, exceptions, parameters, and their
 * meanings.
 *
 * @see[CharSequence.mutf8Length]
 * @see[StreamMutf8Sink]
 * @see[StreamMutf8Sink.writeLength]
 * @see[StreamMutf8Sink.writeFromSequence]
 */
@Throws(IOException::class)
fun OutputStream.writeMutf8Sequence(
    characters: CharSequence,
    range: IntRange = 0..characters.lastIndex,
    bytesPerWrite: Int = DEFAULT_BYTES_PER_WRITE
) = writeMutf8Sequence(characters, startIndex = range.first, endIndex = range.last + 1, bytesPerWrite)

/**
 * Creates a [StreamMutf8Sink] around the receiver, [writes][StreamMutf8Sink.writeLength] the
 * [mutf8Length][CharArray.mutf8Length] of the [characters], and then [writes][StreamMutf8Sink.writeFromArray] the
 * [characters] themselves.
 *
 * In code, that would be:
 * ```kotlin
 * val mutf8Length = characters.mutf8Length(startIndex, endIndex)
 * val sink = StreamMutf8Sink(stream = this, bytesPerWrite)
 * sink.writeLength(mutf8Length)
 * sink.writeFromArray(characters, startIndex, endIndex)
 * ```
 *
 * See the documentation for those methods (linked below) for their limitations, exceptions, parameters, and their
 * meanings.
 *
 * @see[CharArray.mutf8Length]
 * @see[StreamMutf8Sink]
 * @see[StreamMutf8Sink.writeLength]
 * @see[StreamMutf8Sink.writeFromArray]
 */
@Throws(IOException::class)
fun OutputStream.writeMutf8Array(
    characters: CharArray,
    startIndex: Int = 0,
    endIndex: Int = characters.size,
    bytesPerWrite: Int = DEFAULT_BYTES_PER_WRITE
) = writeMutf8(
    characters,
    bytesPerWrite,
    getMutf8Length = { characters.mutf8Length(startIndex, endIndex) },
    writeCharacters = { writeFromArray(characters, startIndex, endIndex) }
)

/**
 * Creates a [StreamMutf8Sink] around the receiver, [writes][StreamMutf8Sink.writeLength] the
 * [mutf8Length][CharArray.mutf8Length] of the [characters], and then [writes][StreamMutf8Sink.writeFromArray] the
 * [characters] themselves.
 *
 * In code, that would be:
 * ```kotlin
 * val mutf8Length = characters.mutf8Length(startIndex = range.first, endIndex = range.last + 1)
 * val sink = StreamMutf8Sink(stream = this, bytesPerWrite)
 * sink.writeLength(mutf8Length)
 * sink.writeFromArray(characters, range)
 * ```
 *
 * See the documentation for those methods (linked below) for their limitations, exceptions, parameters, and their
 * meanings.
 *
 * @see[CharArray.mutf8Length]
 * @see[StreamMutf8Sink]
 * @see[StreamMutf8Sink.writeLength]
 * @see[StreamMutf8Sink.writeFromArray]
 */
@Throws(IOException::class)
fun OutputStream.writeMutf8Array(
    characters: CharArray,
    range: IntRange = 0..characters.lastIndex,
    bytesPerWrite: Int = DEFAULT_BYTES_PER_WRITE
) = writeMutf8Array(characters, startIndex = range.first, endIndex = range.last + 1, bytesPerWrite)

private inline fun <C> OutputStream.writeMutf8(
    characters: C,
    bytesPerWrite: Int,
    getMutf8Length: C.() -> Long,
    writeCharacters: Mutf8Sink.() -> Unit
) {
    require(bytesPerWrite >= 1) { "bytesPerWrite must be at least 1, not $bytesPerWrite" }

    val mutf8Length = characters.getMutf8Length()
    require(mutf8Length >= 0) { "mutf8Length must be at least 0, not $mutf8Length" }
    require(mutf8Length <= MAX_MUTF8_LENGTH) { "String needs $mutf8Length bytes to be encoded, but the maximum is $MAX_MUTF8_LENGTH" }

    val sink = StreamMutf8Sink(stream = this, bytesPerWrite)
    sink.writeLength(mutf8Length.toInt())
    sink.writeCharacters()
}