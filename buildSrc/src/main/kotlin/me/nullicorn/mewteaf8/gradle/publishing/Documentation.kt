package me.nullicorn.mewteaf8.gradle.publishing

import me.nullicorn.mewteaf8.gradle.MewTeaF8BuildProperties
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.GradleDokkaSourceSetBuilder
import java.io.File
import java.net.URI

fun Project.configureDocumentationForMewTeaF8(excludeSourceSetIf: (GradleDokkaSourceSetBuilder) -> Boolean = { false }) {
    // Register the dokka plugin if it isn't already.
    if (plugins.none { it is DokkaPlugin })
        apply(plugin = "org.jetbrains.dokka")

    tasks.register("dokkaJar", Jar::class.java) {
        val htmlTask = tasks.getByName("dokkaHtml")

        dependsOn(htmlTask)
        group = "documentation"
        description = "Generates a jar file with the result of dokkaHtml, especially to be published to Maven Central"

        // Copy the generated HTML into a "javadoc" jar file.
        from(htmlTask)
        archiveClassifier.set("javadoc")
    }

    tasks.withType<AbstractDokkaLeafTask>().configureEach {
        suppressInheritedMembers.set(false)
        suppressObviousFunctions.set(true)

        dokkaSourceSets.configureEach configureSourceSet@{
            if (excludeSourceSetIf(this)) {
                suppress.set(true)
                return@configureSourceSet
            }

            // Register our code samples, package-level documentation, and module-level documentation.
            includeSupplementaryFiles()

            // If this source set has any platform-specific code, include links to its source code on GitHub.
            includeLinksToSourcesOnGitHub(project = this@configureDocumentationForMewTeaF8)

            // Capitalize & switcheroo some of Kotlin's built-in names for targets, like changing "common" to
            // "All Platforms", and "jvm" to "Jvm".
            humanizeSourceSetNames()

            skipDeprecated.set(true)
            skipEmptyPackages.set(true)
            reportUndocumented.set(true)
        }
    }
}

/**
 * Includes [sample source-code files][GradleDokkaSourceSetBuilder.samples] and
 * [package-level/module-level documentation files][GradleDokkaSourceSetBuilder.includes] in the current Dokka
 * souce-set.
 *
 * Sample files are ".sample.kt" files containing code that can be referenced via the "@sample" KDoc tag. They should
 * contain at least 1 top-level container type (class, interface, object, etc) with at least 1 function each. The code
 * inside those functions is what will appear in the documented member's "Samples" section.
 */
private fun GradleDokkaSourceSetBuilder.includeSupplementaryFiles() {
    // Register our code samples, package-level documentation, and module-level documentation.
    for (sourceDir in sourceRoots)
        for (sourceFile in sourceDir.walkTopDown()) {
            // Include markdown files as package-level or module-level documentation.
            if (sourceFile.extension.toLowerCase() in setOf("md", "markdown")) {
                includes.from(sourceFile)
            }

            // Include code samples from files ending in `.sample.kts`.
            if (sourceFile.name.endsWith(".sample.kt", ignoreCase = true))
                samples.from(sourceFile)
        }

    suppressedFiles.from(includes)
    suppressedFiles.from(samples)
}

/**
 * Configures Dokka to link to the URL & starting line number of each documented member on the library's GitHub
 * repository.
 *
 * This makes it easier for curious users to examine the underlying implementation, and for users in
 * performance-critical scenarios to judge the performance impact of their calls to the library.
 *
 * The tree linked to on GitHub is determined using [MewTeaF8BuildProperties.getGitTreeNameOf] with the supplied
 * [project].
 */
private fun GradleDokkaSourceSetBuilder.includeLinksToSourcesOnGitHub(project: Project) {
    // If this source set has any platform-specific code, include links to its source code on GitHub.
    sourceRoots.firstOrNull()?.let { sourceRoot ->
        sourceLink {
            val tree = MewTeaF8BuildProperties.getGitTreeNameOf(project)
            var sourceRootPath = sourceRoot.toRelativeString(base = project.rootDir)

            // Normalize paths to use foward slashes like a regular URL. Namely for Windows, which uses
            // backslashes (\) instead.
            if (File.separatorChar != '/')
                sourceRootPath = sourceRootPath.replace(File.separatorChar, '/')

            remoteUrl.set(URI.create("https://github.com/TheNullicorn/mew-tea-f8/tree/$tree/$sourceRootPath").toURL())
            remoteLineSuffix.set("#L")
            localDirectory.set(sourceRoot)
        }
    }
}

/**
 * Replaces the names of source-sets with more human-friendly equivalents in the generated UI.
 *
 * For example, "common" becomes "All Platforms", and "jvm" becomes "Java / Kotlin JVM". Any names that aren't specially
 * handled are simply converted from camelCase to Title Case with spaces between words.
 */
private fun GradleDokkaSourceSetBuilder.humanizeSourceSetNames() {
    val originalName: String = displayName.getOrNull() ?: name

    // For "common" source-sets specifically, replace it with something friendlier to all users.
    if (originalName.equals("common", ignoreCase = true))
        displayName.set("All Platforms")

    else if (originalName.equals("jvm", ignoreCase = true))
        displayName.set("Java / Kotlin JVM")

    else if (originalName.equals("nonJvm", ignoreCase = true))
        displayName.set("Kotlin JS / Kotlin Native")

    // Prettify the source-set's name; convert camcelCase to regular case + add spaces.
    else displayName.set(buildString {
        append((displayName.getOrNull() ?: name).trim())

        for (i in indices.reversed()) {
            val char = this[i]

            // If it's an uppercase letter right after a character that isn't one, put a space before it.
            if (i > 0 && char in 'A'..'Z' && this[i - 1] !in 'A'..'Z')
                this.insert(i, ' ' /* (U+0020) */)

            // If the first character is a lowercase letter, make it uppercase.
            else if (i == 0 && char in 'a'..'z')
                this[i] = Character.toUpperCase(char)
        }
    })
}