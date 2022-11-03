import me.nullicorn.mewteaf8.gradle.*
import me.nullicorn.mewteaf8.gradle.targets.*

repositories {
    mavenCentral()
}

@Suppress("UNUSED_VARIABLE")
kotlin {
    // Exclude Windows 32-bit & WebAssembly 32-bit because okio doesn't support those targets.
    registerTargetsForMewTeaF8(project, exceptIf = { it.preset?.name == "mingwX86" || it.preset?.name == "wasm32" })
    configureSourceSetsForMewTeaF8(project)

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":mew-tea-f8-common"))
                api(libs.okio)
            }
        }
    }
}