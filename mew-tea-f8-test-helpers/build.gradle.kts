import me.nullicorn.mewteaf8.gradle.configureSourceSetsForMewTeaF8
import me.nullicorn.mewteaf8.gradle.targets.registerTargetsForMewTeaF8
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.binary.compatibility)
}

repositories {
    mavenCentral()
}

@Suppress("UNUSED_VARIABLE", "OPT_IN_USAGE")
kotlin {
    registerTargetsForMewTeaF8(project, /* excludedTargets = */ ::wasm)
    configureSourceSetsForMewTeaF8(project)

    // This module is only used internally (it's not published), so we don't have to worry about its API changing.
    apiValidation.validationDisabled = true

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":mew-tea-f8-common"))
                api(kotlin("test"))
            }
        }

        // If we're building for JVM, also explicitly depend on `kotlin-test-junit`. See the note below for details.
        if (targets.any { it.platformType == KotlinPlatformType.jvm && it.compilations.isNotEmpty() }) {
            val jvmMain by getting {
                dependencies {
                    // "kotlin-test" is set up weird and for some reason the Java API isn't in the classpath at compile
                    // time. Might have something to do with depending on it in a `main` source-set instead of a `test`
                    // one? Either way, that's why it's explicitly declared here, whereas normally the dependency in
                    // `commonMain` (or `commonTest`) would transitively add it to the classpath.
                    implementation(kotlin("test-junit"))
                }
            }
        }
    }
}