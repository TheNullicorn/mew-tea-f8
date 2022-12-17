package me.nullicorn.mewteaf8.gradle.publishing

import me.nullicorn.mewteaf8.gradle.*
import me.nullicorn.mewteaf8.gradle.github.GitHubMewPomConfigurator
import me.nullicorn.mewteaf8.gradle.github.MewGitHubConfig
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.Sign
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.nio.file.Paths

/**
 * Configuration for the project's publications to Maven repositories, especially Maven Central (via Sonatype OSSRH).
 */
class MewPublishingConfig(private val project: Project) {

    /**
     * Whether Maven publishing should be enabled for the project, including the required plugins & tasks.
     *
     * Configured via the Gradle property named "`mew-tea-f8.publishing.enabled`", though it can be overridden by a
     * project from its buildscript. By default it's `false`.
     */
    var enabled: Boolean = project.properties["mew-tea-f8.publishing.enabled"]?.toString()?.toBoolean() ?: false

    /**
     * An instance of [MewPomConfigurator] that populates the project's [POM][MavenPom] metadata prior to publishing.
     *
     * To use GitHub as a source of metadata for the project, use [fillPomUsingGitHub], or set this to a configured
     * instance of [GitHubMewPomConfigurator].
     */
    val mavenPom: Property<MewPomConfigurator> = project.objects.property<MewPomConfigurator>()

    /**
     * The root URL of the repository that snapshots should be published to.
     *
     * Configured using the Gradle property "`mew-tea-f8.publishing.repository.snapshot.url`".
     *
     * This will be used if the project's version ends in "`-SNAPSHOT`", or if it doesn't but the [stagingRepositoryUrl]
     * is `null` and this one isn't.
     */
    val snapshotRepositoryUrl: String? = project.getRepositoryUrl("snapshot")

    /**
     * The root URL of the repository that snapshots should be published to.
     *
     * Configured using the Gradle property "`mew-tea-f8.publishing.repository.staging.url`".
     *
     * This will be used when the project's version **does not end in** "`-SNAPSHOT`", or if it does but the
     * [snapshotRepositoryUrl] is `null` and this one isn't.
     */
    val stagingRepositoryUrl: String? = project.getRepositoryUrl("staging")

    /**
     * The username to use when authenticating with the configured repository whilst publishing.
     *
     * Configured using the Gradle property "`mew-tea-f8.publishing.username`"; do so via environment variable or in
     * "`$GRADLE_HOME/gradle.properties`" to prevent accidentally committing it through the project's
     * "`gradle.properties`".
     *
     * If the repository requires authentication to publish, then this must be configured & valid or else
     * publications will fail.
     */
    val repositoryUsername: String? = project.properties["mew-tea-f8.publishing.username"] as? String

    /**
     * The password corresponding to the [repositoryUsername], if any.
     *
     * Configured using the Gradle property "`mew-tea-f8.publishing.password`"; do so via environment variable or in
     * "`$GRADLE_HOME/gradle.properties`" to prevent accidentally committing it through the project's
     * "`gradle.properties`".
     *
     * If the repository requires authentication to publish, then this must be configured & valid or else
     * publications will fail.
     */
    val repositoryPassword: String? = project.properties["mew-tea-f8.publishing.password"] as? String

    /**
     * Fills in the POM of publications using metadata from a [configured][MewGitHubConfig] GitHub repository.
     *
     * This sets the [mavenPom] property to a [GitHubMewPomConfigurator] configured using the project's [GitHub
     * configuration][MewProjectExtension.github].
     */
    fun fillPomUsingGitHub() {
        mavenPom.set(GitHubMewPomConfigurator(environment = project.`mew-tea-f8`.github))
    }

    // Allows this to be configured in brackets `{ }` like most things in Gradle.
    operator fun invoke(action: MewPublishingConfig.() -> Unit): MewPublishingConfig {
        action(this)
        return this
    }

    init {
        // Run our set-up after the buildscript so that it has a chance to manually configure `enabled` if it wants to.
        project.afterEvaluate {
            if (!enabled)
                return@afterEvaluate

            if (!mavenPom.isPresent)
                throw InvalidUserDataException("mavenPom must be configured when publishing is enabled")

            // Determine the URL of the repository we'll be publishing to.
            val repositoryUrl: String = when {
                // If both URLs are non-null, use the appropriate one based on whether the version of the project itself
                // is a snapshot.
                snapshotRepositoryUrl != null && stagingRepositoryUrl != null ->
                    if (project.version.toString().endsWith("-SNAPSHOT")) snapshotRepositoryUrl
                    else stagingRepositoryUrl

                // If one of them is null, then use the non-null one, regardless of the project's version.
                snapshotRepositoryUrl != null -> snapshotRepositoryUrl
                stagingRepositoryUrl != null -> stagingRepositoryUrl

                // If no URL is configured, exit unseccessfully.
                else -> throw InvalidUserDataException("Publishing is enabled but no Maven repository's URL was set-up")
            }

            // Add the Dokka plugin (if it wasn't already) so that our "dokkaHtmlJar" task can use its "dokkaHtml" task.
            project.plugins.applyDokkaPluginIfMissing()

            // Create a task that bundles dokka's HTML output into a jar file.
            val kdocJarTask = project.tasks.register<Jar>("dokkaHtmlJar") {
                // Get the task that generates the actual KDoc pages.
                val kdocTask = project.tasks.named<DokkaTask>("dokkaHtml")

                // Ensure this task doesn't run until that one is.
                dependsOn(kdocTask)

                // Bundle the output of that task (mostly HTML) into our new jar.
                from(kdocTask)

                // Distinguish this documentation-only jar from those that hold the library's compiled code.
                archiveClassifier.set("javadoc")
            }

            // Configure the publications themselves (run after the project's source-sets & such are all set-up).
            project.publishing.run {
                repositories {
                    maven {
                        name = "ossrh"
                        url = URI(repositoryUrl)
                        credentials {
                            username = repositoryUsername ?: ""
                            password = repositoryPassword ?: ""
                        }
                    }
                }

                publications.withType<MavenPublication>().configureEach {
                    // Include the documentation jar, since Maven Central requires one be attached.
                    artifact(kdocJarTask)

                    // Fill in additional metadata for the pom.xml.
                    mavenPom.get().invoke(pom)
                }

                "signing.secretKeyRingFile".let { keyRingPropertyName ->
                    var keyRingPath = project.properties[keyRingPropertyName] as? String ?: return@let

                    // If the path starts with a tilde '~', treat it as unix would and replace the tilde with the user's
                    // home directory, even if we're not on a unix system.
                    if (keyRingPath.startsWith("~")) {
                        val userBasedKeyRingPath =
                            Paths.get(System.getProperty("user.home"), keyRingPath.substring(startIndex = 1))
                                .toAbsolutePath()
                        project.setProperty(keyRingPropertyName, userBasedKeyRingPath.toString())
                    }
                }

                // Sign all of our artifacts; required for Maven Central to accept them.
                project.signing.sign(publications)

                // Publishing tasks aren't automatically dependent on the signing tasks for some reason, so we have to
                // link them up manually. It still works without this, but it left a bunch of warnings while publishing.
                // See here: https://youtrack.jetbrains.com/issue/KT-46466
                val signingTasks = tasks.withType<Sign>()
                tasks.withType<AbstractPublishToMaven>().configureEach {
                    dependsOn(signingTasks)
                }
            }
        }
    }

    private fun Project.getRepositoryUrl(repository: String): String? {
        val urlString: String? = project.properties["mew-tea-f8.publishing.repository.$repository.url"] as? String
            ?: return null

        try {
            URL(urlString)
        } catch (cause: MalformedURLException) {
            throw InvalidUserDataException("Repository URL is malformed: $urlString\"")
        }

        return urlString
    }
}