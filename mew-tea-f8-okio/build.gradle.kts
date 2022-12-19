import me.nullicorn.mewteaf8.gradle.*

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    id(libs.plugins.kotlin.documentation.get().pluginId)
    id(libs.plugins.binary.compatibility.get().pluginId)
    id(libs.plugins.intellij.settings.get().pluginId)
    id("maven-publish")
    id("signing")
}

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
            api(project(":mew-tea-f8-core"))
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