import me.nullicorn.mewteaf8.gradle.*
import me.nullicorn.mewteaf8.gradle.publishing.*
import me.nullicorn.mewteaf8.gradle.targets.*

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.benchmark)
    alias(libs.plugins.kotlin.allopen)
}

repositories {
    mavenCentral()
}

@Suppress("UNUSED_VARIABLE", "OPT_IN_USAGE")
kotlin {
    registerTargetsForMewTeaF8(project, /* excludedTargets = */ ::wasm)
    configureSourceSetsForMewTeaF8(project)
    configureDocumentationForMewTeaF8(excludeSourceSetIf = { "[Bb]enchmark".toRegex() in it.name })

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(project(":mew-tea-f8-test-helpers"))
            }
        }
    }

    // Register our benchmarks (ONLY IF WE ALREADY HAVE A JVM TARGET/SOURCE-SET/COMPILATION).
    if (this@kotlin.hasJvmCompilations) {
        // Add a separate compilation for our benchmarks.
        jvm {
            compilations {
                // Get the "main" compilation ("jvmMain"), which already exists.
                val main by getting

                // Create a new compilation for our benchmarks, and add the "main" one's entire classpath to it as well.
                val benchmark by creating {
                    defaultSourceSet {
                        dependencies {
                            implementation(main.compileDependencyFiles + main.output.classesDirs)
                        }
                    }
                }
            }
        }

        sourceSets {
            // Get the existing source-set for the "main" compilation.
            val jvmMain by getting

            // Configure the new source-set for our benchmarks.
            val jvmBenchmark by getting {
                dependsOn(jvmMain)
                dependencies {
                    implementation(libs.kotlin.benchmark)
                }
            }
        }

        // Automatically make all benchmark classes `open`, as required by JMH.
        allOpen {
            annotation("org.openjdk.jmh.annotations.State")
        }

        // Register our benchmark source-set with the "kotlinx-benchmark" Gradle plugin.
        benchmark.targets.register("jvmBenchmark")
    }
}