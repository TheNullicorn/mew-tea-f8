import me.nullicorn.mewteaf8.gradle.*
import me.nullicorn.mewteaf8.gradle.publishing.*
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.packagePrefix
import org.jetbrains.gradle.ext.settings
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id(libs.plugins.kotlin.documentation.get().pluginId)
    alias(libs.plugins.binary.compatibility) apply false
    alias(libs.plugins.intellij.settings) apply false
}

group = "me.nullicorn"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = rootProject.libs.plugins.kotlin.multiplatform.get().pluginId)
    apply(plugin = rootProject.libs.plugins.binary.compatibility.get().pluginId)
    apply(plugin = rootProject.libs.plugins.intellij.settings.get().pluginId)

    repositories {
        mavenCentral()
    }

    configure<IdeaModel> {
        val rootPackage = MewTeaF8BuildProperties.getRootPackageOf(this@subprojects)

        module {
            settings {
                for (sourceSet in kotlinExtension.sourceSets)
                    for (sourceDir in sourceSet.kotlin.srcDirs) {
                        val relativeSourceDir = sourceDir.toRelativeString(base = projectDir)
                        packagePrefix[relativeSourceDir] = rootPackage
                    }
            }
        }
    }
}
