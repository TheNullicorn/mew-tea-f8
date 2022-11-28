import me.nullicorn.mewteaf8.gradle.configureSourceSetsForMewTeaF8
import me.nullicorn.mewteaf8.gradle.publishing.configureDocumentationForMewTeaF8
import me.nullicorn.mewteaf8.gradle.targets.registerTargetsForMewTeaF8

repositories {
    mavenCentral()
}

@Suppress("UNUSED_VARIABLE", "OPT_IN_USAGE")
kotlin {
    // Exclude targets that okio doesn't support.
    registerTargetsForMewTeaF8(
        project,
        /* excludedTargets = */
        ::linuxArm32Hfp,
        ::linuxMipsel32,
        ::linuxMips32,
        ::linuxArm64,
        ::mingwX86,
        ::iosArm32,
        ::wasm32,
        ::wasm
    )
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