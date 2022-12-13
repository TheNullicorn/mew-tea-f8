package me.nullicorn.mewteaf8.gradle

import me.nullicorn.mewteaf8.gradle.compilation.MewCompilationConfig
import me.nullicorn.mewteaf8.gradle.documentation.MewDocumentationConfig
import me.nullicorn.mewteaf8.gradle.github.MewGitHubConfig
import me.nullicorn.mewteaf8.gradle.publishing.MewPublishingConfig
import me.nullicorn.mewteaf8.gradle.source.MewSourceConfig
import org.gradle.api.Project

class MewProjectExtension(private val project: Project) {

    val github = MewGitHubConfig(project)

    val compilation = MewCompilationConfig(project)

    val source = MewSourceConfig(project, compilation)

    val publishing = MewPublishingConfig(project)

    val documentation = MewDocumentationConfig(project, github)

    // Allows this to be configured in brackets `{ }` like most things in Gradle.
    operator fun invoke(action: MewProjectExtension.() -> Unit): MewProjectExtension {
        action(this)
        return this
    }
}