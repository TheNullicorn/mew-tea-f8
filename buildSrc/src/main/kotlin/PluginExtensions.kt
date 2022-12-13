package me.nullicorn.mewteaf8.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.gradle.ext.IdeaExtPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin

val Project.`mew-tea-f8`: MewProjectExtension
    get() = extensions.findByType<MewProjectExtension>()
        ?: MewProjectExtension(project = this).also {
            extensions.add(MewProjectExtension::class.java, "mew-tea-f8", it)
        }

val Project.kotlin: KotlinMultiplatformExtension
    get() {
        plugins.applyKotlinPluginIfMissing()
        return extensions.getByType<KotlinMultiplatformExtension>()
    }

internal val Project.idea: IdeaModel
    get() {
        plugins.applyIdeaPluginIfMissing()
        plugins.applyIdeaExtensionsPluginIfMissing()
        return extensions.getByType<IdeaModel>()
    }

internal val Project.signing: SigningExtension
    get() {
        plugins.applySigningPluginIfMissing()
        return extensions.getByType<SigningExtension>()
    }

internal val Project.publishing: PublishingExtension
    get() {
        plugins.applyPublishingPluginIfMissing()
        return extensions.getByType<PublishingExtension>()
    }

internal fun PluginContainer.applyKotlinPluginIfMissing() =
    applyPluginIfMissing<KotlinMultiplatformPlugin>(id = "org.jetbrains.kotlin.multiplatform")

internal fun PluginContainer.applyDokkaPluginIfMissing() =
    applyPluginIfMissing<DokkaPlugin>(id = "org.jetbrains.dokka")

internal fun PluginContainer.applyIdeaPluginIfMissing() =
    applyPluginIfMissing<IdeaPlugin>(id = "idea")

internal fun PluginContainer.applyIdeaExtensionsPluginIfMissing() =
    applyPluginIfMissing<IdeaExtPlugin>(id = "org.jetbrains.gradle.plugin.idea-ext")

internal fun PluginContainer.applySigningPluginIfMissing() =
    applyPluginIfMissing<SigningPlugin>(id = "signing")

internal fun PluginContainer.applyPublishingPluginIfMissing() =
    applyPluginIfMissing<PublishingPlugin>(id = "maven-publish")

private inline fun <reified P : Plugin<Project>> PluginContainer.applyPluginIfMissing(id: String) {
    if (none { it is P })
        apply(id)
}