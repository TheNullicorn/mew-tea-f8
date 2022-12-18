package me.nullicorn.mewteaf8.gradle.documentation

import me.nullicorn.mewteaf8.gradle.github.MewGitHubConfig
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.GradleDokkaSourceSetBuilder
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

                    // TODO: 12/11/2022 Re-add support for package-level and module-level documentation via ".md" files.
                    // TODO: 12/11/2022 Re-add GitHub source-links using the project's github configuration.
                }
            }
        }
    }
}