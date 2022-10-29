package me.nullicorn.mewteaf8.Mutf8Sink

import me.nullicorn.mewteaf8.Mutf8Sink

internal object BlackHoleMutf8Sink : Mutf8Sink() {
    override fun writeBytes(bytes: ByteArray, untilIndex: Int) {
        // Do nothing with the bytes.
    }
}