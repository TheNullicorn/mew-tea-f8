import me.nullicorn.mewteaf8.gradle.*
import me.nullicorn.mewteaf8.gradle.targets.*

repositories {
    mavenCentral()
}

@Suppress("UNUSED_VARIABLE", "OPT_IN_USAGE")
kotlin {
    // Exclude Windows 32-bit & WebAssembly 32-bit because ktor-io doesn't support those targets.
    registerTargetsForMewTeaF8(project, /* excludedTargets = */ ::mingwX86, ::wasm32, ::wasm)
    configureSourceSetsForMewTeaF8(project)

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":mew-tea-f8-common"))
                api(libs.ktor.io)
            }
        }
    }
}