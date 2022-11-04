package me.nullicorn.mewteaf8.gradle.targets

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetDsl

typealias TargetRegistrator = () -> Any

/**
 * Registers the available Kotlin targets [configured][mewTeaF8BuildTargets] for the [project]'s current environment.
 *
 * This does not necessarily register *every* target that fits the criteria, especially obscure targets, but it does
 * register the most common ones for that environment.
 *
 * If any targets should not be registered, such as those that aren't supported by a dependency of ours, they should be
 * passed in via the [excludedTargets] parameter.
 *
 * @param[project] The project whose [configured][mewTeaF8BuildTargets] targets will be registered.
 * @param[excludedTargets] Any targets that should not be registered. Each element should be a reference to the target's
 * registration method that doesn't require any parameters.
 *
 * For example, `::jvm`, which is a reference to the overload of
 * [kotlin.jvm()][KotlinTargetContainerWithPresetFunctions.jvm] without any parameters.
 */
fun KotlinMultiplatformExtension.registerTargetsForMewTeaF8(
    project: Project,
    vararg excludedTargets: TargetRegistrator
) {
    val environment = project.mewTeaF8BuildTargets

    if (environment.includesCommon)
        registerCommonTargets(*excludedTargets)

    if (environment.includesNative)
        registerNativeTargets(project, *excludedTargets)
}

private fun KotlinMultiplatformExtension.registerCommonTargets(vararg excludedTargets: TargetRegistrator) {
    // Target the JVM (Kotlin, Java, etc).
    if (::jvm !in excludedTargets)
        jvm {
            // Target Java 8.
            val targetVersion = JavaVersion.VERSION_1_8.toString()
            compilations.all {
                kotlinOptions.jvmTarget = targetVersion
            }
        }


    if (::js !in excludedTargets)
        js(BOTH) {
            // Target both client-side (Browser) & server-side (Node.js) JavaScript.
            val subTargets: List<(KotlinJsSubTargetDsl.() -> Unit) -> Unit> =
                listOf(::nodejs, ::browser)

            // Register & configure each target listed above.
            for (jsTarget in subTargets) {
                jsTarget {
                    testTask {
                        useMocha {
                            // Disable the time limit on tests.
                            timeout = "0s"
                        }
                    }
                }
            }
        }

    // Target WebAssembly.
    if (::wasm !in excludedTargets)
        wasm {
            // Target Node.js, Browser JS, and D8 (V8 Shell) JS
            val subTargets: List<(KotlinJsSubTargetDsl.() -> Unit) -> Unit> =
                listOf(::nodejs, ::browser, ::d8)

            // Register & configure each target listed above.
            for (wasmTarget in subTargets) {
                wasmTarget {
                    testTask {
                        useMocha {
                            // Disable the time limit on tests.
                            timeout = "0s"
                        }
                    }
                }
            }
        }
}

private fun KotlinMultiplatformExtension.registerNativeTargets(
    project: Project,
    vararg excludedTargets: TargetRegistrator
) {
    // A collection of method references to the functions that register each target, like `linuxX64()`.
    val nativeTargets: MutableSet<TargetRegistrator > = HashSet()

    // Target 32-bit WebAssembly, regardless of the current platform.
    nativeTargets += ::wasm32

    // Target Windows, macOS,or Linux, whichever one of those the current OS is.
    val hostOs = System.getProperty("os.name")
    when {
        hostOs.startsWith("Windows") -> {
            nativeTargets += ::mingwX86
            nativeTargets += ::mingwX64
        }

        hostOs == "Linux" -> {
            nativeTargets += ::linuxX64
            nativeTargets += ::linuxArm32Hfp
            nativeTargets += ::linuxArm64
            nativeTargets += ::linuxMips32
            nativeTargets += ::linuxMipsel32
        }

        hostOs == "Mac OS X" -> {
            // macOS
            nativeTargets += ::macosX64
            nativeTargets += ::macosArm64

            // iOS
            nativeTargets += ::iosX64
            nativeTargets += ::iosArm32
            nativeTargets += ::iosArm64
            nativeTargets += ::iosSimulatorArm64

            // watchOS
            nativeTargets += ::watchosX86
            nativeTargets += ::watchosX64
            nativeTargets += ::watchosArm32
            nativeTargets += ::watchosArm64
            nativeTargets += ::watchosSimulatorArm64

            // tvOS
            nativeTargets += ::tvosX64
            nativeTargets += ::tvosArm64
            nativeTargets += ::tvosSimulatorArm64

            // GLaDOS
            // targets += ::gladosArm64
        }

        else -> {
            project.logger.warn("Current OS is not known to support any platform-specific targets: \"$hostOs\"")
        }
    }

    for (registerTarget in nativeTargets - excludedTargets)
        registerTarget()
}