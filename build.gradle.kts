import kotlinx.validation.ApiValidationExtension
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.packagePrefix
import org.jetbrains.gradle.ext.settings
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.binary.compatibility) apply false
    alias(libs.plugins.intellij.settings) apply false
}

group = "me.nullicorn"
version = "0.0.1-SNAPSHOT"

val rootPackage: String = properties["library.root_package"] as String

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

    // Configures the same multiplatform targets for all subprojects.
    @Suppress("UNUSED_VARIABLE")
    configure<KotlinMultiplatformExtension> {
        jvm {
            compilations.all {
                kotlinOptions.jvmTarget = "1.8"
            }
            withJava()
            testRuns["test"].executionTask.configure {
                useJUnitPlatform()
            }
        }

        js(BOTH) {
            nodejs()
            browser()
        }

        val hostOs = System.getProperty("os.name")
        val isMingwX64 = hostOs.startsWith("Windows")
        val nativeTarget = when {
            hostOs == "Mac OS X" -> macosX64("native")
            hostOs == "Linux" -> linuxX64("native")
            isMingwX64 -> mingwX64("native")
            else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
        }

        sourceSets {
            val commonMain by getting
            val commonTest by getting {
                dependencies {
                    implementation(kotlin("test"))
                }
            }
            val jvmMain by getting
            val jvmTest by getting
            val jsMain by getting
            val jsTest by getting
            val nativeMain by getting
            val nativeTest by getting
        }
    }

    configure<IdeaModel> {
        module {
            settings {
                for (sourceSet in kotlinExtension.sourceSets)
                    for (sourceDir in sourceSet.kotlin.srcDirs) {
                        val relativeSourceDir = sourceDir.toRelativeString(base = projectDir)
                        packagePrefix[relativeSourceDir] = rootPackage
                    }
            }
        }
    }

    configure<ApiValidationExtension> {
        ignoredPackages += "${rootPackage}.internal"
        nonPublicMarkers += "${rootPackage}.internal.InternalMewTeaF8Api"
    }
}
