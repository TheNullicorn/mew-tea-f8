import me.nullicorn.mewteaf8.gradle.*

repositories {
    mavenCentral()
}

@Suppress("OPT_IN_USAGE")
`mew-tea-f8` {
    compilation {
        // Exclude targets that okio doesn't support.
        registerTargets(/* excluded = */
            kotlin::linuxArm32Hfp,
            kotlin::linuxMipsel32,
            kotlin::linuxMips32,
            kotlin::linuxArm64,
            kotlin::mingwX86,
            kotlin::iosArm32,
            kotlin::wasm32,
            kotlin::wasm
        )
    }
}

@Suppress("UNUSED_VARIABLE")
kotlin.sourceSets {
    val commonMain by getting {
        dependencies {
            api(project(":mew-tea-f8-common"))
            api(libs.okio)
        }
    }
    val commonTest by getting {
        dependencies {
            implementation(libs.bundles.kotlin.test)
            implementation(project(":mew-tea-f8-test-helpers"))
        }
    }
}