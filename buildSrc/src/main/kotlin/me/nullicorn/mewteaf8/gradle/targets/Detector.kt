package me.nullicorn.mewteaf8.gradle.targets

import me.nullicorn.mewteaf8.gradle.targets.MewTeaF8BuildTargets.*
import org.gradle.api.Project

/**
 * The configured [MewTeaF8BuildTargets] for the current [Project].
 *
 * The configured targets are searched for in the following places, in order:
 * 1. If the environment variable "`MEW_TARGET`" is set, that option is used
 * 2. If the Gradle property "`mew-tea-f8-target`" is set, that option is used
 * 3. Otherwise, [MewTeaF8BuildTargets.COMMON_AND_NATIVE] is used by default
 *
 * The allowed options are:
 * - "`all`": [MewTeaF8BuildTargets.COMMON_AND_NATIVE]
 * - "`common`": [MewTeaF8BuildTargets.COMMON_ONLY]
 * - "`native`": [MewTeaF8BuildTargets.NATIVE_ONLY]
 *
 * If any other value is found, an [IllegalArgumentException] is thrown to cancel the build.
 */
val Project.mewTeaF8BuildTargets: MewTeaF8BuildTargets
    get() {
        val name = System.getenv(ENV_VAR_NAME) ?: project.properties[PROPERTY_NAME] as? String

        if (name == null) {
            logger.warn(
                """
                Build environment not configured. Defaulting to "${COMMON_AND_NATIVE.configName}"...
                Set the "$PROPERTY_NAME" property or "$ENV_VAR_NAME" environment variable to one of: $availableTargetNames
                """.trimIndent()
            )
            return COMMON_AND_NATIVE
        }

        for (target in values())
            if (name.equals(target.configName, ignoreCase = true))
                return target

        throw IllegalArgumentException("\"$name\" is not a valid enviornment. Try one of these: $availableTargetNames")
    }

/**
 * The name of the environment variable that [mewTeaF8BuildTargets] will check first when looking for the configured
 * targets.
 */
private const val ENV_VAR_NAME = "MEW_TARGET"

/**
 * The name of the Grale property that [mewTeaF8BuildTargets] will check if the corresponding
 * [environment variable][ENV_VAR_NAME] is not set when looking for the configured targets.
 */
private const val PROPERTY_NAME = "mew-tea-f8-target"

/**
 * A stringified list of the `configName` for each target in our enum.
 *
 * Only intended for error messages in [mewTeaF8BuildTargets].
 */
private val availableTargetNames: String =
    values().joinToString(
        prefix = "[",
        postfix = "]",
        separator = ", ",
        transform = { "\"${it.configName}\"" })