package me.nullicorn.mewteaf8.gradle.targets

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetDsl

/**
 * Registers the available Kotlin targets [configured][mewTeaF8BuildTargets] for the [project]'s current environment.
 *
 * This does not necessarily register *every* target that fits the criteria, especially obscure targets, but it does
 * register the most common ones for that environment.
 *
 * @param[project] The project whose [configured][mewTeaF8BuildTargets] targets will be registered.
 * @param[exceptIf] A predicate that unregisters all targets that match it, such as those not supported by a dependency
 * of the project, meaning we can't support them either for that module.
 */
fun KotlinMultiplatformExtension.registerTargetsForMewTeaF8(
    project: Project,
    exceptIf: (KotlinTarget) -> Boolean = { false }
) {
    val environment = project.mewTeaF8BuildTargets

    if (environment.includesCommon)
        registerCommonTargets()

    if (environment.includesNative)
        registerNativeTargets()

    // Remove any targets (& their source-sets) that are excluded by the `exceptIf` predicate.
    val targetsToRemove = targets.filter(exceptIf)
    for (target in targetsToRemove) {
        targets.remove(target)
        sourceSets.removeIf { it.name.startsWith(target.name) }
    }
}

private fun KotlinMultiplatformExtension.registerCommonTargets() {
    // Target the JVM (Kotlin, Java, etc).
    jvm {
        // Target Java 8.
        val targetVersion = JavaVersion.VERSION_1_8.toString()
        compilations.all {
            kotlinOptions.jvmTarget = targetVersion
        }
    }

    // Target the new (IR) and legacy compiler backends for Kotlin/JS.
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

    // Target WebAssembly. Even though it's a "native" target, it's platform-independent, so we build it along with
    // JVM and JS.
    wasm32()
}

private fun KotlinMultiplatformExtension.registerNativeTargets() {
    // A collection of method references to the functions that register each target, like `linuxX64()`.
    val nativeTargets: MutableSet<() -> Any> = HashSet()

    // Target Windows, macOS and Linux.
    val hostOs = System.getProperty("os.name")
    when {
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

        hostOs.startsWith("Windows") -> {
            nativeTargets += ::mingwX86
            nativeTargets += ::mingwX64
        }
    }

    for (registerTarget in nativeTargets)
        registerTarget()
}