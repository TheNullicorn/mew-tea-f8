package me.nullicorn.mewteaf8.gradle

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

private val INCLUDED_EXTENSIONS = setOf(".kt", ".kts", ".java")
private val EXCLUDED_EXTENSIONS = setOf(".sample.kt", ".sample.kts", ".sample.java")

fun KotlinMultiplatformExtension.configureSourceSetsForMewTeaF8(project: Project) {
    val environment = MewTeaF8BuildProperties.getBuildTargetsOf(project)

    @Suppress("UNUSED_VARIABLE")
    sourceSets.run {
        val commonMain = getByName("commonMain")
        val commonTest = getByName("commonTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        // Parent for all source-sets for non-JVM targets.
        val nonJvmMain = create("nonJvmMain") {
            dependsOn(commonMain)
        }

        // Parent for all source-sets that compile to machine code.
        val nativeMain = create("nativeMain") {
            dependsOn(nonJvmMain)
        }

        // If we're building targets that can be built on any platform, then configure JVM, JS, and WASM source-sets.
        if (environment.includesCommon) {
            val jvmMain = getByName("jvmMain")

            val jsMain = getByName("jsMain") {
                dependsOn(nonJvmMain)
            }

            for (wasmSourceSet in getMainSourceSetsForPlatform(KotlinPlatformType.wasm))
                wasmSourceSet.dependsOn(nonJvmMain)
        }

        // If we're building targets that can only be built on the current platform, then configure those.
        if (environment.includesNative)
            for (nativeSourceSet in getMainSourceSetsForPlatform(KotlinPlatformType.native))
                nativeSourceSet.dependsOn(nativeMain)

        // Exclude non-source files, such as markdown documentation (`*.md`) and code samples (`*.sample.kt`).
        configureEach {
            kotlin {
                include { file -> INCLUDED_EXTENSIONS.any { ext -> file.name.endsWith(ext, ignoreCase = true) } }
                exclude { file -> EXCLUDED_EXTENSIONS.any { ext -> file.name.endsWith(ext, ignoreCase = true) } }
            }
        }
    }
}

private fun KotlinMultiplatformExtension.getMainSourceSetsForPlatform(platform: KotlinPlatformType) =
    HashSet<KotlinSourceSet>().apply {
        // Find all targets for the `platform` that will actually be compiled (at least 1 compilation).
        for (target in targets)
            if (target.platformType == platform && target.compilations.isNotEmpty()) {
                val mainSourceSetName = "${target.name}Main"
                val mainSourceSet = sourceSets.getByName(mainSourceSetName)
                add(mainSourceSet)
            }
    }