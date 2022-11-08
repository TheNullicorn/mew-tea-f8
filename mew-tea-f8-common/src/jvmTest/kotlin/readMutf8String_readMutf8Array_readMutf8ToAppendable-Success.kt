package me.nullicorn.mewteaf8

import java.io.ByteArrayInputStream
import java.io.InputStream
import me.nullicorn.mewteaf8.readMutf8Array as actual_readMutf8Array
import me.nullicorn.mewteaf8.readMutf8String as actual_readMutf8String
import me.nullicorn.mewteaf8.readMutf8ToAppendable as actual_readMutf8ToAppendable

class ReadMutf8SuccessfullyTests : ReadMutf8SuccessfullySharedTests<InputStream>() {

    override val readMutf8String = InputStream::actual_readMutf8String

    override val readMutf8Array = InputStream::actual_readMutf8Array

    override val readMutf8ToAppendable = InputStream::actual_readMutf8ToAppendable

    override fun Input(bytes: List<Byte>) = ByteArrayInputStream(bytes.toByteArray())
}