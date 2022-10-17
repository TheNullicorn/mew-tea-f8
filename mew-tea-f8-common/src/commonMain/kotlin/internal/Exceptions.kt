package me.nullicorn.mewteaf8.internal

@InternalMewTeaF8Api
sealed class ModifiedUtf8Exception(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

@InternalMewTeaF8Api
class ModifiedUtf8IOException(message: String? = null, cause: Throwable? = null) : ModifiedUtf8Exception(message, cause)

@InternalMewTeaF8Api
class CharacterStartedTooLateException(charSize: Int, bytesLeft: Int) : ModifiedUtf8Exception(
    message = "A ${charSize}-byte character was started with${if (bytesLeft > 0) "only" else ""} $bytesLeft bytes left in the string"
)

@InternalMewTeaF8Api
class MalformedPrimaryByteException(byte: Int) : ModifiedUtf8Exception(
    message = "Byte #1 of a character has the bits ${byte.toBinaryOctet()}\u2082 which doesn't match any of the expected patterns: 0xxxxxxx\u2082 or 110xxxxx\u2082 or 1110xxxx\u2082"
)

@InternalMewTeaF8Api
class MalformedSecondaryByteException(byte: Int, charSize: Int, byteOffset: Int) : ModifiedUtf8Exception(
    message = "Byte #${byteOffset + 1} of a ${charSize}-byte character has the bits ${byte.toBinaryOctet()}\u2082 which don't match the expected pattern 10xxxxxx\u2082"
)

private fun Int.toBinaryOctet(): String =
    (this and 0xFF).toString(radix = 2).padStart(length = 8, padChar = '0')