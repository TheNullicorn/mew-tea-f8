package me.nullicorn.mewteaf8.Mutf8Source

import me.nullicorn.mewteaf8.Mutf8Source

abstract class Mutf8SourceTests<Source : Mutf8Source> {

    protected abstract fun Source(input: List<Byte> = emptyList()): Source

    protected inline fun Source(populate: MutableList<Byte>.() -> Unit): Source = Source(input = buildList(populate))
}