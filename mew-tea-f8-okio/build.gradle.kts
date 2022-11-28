import me.nullicorn.mewteaf8.gradle.*
import me.nullicorn.mewteaf8.gradle.publishing.*
import me.nullicorn.mewteaf8.gradle.targets.*

repositories {
    mavenCentral()
}

@Suppress("UNUSED_VARIABLE", "OPT_IN_USAGE")
kotlin {
    // Exclude Windows 32-bit & WebAssembly 32-bit because okio doesn't support those targets.
    registerTargetsForMewTeaF8(project, /* excludedTargets = */ ::mingwX86, ::wasm32, ::wasm)
    configureSourceSetsForMewTeaF8(project)
    configureDocumentationForMewTeaF8()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":mew-tea-f8-common"))
                api(libs.okio)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(project(":mew-tea-f8-test-helpers"))
            }
        }
    }
}