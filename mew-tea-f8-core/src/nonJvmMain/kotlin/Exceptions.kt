package me.nullicorn.mewteaf8

actual open class IOException actual constructor(message: String, cause: Throwable?) : Exception(message, cause)

actual open class EOFException actual constructor(message: String) : IOException(message)

actual open class UTFDataFormatException actual constructor(message: String) : IOException(message)