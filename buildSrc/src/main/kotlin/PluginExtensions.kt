package me.nullicorn.mewteaf8.gradle

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

val Project.`mew-tea-f8`: MewProjectExtension
    get() = extensions.findByType<MewProjectExtension>()
        ?: MewProjectExtension(project = this).also {
            extensions.add(MewProjectExtension::class.java, "mew-tea-f8", it)
        }

val Project.kotlin: KotlinMultiplatformExtension
    get() = extensions.getByType<KotlinMultiplatformExtension>()

internal val Project.idea: IdeaModel
    get() = extensions.getByType<IdeaModel>()

internal val Project.signing: SigningExtension
    get() = extensions.getByType<SigningExtension>()

internal val Project.publishing: PublishingExtension
    get() = extensions.getByType<PublishingExtension>()