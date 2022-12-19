import me.nullicorn.mewteaf8.gradle.*

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id(libs.plugins.kotlin.documentation.get().pluginId)
    id(libs.plugins.intellij.settings.get().pluginId)
    alias(libs.plugins.binary.compatibility) apply false
}

group = "me.nullicorn"
version = "0.0.1"

repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = rootProject.libs.plugins.kotlin.multiplatform.get().pluginId)
    apply(plugin = rootProject.libs.plugins.binary.compatibility.get().pluginId)
    apply(plugin = rootProject.libs.plugins.intellij.settings.get().pluginId)

    repositories {
        mavenCentral()
    }

    // Inherit the root project's group & version.
    group = rootProject.group
    version = rootProject.version

    `mew-tea-f8` {
        publishing {
            fillPomUsingGitHub()
        }
    }
}