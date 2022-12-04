package me.nullicorn.mewteaf8.gradle.publishing

import me.nullicorn.mewteaf8.gradle.MewTeaF8BuildProperties
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.GradleDokkaSourceSetBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
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
            includeSupplementaryFiles(project = this@configureDocumentationForMewTeaF8)

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
private fun GradleDokkaSourceSetBuilder.includeSupplementaryFiles(project: Project) {
    // A map of all the markdown files we'll be including, keyed by their paths relative to their source-set's root dir.
    // This way if the source-set has a markdown file at the same path as one of its parent source-sets, the child's
    // will take precedence. Or, if a child source-set is missing a markdown file that only its parent has, it will be
    // included in the child's docs.
    val markdownDocs = HashMap<String, File>()

    // Register our code samples, package-level documentation, and module-level documentation. This is recursive so that
    // each source-set includes the docs of any of its parent source-sets.
    fun includeFromSourceSet(sourceSet: KotlinSourceSet) {
        for (sourceDir in sourceSet.kotlin.srcDirs)
            for (sourceFile in sourceDir.walkTopDown()) {
                // Include markdown files as package-level or module-level documentation. Right now we're just
                // collecting them in a map; they're actually included at the end of the outer function.
                if (sourceFile.extension.toLowerCase() in setOf("md", "markdown"))
                    markdownDocs.putIfAbsent(/* key = */ sourceFile.toRelativeString(base = sourceDir), /* value = */sourceFile)

                // Include code samples from files ending in `.sample.kts`. Unlike markdown files, these actually are
                // being registered right now, not at the end.
                if (sourceFile.name.endsWith(".sample.kt", ignoreCase = true))
                    samples.from(sourceFile)
            }

        for (parentSourceSet in sourceSet.dependsOn)
            includeFromSourceSet(parentSourceSet)
    }

    // Determine the `KotlinSourceSet` that this `DokkaSourceSet` was created for.
    val kotlinSourceSet = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        .sourceSets.firstOrNull { it.kotlin.sourceDirectories.any { it in sourceRoots } }
        ?: return

    // Register all the markdown files & sample code from the source-set & all its parents.
    includeFromSourceSet(kotlinSourceSet)

    // Actually register the markdown files; the samples are already registered.
    includes.from.addAll(markdownDocs.values)

    // Exclude any sample files & markdown files from being documented as part of the code itself, rather than being
    // used as supplements, since they appear alongside the library code itself.
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