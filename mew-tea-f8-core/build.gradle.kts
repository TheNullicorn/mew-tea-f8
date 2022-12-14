import me.nullicorn.mewteaf8.gradle.*

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    id(libs.plugins.kotlin.documentation.get().pluginId)
    id(libs.plugins.binary.compatibility.get().pluginId)
    id(libs.plugins.intellij.settings.get().pluginId)
    id("maven-publish")
    id("signing")

    // These 2 plugins are needed specifically for benchmarks; see further down.
    alias(libs.plugins.kotlin.benchmark)
    alias(libs.plugins.kotlin.allopen)
}

repositories {
    mavenCentral()
}

@Suppress("OPT_IN_USAGE")
`mew-tea-f8` {
    compilation {
        registerTargets(/* excluded = */ kotlin::wasm)
    }

    source {
        registerNonJvmSourceSet()
    }
}

@Suppress("UNUSED_VARIABLE")
kotlin.sourceSets {
    val commonTest by getting {
        dependencies {
            implementation(libs.bundles.kotlin.test)
            implementation(project(":mew-tea-f8-test-helpers"))
        }
    }
}

// Register our benchmarks (ONLY IF WE ALREADY HAVE A JVM TARGET/SOURCE-SET/COMPILATION).
if (`mew-tea-f8`.compilation.buildMode.includesJvm)
    @Suppress("UNUSED_VARIABLE") kotlin {
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