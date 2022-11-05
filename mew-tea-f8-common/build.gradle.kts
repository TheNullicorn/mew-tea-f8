import me.nullicorn.mewteaf8.gradle.*
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

    // Add a separate compilation for our benchmarks.
    jvm {
        compilations {
            val main by getting
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
        val commonTest by getting {
            dependencies {
                implementation(project(":mew-tea-f8-test-helpers"))
            }
        }

        val jvmMain by getting
        val jvmBenchmark by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation(libs.kotlin.benchmark)
            }
        }
    }
}

benchmark {
    targets {
        register("jvmBenchmark")
    }
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}