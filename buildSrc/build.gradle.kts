import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin", version = libs.versions.kotlin.asProvider().get()))
}

// Set the JVM target version to 1.8 for both Kotlin and Java because for some reason they're different in buildSrc.

val jvmTargetVersion = JavaVersion.VERSION_1_8.toString()

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<KotlinJvmOptions>>().configureEach {
    kotlinOptions {
        jvmTarget = jvmTargetVersion
    }
}

tasks.withType<JavaCompile>().configureEach {
    targetCompatibility = jvmTargetVersion
}