package me.nullicorn.mewteaf8.gradle.source

import me.nullicorn.mewteaf8.gradle.compilation.MewCompilationConfig
import me.nullicorn.mewteaf8.gradle.idea
import me.nullicorn.mewteaf8.gradle.kotlin
import org.gradle.api.Project
import org.jetbrains.gradle.ext.packagePrefix
import org.jetbrains.gradle.ext.settings
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

/**
 * Configuration for the project's source-code, such as its structure, classpath and IDE-specific settings.
 */
class MewSourceConfig(private val project: Project, private val compilation: MewCompilationConfig) {

    val packagePrefix: String =
        project.properties["mew-tea-f8.source.package_prefix"] as? String
            ?: ""

    init {
        // For each Kotlin source-set that gets added...
        project.kotlin.sourceSets.whenObjectAdded {
            // For each of that source-set's root directories...
            for (sourceDir in kotlin.srcDirs) {
                // Get the path to that directory relative to the greater project's.
                val relativePath = sourceDir.toRelativeString(base = project.projectDir)
                // Assign our package-prefix to the source directory at that path.
                project.idea.module.settings.packagePrefix[relativePath] = this@MewSourceConfig.packagePrefix
            }
        }
    }

    // Allows this to be configured in brackets `{ }` like most things in Gradle.
    operator fun invoke(action: MewSourceConfig.() -> Unit): MewSourceConfig {
        action(this)
        return this
    }

    /**
     * Creates two new source-sets, "`nonJvmMain`" and "`nativeMain`", and reconfigures the source-set hierarchy so that
     * all source-sets for "native" targets descend from "`nativeMain`", "`nativeMain`" and any other source-sets for
     * non-JVM targets descend from "`nonJvmMain`", and "`nonJvmMain`" descends from "`commonMain`".
     *
     * The same hierarchy is also created with those source-sets' corresponding "`Test`" source-sets, but with
     * "`commonTest`", "`nonJvmTest`" and "`nativeTest`" instead.
     *
     * For example, the following source-set tree would go from...
     * ```text
     * - commonMain
     *    - jvmMain
     *    - jsMain
     *    - mingwX86Main
     *    - macosArm64Main
     *    - linuxX64Main
     * - commonTest
     *    - jvmTest
     *    - jsTest
     *    - mingwX86Test
     *    - macosArm64Test
     *    - linuxX64Test
     * ```
     *
     * to...
     * ```text
     * - commonMain
     *    - jvmMain
     *    - nonJvmMain
     *       - jsMain
     *       - nativeMain
     *          - mingwX86Main
     *          - macosArm64Main
     *          - linuxX64Main
     * - commonTest
     *  - jvmTest
     *  - nonJvmTest
     *     - jsTest
     *     - nativeTest
     *        - mingwX86Test
     *        - macosArm64Test
     *        - linuxX64Test
     * ```
     */
    fun registerNonJvmSourceSet() {
        project.kotlin.run {
            val commonMain = sourceSets.getByName("commonMain")
            val commonTest = sourceSets.getByName("commonTest")

            val nonJvmMain = sourceSets.create("nonJvmMain") { dependsOn(commonMain) }
            val nonJvmTest = sourceSets.create("nonJvmTest") { dependsOn(commonTest) }

            val nativeMain = sourceSets.create("nativeMain") { dependsOn(nonJvmMain) }
            val nativeTest = sourceSets.create("nativeTest") { dependsOn(nonJvmTest) }

            sourceSets.all sourceSet@{
                // Skip the source-sets above because otherwise it'd create a circular dependency situation.
                if (this in setOf(commonMain, commonTest, nonJvmMain, nonJvmTest, nativeMain, nativeTest))
                    return@sourceSet

                // Determine which platform the source-set belongs to, if any. If none, skip the source-set.
                val target = targets.firstOrNull {
                    this@sourceSet in it.compilations.flatMap { it.allKotlinSourceSets }
                } ?: return@sourceSet

                // Check if the source-set is for tests.
                val isTest = this@sourceSet.name.endsWith("test", ignoreCase = true)

                // Make all native source-sets descend from "nativeMain"/"nativeTest", which itself descends from
                // "nonJvmMain"/"nonJvmTest".
                if (target is KotlinNativeTarget)
                    dependsOn(if (isTest) nativeTest else nativeMain)

                // Make all other non-JVM source sets descend from "nonJvmMain"/"nonJvmTest".
                else if (target !is KotlinJvmTarget && target !is KotlinAndroidTarget)
                    dependsOn(if (isTest) nonJvmTest else nonJvmMain)
            }
        }
    }
}