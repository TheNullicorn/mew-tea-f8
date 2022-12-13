import me.nullicorn.mewteaf8.gradle.*

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.binary.compatibility)
}

repositories {
    mavenCentral()
}

`mew-tea-f8` {
    // Disable Maven publications & documentation for this module since it's only used internally for unit tests. The
    // same is done for the "binary-compatibility-validator" plugin later in this file because it doesn't have a public
    // API that we need to worry about changes to.
    publishing.enabled = false
    documentation.enabled.set(false)

    compilation {
        @Suppress("OPT_IN_USAGE")
        registerTargets(/* exclude = */ kotlin::wasm)
    }
}

@Suppress("UNUSED_VARIABLE")
kotlin.sourceSets {
    val commonMain by getting {
        dependencies {
            api(project(":mew-tea-f8-common"))
            implementation(libs.bundles.kotlin.test)
        }
    }

    // If we're building for JVM, also explicitly depend on `kotlin-test-junit`. See the note below for details.
    if (`mew-tea-f8`.compilation.buildMode.includesJvm)
        getByName("jvmMain") {
            dependencies {
                // "kotlin-test" is set up weird and for some reason the Java API isn't in the classpath at compile
                // time. It might have something to do with us depending on it in a "main" source-set instead of a
                // "test" one? Either way, that's why it's explicitly declared here, whereas normally the dependency
                // `kotlin("test")` would transitively add it to the classpath.
                implementation(libs.bundles.kotlin.test.jvm)
            }
        }
}

// This module is only used internally (it's not published), so we don't have to worry about its API changing.
apiValidation.validationDisabled = true