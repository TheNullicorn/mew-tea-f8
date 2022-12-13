package me.nullicorn.mewteaf8.gradle.compilation

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Groups of Kotlin Multiplatform targets which can be enabled for the project one-at-a-time, determining what platforms
 * will be included in unit testing, documentation generation, compilation/building, etc.
 *
 * @property[configName] The string used to select the mode via the appropriate Gradle property.
 * @property[includesCommon] Whether the mode's Kotlin targets include those not considered to be "native". In
 * particular, this includes Kotlin's JVM, JavaScript, and WebAssembly (the new "wasm") targets.
 * @property[includesNative] Whether the mode's Kotlin targets include those considered to be "native". In particular,
 * this includes targets for Darwin (Apple), Windows, and Linux operating systems, as well as WebAssembly (the old
 * "wasm32" target).
 */
enum class MewBuildMode(
    internal val configName: String,
    internal val includesCommon: Boolean,
    internal val includesNative: Boolean
) {
    /**
     * Includes all targets from both [COMMON] and [NATIVE].
     */
    ALL("all", includesCommon = true, includesNative = true),

    /**
     * Includes targets that Kotlin considers to be non-"native".
     *
     * The following targets, unless otherwise [excluded][MewCompilationConfig.registerTargets], are registered for this
     * mode:
     * - [jvm][KotlinMultiplatformExtension.jvm]
     * - [js][KotlinMultiplatformExtension.js]
     * - [wasm32][KotlinMultiplatformExtension.wasm32]
     */
    COMMON("common", includesCommon = true, includesNative = false),

    /**
     * Includes targets that Kotlin considers to be "native".
     *
     * The targets registered for this mode depend on the platform running Gradle. Unless otherwise
     * [excluded][MewCompilationConfig.registerTargets] from a module (namely if a primary dependency doesn't support
     * them), those targets are:
     * - All:
     *    - [wasm][KotlinMultiplatformExtension.wasm]
     * - Windows:
     *    - [mingwx86][KotlinMultiplatformExtension.mingwx86]
     *    - [mingwx64][KotlinMultiplatformExtension.mingwx64]
     * - Linux:
     *    - [linuxX64][KotlinMultiplatformExtension.linuxX64]
     *    - [linuxArm32Hfp][KotlinMultiplatformExtension.linuxArm32Hfp]
     *    - [linuxArm64][KotlinMultiplatformExtension.linuxArm64]
     *    - [linuxMips32][KotlinMultiplatformExtension.linuxMips32]
     *    - [linuxMipsel32][KotlinMultiplatformExtension.linuxMipsel32]
     * - macOS:
     *    - [macosX64][KotlinMultiplatformExtension.macosX64]
     *    - [macosArm64][KotlinMultiplatformExtension.macosArm64]
     *    - [iosX64][KotlinMultiplatformExtension.iosX64]
     *    - [iosArm32][KotlinMultiplatformExtension.iosArm32]
     *    - [iosArm64][KotlinMultiplatformExtension.iosArm64]
     *    - [iosSimulatorArm64][KotlinMultiplatformExtension.iosSimulatorArm64]
     *    - [watchosX86][KotlinMultiplatformExtension.watchosX86]
     *    - [watchosX64][KotlinMultiplatformExtension.watchosX64]
     *    - [watchosArm32][KotlinMultiplatformExtension.watchosArm32]
     *    - [watchosArm64][KotlinMultiplatformExtension.watchosArm64]
     *    - [watchosSimulatorArm64][KotlinMultiplatformExtension.watchosSimulatorArm64]
     *    - [tvosX64][KotlinMultiplatformExtension.tvosX64]
     *    - [tvosArm64][KotlinMultiplatformExtension.tvosArm64]
     *    - [tvosSimulatorArm64][KotlinMultiplatformExtension.tvosSimulatorArm64]
     */
    NATIVE("native", includesCommon = false, includesNative = true);

    /**
     * Whether the mode includes Kotlin's [JVM][KotlinMultiplatformExtension.jvm] target.
     */
    val includesJvm: Boolean
        get() = includesCommon
}