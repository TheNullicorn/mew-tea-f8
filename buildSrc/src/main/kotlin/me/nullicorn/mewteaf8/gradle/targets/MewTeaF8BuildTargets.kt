package me.nullicorn.mewteaf8.gradle.targets

/**
 * Determines which Kotlin targets will be registered when building a project.
 *
 * Configure this using the `mew-tea-f8-target` property or the `MEW_TARGET` environment variable.
 */
enum class MewTeaF8BuildTargets(val configName: String, val includesCommon: Boolean, val includesNative: Boolean) {

    /**
     * Builds platform-independent targets. Use "`common`" to select this.
     *
     * This includes:
     * - JVM
     * - JavaScript
     *    - Both IR & Legacy compilers
     *    - Both Node.js & Browser
     * - WebAssembly
     */
    COMMON_ONLY("common", true, false),

    /**
     * Builds platform-specific targets. Use "`native`" to select this.
     *
     * The built targets depend on the operating system being built on:
     * - Windows:
     *    - Includes all Windows (`mingw`) targets.
     * - Linux:
     *    - Includes all Linux targets.
     * - macOS:
     *    - Includes all macOS targets.
     *    - Includes all iOS targets.
     *    - Includes all watchOS targets.
     *    - Includes all tvOS targets.
     */
    NATIVE_ONLY("native", false, true),

    /**
     * Builds both [platform-independent][COMMON_ONLY] and [platform-specific][NATIVE_ONLY] targets. Use "`all`" to
     * to select this.
     *
     * Includes all targets documented at [COMMON_ONLY], and those documented at [NATIVE_ONLY] for the current operating
     * system.
     */
    COMMON_AND_NATIVE("all", true, true),
}