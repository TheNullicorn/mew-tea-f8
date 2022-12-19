package me.nullicorn.mewteaf8.gradle.github

import org.gradle.api.Project

/**
 * Configuration for the GitHub repository that the project's code is hosted on.
 */
class MewGitHubConfig(internal val project: Project) {

    /**
     * The name of the "tree" that the current code is from or is going to be pushed to.
     *
     * Configured via the Gradle property "`mew-tea-f8.github.tree`". If not configured, "`HEAD`" is used by default.
     *
     * "Tree" refers to the portion of GitHub URLs used to link to a specific point in a repository's history. In the
     * example URL below, it's where "`<tree>`" is:
     * ```text
     * https://github.com/<user>/<repo>/tree/<tree>
     * ```
     * This can be the name of a branch or tag, a commit's hash, "`HEAD`", or anything else that GitHub accepts there.
     *
     * This is used when generating documentation to create links to the source code of documented members (classes,
     * properties, methods, etc). For documentation being generated for a release, it's recommended that this is the
     * SHA1 hash of the commit being released, that way the source code URLs always point to the exact version of the
     * code they were generated for, rather than the latest version where the linked lines may not match up.
     */
    val tree: String = project.properties["mew-tea-f8.github.tree"] as? String ?: "HEAD"

    /**
     * The GitHub username of the user who owns the hosted repository.
     *
     * Configured via the Gradle property "`mew-tea-f8.github.owner`".
     *
     * This is used when generating documentation to create links to a documented member's source code, and when
     * publishing to fill in metadata about where the project's code can be found online in its `pom.xml` file for
     * Maven.
     */
    val owner: String? = project.properties["mew-tea-f8.github.owner"] as? String

    /**
     * The name of the repository where the project's code is hosted on GitHub.
     *
     * Configured via the Gradle property "`mew-tea-f8.github.repository`".
     *
     * This is used when generating documentation to create links to a documented member's source code, and when
     * publishing to fill in metadata about where the project's code can be found online in its `pom.xml` file for
     * Maven.
     */
    val repository: String? = project.properties["mew-tea-f8.github.repository"] as? String

    /**
     * A GitHub access token with read-access to the metadata & collaborators of the project's repository.
     *
     * Configured using the Gradle property "`mew-tea-f8.github.token`"; do so via environment variable or in
     * `$GRADLE_HOME/gradle.properties`" to prevent accidentally committing it through the project's
     * "`gradle.properties`".
     */
    val accessToken: String? = project.properties["mew-tea-f8.github.token"] as? String

    // Allows this to be configured in brackets `{ }` like most things in Gradle.
    operator fun invoke(action: MewGitHubConfig.() -> Unit): MewGitHubConfig {
        action(this)
        return this
    }
}