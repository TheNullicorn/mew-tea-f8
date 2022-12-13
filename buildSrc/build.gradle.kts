import org.jetbrains.gradle.ext.packagePrefix
import org.jetbrains.gradle.ext.settings
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    // Not using the entire `kotlin-dsl` plugin because it drags in other plugins that make Gradle think that "buildSrc"
    // should be a plugin too. When it realizes it's not, it leaves a warning in console before builds. See here for
    // details: https://github.com/gradle/gradle/pull/13073#issuecomment-830098521
    `kotlin-dsl-base`

    alias(libs.plugins.intellij.settings)
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    fun plugin(id: String, version: String) = "$id:${id}.gradle.plugin:$version"
    val kotlinVersion = libs.versions.kotlin.asProvider().get()
    val intellijPluginVersion = libs.versions.intellij.settings.get()

    // Used to configure Kotlin Multiplatform source-sets & target platforms.
    implementation(kotlin("gradle-plugin", version = kotlinVersion))

    // Used to configure documentation.
    implementation("org.jetbrains.dokka:dokka-base:$kotlinVersion")
    implementation(plugin("org.jetbrains.dokka", version = kotlinVersion))

    // Used to configure package-prefixes for IntelliJ users.
    implementation(plugin("org.jetbrains.gradle.plugin.idea-ext", version = intellijPluginVersion))

    // Used to automatically fill in "pom.xml" details when publishing to Maven Central.
    implementation(libs.github.api)
}

kotlin.sourceSets.configureEach {
    languageSettings.optIn("org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl")

    // Use a package-prefix so that we don't need actual directories for the base packages.
    for (sourceDir in kotlin.srcDirs)
        idea.module.settings.packagePrefix[sourceDir.toRelativeString(projectDir)] = "me.nullicorn.mewteaf8.gradle"
}

// Set the JVM target version to 1.8 for both Kotlin and Java because for some reason they're different in buildSrc.

val jvmTargetVersion = JavaVersion.VERSION_1_8.toString()

tasks.withType<JavaCompile>().configureEach {
    targetCompatibility = jvmTargetVersion
}

tasks.withType<KotlinCompile<KotlinJvmOptions>>().configureEach {
    kotlinOptions {
        jvmTarget = jvmTargetVersion
    }
}