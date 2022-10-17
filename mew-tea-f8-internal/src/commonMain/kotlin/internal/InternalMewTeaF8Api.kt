package me.nullicorn.mewteaf8.internal

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.*

/**
 * Indicates that the annotated member is intended to be used inside mew-tea-f8 only.
 *
 * Using methods, fields, or classes that use this annotation is not supported; they may change between versions without
 * warning, potentially causing existing code to break. If you find yourself needing to use one, open an issue
 * [here](https://github.com/TheNullicorn/mew-tea-f8/issues/new) with your use-case.
 */
@Target(CLASS, PROPERTY, FUNCTION)
@Retention(BINARY)
@RequiresOptIn
annotation class InternalMewTeaF8Api