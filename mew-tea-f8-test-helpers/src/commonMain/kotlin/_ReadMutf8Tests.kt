package me.nullicorn.mewteaf8

interface ReadMutf8Tests<Input> {

    val readMutf8String: Input.() -> String

    val readMutf8Array: Input.() -> CharArray

    val readMutf8ToAppendable: Input.(Appendable) -> Unit

    fun Input(bytes: List<Byte>): Input
}

internal fun <Input> ReadMutf8Tests<Input>.Input(populate: MutableList<Byte>.() -> Unit): Input =
    Input(bytes = buildList(populate))