package me.nullicorn.mewteaf8

/**
 * Thrown when issue occurs while trying to read or write data to an external location, like a file or network socket.
 *
 * In the JVM, this is an alias for `java.io.IOException`. On other platforms, a placeholder class exists.
 */
expect open class IOException(message: String, cause: Throwable? = null) : Exception

/**
 * Thrown when the end of a binary sequence is reached earlier than expected while reading.
 *
 * In the JVM, this is an alias for `java.io.EOFException`. On other platforms, a placeholder class exists.
 */
expect open class EOFException(message: String) : IOException

/**
 * Thrown when malformed Modified UTF-8 data is read.
 *
 * In the JVM, this is an alias for `java.io.UTFDataFormatException`. On other platforms, a placeholder class exists.
 */
expect open class UTFDataFormatException(message: String) : IOException