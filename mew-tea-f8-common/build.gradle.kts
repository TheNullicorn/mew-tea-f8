import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetDsl

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.benchmark)
    alias(libs.plugins.kotlin.allopen)
}

repositories {
    mavenCentral()
}

@Suppress("UNUSED_VARIABLE")
kotlin {
    // Target Java 8.
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
            all {
                kotlinOptions.jvmTarget = "1.8"
            }
        }
    }

    // Target NodeJS and in-browser JavaScript.
    js(BOTH) {
        // Target both client & server-side JavaScript.
        val subTargets: List<(KotlinJsSubTargetDsl.() -> Unit) -> Unit> =
            listOf(::nodejs, ::browser)

        // Register & configure each target listed above.
        for (jsTarget in subTargets) {
            jsTarget {
                testTask {
                    useMocha {
                        // Allow longer tests to run up to 10 seconds before failing.
                        timeout = "0s"
                    }
                }
            }
        }
    }

    // Target Windows, macOS and Linux.
    val hostOs = System.getProperty("os.name")
    when {
        hostOs == "Linux" -> linuxX64("native")
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs.startsWith("Windows") -> mingwX64("native")
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

        val nonJvmMain by creating {
            dependsOn(commonMain)
        }

        val jsMain by getting {
            dependsOn(nonJvmMain)
        }
        val jsTest by getting

        val nativeMain by getting {
            dependsOn(nonJvmMain)
        }
        val nativeTest by getting

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