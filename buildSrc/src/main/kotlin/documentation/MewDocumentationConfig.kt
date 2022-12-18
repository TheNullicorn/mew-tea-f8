package me.nullicorn.mewteaf8.gradle.documentation

import me.nullicorn.mewteaf8.gradle.github.MewGitHubConfig
import me.nullicorn.mewteaf8.gradle.kotlin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.GradleDokkaSourceSetBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File
import java.net.URI
import kotlin.text.RegexOption.IGNORE_CASE

private val PLATFORM_NAME_KEY_PATTERN = "^mew-tea-f8\\.documentation\\.platform\\.(.*)\\.name$".toRegex(IGNORE_CASE)

/**
 * Configuration for the project's documentation.
 */
class MewDocumentationConfig(private val project: Project, private val github: MewGitHubConfig) {

    /**
     * Whether documentation should be enabled at all for the project, including the plugins & tasks it would otherwise
     * bring with it.
     *
     * This is intended for internal modules that will never be published, like those used solely for unit testing. In
     * those cases, this should be `false`.
     *
     * For published modules this should always be `true`.
     */
    var enabled: Property<Boolean> = project.objects.property(Boolean::class.java).convention(true)

    /**
     * Replacement names for documented source-sets.
     *
     * Configured via Gradle properties named "`mew-tea-f8.documentation.platform.<default>.name`" **where `<default`>
     * is replaced with the default name given to the source-set by Dokka**, like "`jvm`" or "`macosX64`".
     *
     * This is intended for giving "pretty" names to platforms in the generated documentation because by default, Dokka
     * just chops the "`Main`" off the end of a source-set's name to get the platform's (e.g. "`jvmMain`" becomes just
     * "`jvm`").
     *
     * The keys in this map are the default names given to a source-set by Dokka like "`mingwX86`", which you could
     * replace with something like "`Windows (x86)`" or "`Windows 32-bit`".
     */
    val platformNameReplacements: Map<String, String> = HashMap<String, String>().apply {
        for ((key, newName) in project.properties)
            if (newName is String) {
                val oldName = PLATFORM_NAME_KEY_PATTERN
                    .findAll(input = key)
                    .filterIndexed { i, _ -> i == 1 }
                    .firstOrNull()?.value
                    ?: continue

                put(oldName, newName)
            }
    }

    /**
     * Predicates that, if matched by a [Dokka source-set][GradleDokkaSourceSetBuilder], should be excluded from
     * documentation.
     *
     * This is intended for source-sets that are not published, such as source-sets that only contain benchmarks. "Test"
     * source-sets are excluded by Dokka automatically and don't need to be included here.
     */
    private val excludedSourceSetRules = HashSet<(GradleDokkaSourceSetBuilder) -> Boolean>()

    /**
     * Tells Dokka to exclude any source-sets that match a [predicate] from being documented.
     *
     * Calling this multiple times will not overwrite previous predicates. Instead, source-sets that match *any* of the
     * registered predicates will be excluded.
     *
     * This is intended for source-sets that are not published, such as source-sets that only contain benchmarks. "Test"
     * source-sets are excluded by Dokka automatically and don't need to be included here.
     *
     * @param[predicate] A condition under which source-sets should be excluded from documentation.
     */
    fun excludeSourceSetsIf(predicate: (GradleDokkaSourceSetBuilder) -> Boolean) {
        excludedSourceSetRules += predicate
    }

    // Allows this to be configured in brackets `{ }` like most things in Gradle.
    operator fun invoke(action: MewDocumentationConfig.() -> Unit): MewDocumentationConfig {
        action(this)
        return this
    }

    init {
        project.afterEvaluate {
            // Don't configure Dokka if documentation is disabled.
            if (!this@MewDocumentationConfig.enabled.get())
                return@afterEvaluate

            tasks.withType<DokkaTask>().configureEach taskConfig@{
                dokkaSourceSets.configureEach sourceSetConfig@{
                    // Exclude source-sets that match any of the configured exclusion rules.
                    if (excludedSourceSetRules.any { it(this) }) {
                        suppress.set(true)
                        return@sourceSetConfig
                    }

                    val currentName: String = displayName.orNull ?: name
                    val newName: String = platformNameReplacements.entries
                        .firstOrNull { it.key.equals(currentName, ignoreCase = true) }
                        ?.value
                        ?: currentName

                    displayName.set(newName)

                    configureGitHubLinks(github, projectRootDir = project.rootDir)

                    project.kotlin.sourceSets
                        .firstOrNull { it.kotlin.sourceDirectories.any { it in sourceRoots } }
                        ?.let { configureSupplementaryFiles(correspondingKotlinSourceSet = it) }
                }
            }
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
private fun GradleDokkaSourceSetBuilder.configureSupplementaryFiles(correspondingKotlinSourceSet: KotlinSourceSet) {
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
                    markdownDocs.putIfAbsent(/* key = */ sourceFile.toRelativeString(base = sourceDir), /* value = */
                        sourceFile
                    )

                // Include code samples from files ending in `.sample.kts`. Unlike markdown files, these actually are
                // being registered right now, not at the end.
                if (sourceFile.name.endsWith(".sample.kt", ignoreCase = true))
                    samples.from(sourceFile)
            }

        for (parentSourceSet in sourceSet.dependsOn)
            includeFromSourceSet(parentSourceSet)
    }

    // Register all the markdown files & sample code from the source-set & all its parents.
    includeFromSourceSet(correspondingKotlinSourceSet)

    // Actually register the markdown files; the samples are already registered.
    includes.from.addAll(markdownDocs.values)

    // Exclude any sample files & markdown files from being documented as part of the code itself, rather than being
    // used as supplements, since they appear alongside the library code itself.
    suppressedFiles.from(includes)
    suppressedFiles.from(samples)

    // Exclude all supplementary files from the actual Kotlin source-set since they're not intended to be in the final
    // compilations.
    correspondingKotlinSourceSet.kotlin.exclude { it.file in suppressedFiles }
}

/**
 * Configures Dokka to link to the URL & starting line number of each documented member on the library's GitHub
 * repository.
 *
 * This makes it easier for curious users to examine the underlying implementation, and for users in
 * performance-critical scenarios to judge the performance impact of their calls to the library.
 */
private fun GradleDokkaSourceSetBuilder.configureGitHubLinks(github: MewGitHubConfig, projectRootDir: File) {
    if (github.owner == null || github.repository == null) return

    // Get the directory, if any, of the source-set being documented.
    val sourceDir = sourceRoots.firstOrNull() ?: return

    // Create the URL to the repository @ the configured tree & sourceDir.
    val githubUrlBase = StringBuilder("https://github.com/")
        // Add the repository's owner & name to the URL.
        .append(github.owner).append('/').append(github.repository)
        // Add the working tree/ref to the URL.
        .append("/tree/").append(github.tree).append('/')
        // Add the sourceDir's path, relative to the project's root, using forward slashes.
        .append(
            sourceDir
                .toRelativeString(base = projectRootDir)
                .replace(File.separatorChar, '/')
        )
        .toString()

    sourceLink {
        remoteUrl.set(URI.create(githubUrlBase).toURL())
        localDirectory.set(sourceDir)
        remoteLineSuffix.set("#L")
    }
}