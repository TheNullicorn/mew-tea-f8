package me.nullicorn.mewteaf8

import java.io.InputStream

/**
 * A [Mutf8Source] that reads its data from an [InputStream].
 *
 * @param[stream] The [InputStream] to read from.
 */
class StreamMutf8Source(private val stream: InputStream) : Mutf8Source() {

    override fun readLength(): Int {
        val byte1 = readOctetOrElseThrow { "Data ended before the first byte of the length" }
        val byte2 = readOctetOrElseThrow { "Data ended before the second byte of the length" }
        return (byte1 shl 8) or byte2
    }

    override fun readBytes(amount: Int): ByteArray {
        require(amount >= 0) { "amount must be at least 0, not $amount" }

        val bytes = ByteArray(size = amount)
        val amountRead = stream.read(bytes)
        if (amountRead < amount)
            throw EOFException("Stream only returned $amountRead out of $amount expected bytes")

        return bytes
    }

    /**
     * Reads the next byte from the [stream] and returns its 8 least-significant bits (hence "octet").
     *
     * @param[message] The message set for an [EOFException], if thrown.
     * @return the octet that was read from the stream. This will always be in the range `0 .. 255`.
     *
     * @throws[EOFException] if [stream.read()][InputStream.read] returns `-1`, indicating it has no bytes remaining.
     */
    private inline fun readOctetOrElseThrow(message: () -> String): Int {
        val byte = stream.read()
        if (byte == -1) throw EOFException(message())
        return byte and 0xFF
    }
}