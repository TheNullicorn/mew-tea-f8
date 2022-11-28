package me.nullicorn.mewteaf8.gradle

import me.nullicorn.mewteaf8.gradle.targets.MewTeaF8BuildTargets
import org.gradle.api.Project
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object MewTeaF8BuildProperties {

    private const val QUALIFIER = "mew-tea-f8."
    private const val ENV_QUALIFIER = "MEW_"

    // Gradle Properties              @formatter:off
    private const val BUILD_TARGET  = "${QUALIFIER}target"
    private const val ROOT_PACKAGE  = "${QUALIFIER}root_package"
    private const val GIT_TREE_NAME = "${QUALIFIER}git_tree_name"
    // @formatter:on

    // Environment Variables             @formatter:off
    private const val ENV_BUILD_TARGET  = "${ENV_QUALIFIER}TARGET"
    private const val ENV_GIT_TREE_NAME = "${ENV_QUALIFIER}GIT_TREE_NAME"
    // @formatter:on

    /**
     * A stringified list of the `configName` for each target in our enum.
     *
     * Only intended for error messages in [mewTeaF8BuildTargets].
     */
    private val allowedBuildTargetsForLogMessage: String by lazy {
        MewTeaF8BuildTargets.values().joinToString { "\"${it.configName}\"" }
    }

    /**
     * Retrieves the configured [MewTeaF8BuildTargets] for a Gradle [project].
     *
     * The configured targets are searched for in the following places, in order:
     * 1. If the environment variable "`MEW_TARGET`" is set, that option is used
     * 2. If the Gradle property "`mew-tea-f8.target`" is set, that option is used
     * 3. Otherwise, [MewTeaF8BuildTargets.COMMON_AND_NATIVE] is used by default
     *
     * The allowed options are:
     * - "`all`": [MewTeaF8BuildTargets.COMMON_AND_NATIVE]
     * - "`common`": [MewTeaF8BuildTargets.COMMON_ONLY]
     * - "`native`": [MewTeaF8BuildTargets.NATIVE_ONLY]
     *
     * If any other value is found, an [IllegalArgumentException] is thrown to cancel the build.
     */
    fun getBuildTargetsOf(project: Project): MewTeaF8BuildTargets {
        val name = System.getenv(ENV_BUILD_TARGET) ?: project.properties[BUILD_TARGET] as? String

        if (name == null) {
            project.logger.warn(
                """
                Build environment not configured. Defaulting to "${MewTeaF8BuildTargets.COMMON_AND_NATIVE.configName}"...
                Set the "$BUILD_TARGET" Gradle property or "$ENV_BUILD_TARGET" environment variable to one of: $allowedBuildTargetsForLogMessage
                """.trimIndent()
            )
            return MewTeaF8BuildTargets.COMMON_AND_NATIVE
        }

        for (target in MewTeaF8BuildTargets.values())
            if (name.equals(target.configName, ignoreCase = true))
                return target

        throw IllegalArgumentException("\"$name\" is not a valid enviornment. Try one of these: $allowedBuildTargetsForLogMessage")
    }

    /**
     * Retrieves the configured root package name (or "package prefix", as its referred to by the `idea-ext` plugin) of
     * a Gradle [project].
     *
     * This is retrieved from the "`mew-tea-f8.root_package`" Gradle property. If it's not configured, or if it isn't a
     * string, then an empty string is returned to indicate that no root package name should be applied.
     */
    fun getRootPackageOf(project: Project): String =
        project.properties[ROOT_PACKAGE] as? String ?: ""

    /**
     * Retrieves the configured git tree name that the local files of a [project] are from.
     *
     * The configured tree name is searched for in the following places, in order:
     * 1. If the environment variable "`MEW_GIT_TREE_NAME`" is set (and not empty), that option is used
     * 2. If the Gradle property "`mew-tea-f8.git_tree_name`" is set (and is a non-empty string), that option is used
     * 3. Otherwise, "`HEAD`" is used so that source code links point to the most recently dated commit hosted on the
     * primary branch.
     *
     * This is used by Dokka to generate hyperlinks to the source code itself on GitHub. Therefore, it should be any
     * value that GitHub is able to interpret as the `<tree>` in URLs such as these:
     * "`https://github.com/user/repo/tree/<tree>`". For example, the names of branches and tags, commit hashes, and
     * "`HEAD`" are all valid.
     */
    fun getGitTreeNameOf(project: Project): String {
        var gitTreeName = System.getenv(ENV_GIT_TREE_NAME)

        if (gitTreeName.isNullOrEmpty())
            gitTreeName = project.properties[GIT_TREE_NAME] as? String

        if (gitTreeName.isNullOrEmpty())
            gitTreeName = "HEAD"

        // Just to be safe, URL-encode the name since it'll be used in the path of a URL.
        return URLEncoder.encode(gitTreeName, "UTF-8")
    }
}