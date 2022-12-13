package me.nullicorn.mewteaf8.gradle.compilation

import me.nullicorn.mewteaf8.gradle.kotlin
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.mocha.KotlinMocha

/**
 * Configuration for the project's compilations / builds.
 */
class MewCompilationConfig(private val project: Project) {

    /**
     * Whether [registerTargets] has been called yet by the project.
     *
     * If not, an exception should be thrown before any tasks because all modules must include at least 1 target or else
     * nothing will be built, which is almost always caused by misconfiguration.
     */
    private var didRegisterTargets = false

    /**
     * Determines which Kotlin targets will be considered by Gradle, including unit tests, documentation, compilation
     * and publishing.
     *
     * The Gradle property for configuring this is `mew-tea-f8.compilation.mode`, which should be set to one of the
     * following strings:
     * - "`all`" for [MewBuildMode.ALL]
     * - "`common`" for [MewBuildMode.COMMON]
     * - "`native`" for [MewBuildMode.NATIVE]
     */
    val buildMode: MewBuildMode = run {
        // Get the configured build mode. If no mode was selected, compile only platform-independent targets by default.
        val selectedBuildMode: String? =
            project.properties["mew-tea-f8.compilation.mode"] as? String
                ?: return@run MewBuildMode.COMMON

        // Set it to the `MewBuildMode` whose `configValue` is the same as the supplied `selectedTargets`.
        for (buildMode in MewBuildMode.values())
            if (selectedBuildMode.equals(buildMode.configName, ignoreCase = true))
                return@run buildMode

        // No `MewBuildMode` matches the selected one.
        throw InvalidUserDataException("Unknown compilation mode: \"$selectedBuildMode\"")
    }

    // Allows this to be configured in brackets `{ }` like most things in Gradle.
    operator fun invoke(action: MewCompilationConfig.() -> Unit): MewCompilationConfig {
        action(this)
        return this
    }

    init {
        // Make sure the project's buildscript registers its Kotlin targets before it's done being evaluated.
        project.afterEvaluate {
            if (!didRegisterTargets)
                throw GradleException("`mew-tea-f8`.compilation.registerTargets() was never called")
        }
    }

    /**
     * Registers all Kotlin targets corresponding to the configured [build mode][buildMode].
     *
     * If any targets should explicitly be disabled, like if one of the project's irreplacible dependencies doesn't
     * support them, then they can be [excluded] via a reference to their registration function in an instance of
     * [KotlinMultiplatformExtension]. For example, supplying `kotlin::mingwX86` (referencing
     * [this][KotlinMultiplatformExtension.mingwX86] method) would completely disable the Windows (x86) target in all
     * areas of the project (docs, compilations, tests, etc).
     *
     * Excluded targets don't have to be part of the current [build mode][buildMode]. For example, it's acceptable to
     * exclude a native target (like `kotlin::linuxX64`) when the mode is [MewBuildMode.COMMON], which would never have
     * native targets anyway.
     *
     * @param[excluded] References to the registration function of any platforms that should be disabled.
     */
    fun registerTargets(vararg excluded: () -> Any) {
        val kotlin = project.kotlin

        this.didRegisterTargets = true

        // If we're building platform-independent targets, then configure JVM, JavaScript, and WebAssembly  targets.
        if (buildMode.includesCommon) {
            // If a JVM compilation(s) is going to be made, set the target version to Java 8.
            if (kotlin::jvm !in excluded)
                kotlin.jvm {
                    compilations.all {
                        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
                    }
                }

            // If a JavaScript compilation(s) is going to be made, target Node.js and Browser, both for Kotlin's IR
            // backend and the legacy one.
            if (kotlin::js !in excluded)
                kotlin.js(compiler = KotlinJsCompilerType.BOTH) {
                    // Target both client-side (Browser) & server-side (Node.js) JavaScript.
                    val subTargets: List<(KotlinJsSubTargetDsl.() -> Unit) -> Unit> =
                        listOf(::nodejs, ::browser)

                    // Register & configure each target listed above.
                    for (jsTarget in subTargets)
                        jsTarget {
                            testTask {
                                // Disable the time limit on tests.
                                testFramework = KotlinMocha(compilation, path).also {
                                    it.timeout = "0s"
                                }
                            }
                        }
                }

            // If a WebAssembly compilation(s) is going to be made, target Node.js, Browser, and V8 Shell (D8).
            if (kotlin::wasm !in excluded)
                kotlin.wasm {
                    // Target Node.js, Browser JS, and D8 (V8 Shell) JS
                    val subTargets: List<(KotlinJsSubTargetDsl.() -> Unit) -> Unit> =
                        listOf(::nodejs, ::browser, ::d8)

                    // Register & configure each target listed above.
                    for (wasmTarget in subTargets) {
                        wasmTarget {
                            testTask {
                                // Disable the time limit on tests.
                                testFramework = KotlinMocha(compilation, path).also {
                                    it.timeout = "0s"
                                }
                            }
                        }
                    }
                }
        }

        // If we're building platform-dependent targets, register any that aren't specifically excluded.
        if (buildMode.includesNative) {
            val nativeTargets = mutableSetOf<() -> Any>()

            // WebAssembly (legacy).
            nativeTargets += kotlin::wasm32

            // Windows
            nativeTargets += kotlin::mingwX86
            nativeTargets += kotlin::mingwX64

            // Linux
            nativeTargets += kotlin::linuxX64
            nativeTargets += kotlin::linuxArm32Hfp
            nativeTargets += kotlin::linuxArm64
            nativeTargets += kotlin::linuxMips32
            nativeTargets += kotlin::linuxMipsel32

            // macOS
            nativeTargets += kotlin::macosX64
            nativeTargets += kotlin::macosArm64

            // iOS
            nativeTargets += kotlin::iosX64
            nativeTargets += kotlin::iosArm32
            nativeTargets += kotlin::iosArm64
            nativeTargets += kotlin::iosSimulatorArm64

            // watchOS
            nativeTargets += kotlin::watchosX86
            nativeTargets += kotlin::watchosX64
            nativeTargets += kotlin::watchosArm32
            nativeTargets += kotlin::watchosArm64
            nativeTargets += kotlin::watchosSimulatorArm64

            // tvOS
            nativeTargets += kotlin::tvosX64
            nativeTargets += kotlin::tvosArm64
            nativeTargets += kotlin::tvosSimulatorArm64

            // GLaDOS
            // targets += ::gladosArm64

            for (registerTarget in nativeTargets - excluded)
                registerTarget()
        }
    }
}