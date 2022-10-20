@file:JvmSynthetic

package me.nullicorn.mewteaf8.internal

import kotlin.jvm.JvmSynthetic

@InternalMutf8Api
sealed class Mutf8Exception(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

@InternalMutf8Api
class Mutf8IOException(message: String? = null, cause: Throwable? = null) : Mutf8Exception(message, cause)

@InternalMutf8Api
class Mutf8EOFException(message: String? = null, cause: Throwable? = null) : Mutf8Exception(message, cause)

@InternalMutf8Api
class Mutf8TruncatedCharacterException(charSize: Int, bytesLeft: Int) : Mutf8Exception(
    message = "A ${charSize}-byte character was started with${if (bytesLeft > 0) "only" else ""} $bytesLeft bytes left in the string"
)

@InternalMutf8Api
class Mutf8MalformedPrimaryByteException(byte: Int) : Mutf8Exception(
    message = "Byte #1 of a character has the bits ${byte.toBinaryOctet()}\u2082 which doesn't match any of the expected patterns: 0xxxxxxx\u2082 or 110xxxxx\u2082 or 1110xxxx\u2082"
)

@InternalMutf8Api
class Mutf8MalformedSecondaryByteException(byte: Int, charSize: Int, byteOffset: Int) : Mutf8Exception(
    message = "Byte #${byteOffset + 1} of a ${charSize}-byte character has the bits ${byte.toBinaryOctet()}\u2082 which don't match the expected pattern 10xxxxxx\u2082"
)

private fun Int.toBinaryOctet(): String =
    (this and 0xFF).toString(radix = 2).padStart(length = 8, padChar = '0')