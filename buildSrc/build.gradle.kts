import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    fun plugin(id: String, version: String) = "$id:${id}.gradle.plugin:$version"
    val kotlinVersion = libs.versions.kotlin.asProvider().get()

    implementation(kotlin("gradle-plugin", version = kotlinVersion))
    implementation(plugin("org.jetbrains.dokka", version = kotlinVersion))
    implementation("org.jetbrains.dokka:dokka-base:$kotlinVersion")
}

// Set the JVM target version to 1.8 for both Kotlin and Java because for some reason they're different in buildSrc.

val jvmTargetVersion = JavaVersion.VERSION_1_8.toString()

tasks.withType<KotlinCompile<KotlinJvmOptions>>().configureEach {
    kotlinOptions {
        jvmTarget = jvmTargetVersion
    }
}

kotlin.sourceSets.all {
    languageSettings.optIn("org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl")
}

tasks.withType<JavaCompile>().configureEach {
    targetCompatibility = jvmTargetVersion
}