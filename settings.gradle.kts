rootProject.name = "mew-tea-f8"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

// Published modules.
include("mew-tea-f8-common")
include("mew-tea-f8-okio")
include("mew-tea-f8-ktor-io")

// Internal modules.
include("mew-tea-f8-test-helpers")