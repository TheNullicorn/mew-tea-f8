package me.nullicorn.mewteaf8

import okio.Buffer
import okio.Source
import me.nullicorn.mewteaf8.readMutf8Array as actual_readMutf8Array
import me.nullicorn.mewteaf8.readMutf8String as actual_readMutf8String
import me.nullicorn.mewteaf8.readMutf8ToAppendable as actual_readMutf8ToAppendable

class ReadMutf8SuccessfullyTests : ReadMutf8SuccessfullySharedTests<Source>() {

    override val readMutf8String = Source::actual_readMutf8String

    override val readMutf8Array = Source::actual_readMutf8Array

    override val readMutf8ToAppendable = Source::actual_readMutf8ToAppendable

    override fun Input(bytes: List<Byte>) = Buffer().apply { write(bytes.toByteArray()) }
}