package me.nullicorn.mewteaf8.gradle.github

import me.nullicorn.mewteaf8.gradle.publishing.MewPomConfigurator
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.kohsuke.github.*

/**
 * A [MewPomConfigurator] that uses the GitHub API to fill in the project's metadata based on its remote repository.
 *
 * @param[environment] The project's GitHub configuration, including the repository's owner & name, as well as (if
 * necessary) an access token with read access to the repository.
 */
internal fun GitHubMewPomConfigurator(environment: MewGitHubConfig): MewPomConfigurator = {
    // Ensure we have the minimal info needed to lookup the repository.
    if (environment.owner == null || environment.repository == null)
        throw InvalidUserDataException("Repository name & owner must be configured in order to configure pom.xml based on GitHub")

    val github: GitHub =
        if (environment.accessToken?.isBlank() == false)
            GitHub.connectUsingOAuth(environment.accessToken)
        else
            GitHub.connectAnonymously()

    val repository: GHRepository
    val contributors: Set<GHUser>
    val collaborators: Set<GHUser>
    val licenseMeta: GHLicense?
    val licenseFile: GHContent?

    try {
        repository = github.getRepository("${environment.owner}/${environment.repository}")!!
        contributors = repository.listContributors()!!.filterNotNull().toSet()
        collaborators = repository.collaborators!!.filterNotNull().toSet()
        licenseMeta = repository.license
        licenseFile =
            if (licenseMeta != null) repository.licenseContent
            else null

    } catch (cause: GHFileNotFoundException) {
        val statusCode = cause.responseHeaderFields?.get(":status")?.firstOrNull()

        when (statusCode) {
            "404" ->
                if (github.isAnonymous)
                    throw InvalidUserDataException("Configured GitHub repo does not exist or is private; if it's the latter case, supply an access token via the environment variable whose name is configured in gradle.properties")
                else
                    throw InvalidUserDataException("Configured GitHub repo does not exist or is private; if it's the latter case, make sure the supplied access token is able to read that repository")

            "401" ->
                if (github.isAnonymous)
                    throw InvalidUserDataException("An access token is required so that collaborators can be read & added to pom.xml as developers; add one via the environment variable whose name is configured in gradle.properties")
                else
                    throw InvalidUserDataException("The supplied access token is not able to read the repository's collaborators, which is required in order to create the developer list in pom.xml")

            else -> throw cause
        }
    }

    // Use Gradle to get the project's name instead of GitHub since subprojects can have their own unique names.
    name.set(environment.project.name)

    // Get the repo's URL, or fill it in manually if it can't be retrieved off the `repository` object for some reason.
    url.set(repository.htmlUrl?.toString() ?: "https://github.com/${environment.owner}/${environment.repository}")

    // If the repository has a non-blank description, copy that into the POM.
    if (repository.description?.isBlank() == false)
        description.set(repository.description!!)

    // Populate the POM's contributor list using the one for the GitHub repo.
    for (user in contributors)
        contributors {
            contributor {
                name.set(user.name ?: user.login ?: "<GitHub User ID: ${user.id.toString()}>")
                url.set(user.htmlUrl.toString())
            }
        }

    // Populate the POM's official "developer" list with all the repo's collaborators who have made code contributions.
    val developers = collaborators
        .filter { dev -> contributors.any { dev.id == it.id } }
        .sortedWith(Comparator.comparingInt { if (it.id == repository.owner.id) -1 else 0 })
    for (user in developers)
        developers {
            developer {
                name.set(user.name ?: user.login ?: "<GitHub User ID: ${user.id.toString()}>")
                url.set(user.htmlUrl.toString())
                id.set(user.login ?: user.name ?: "<GitHub User ID: ${user.id.toString()}>")

                if (user.email != null)
                    email.set(user.email)
            }
        }

    // Fill in the POM's source-code hosting info & point it to the repository on GitHub.
    scm {
        val repoName = repository.fullName!!

        url.set(repository.htmlUrl?.toString() ?: "https://github.com/$repoName")
        connection.set("scm:git:${repository.gitTransportUrl ?: "git://github.com/${repoName}.git"}")
        developerConnection.set("scm:git:${repository.sshUrl ?: "ssh://git@github.com:${repoName}.git"}")
    }

    // If the repository has a license, add it to the POM as well.
    if (licenseMeta != null)
        licenses {
            license {
                // Determine the URL to a copy of the license using several fallback values.
                val licenseUrl: String =
                    licenseMeta.htmlUrl?.toString() // Link to a public copy of the license itself.
                        ?: licenseFile?.htmlUrl // Link to the page for the license's file in our repo.
                        ?: licenseFile?.downloadUrl // Link to download our repo's license copy.
                        ?: licenseMeta.url?.toString() // Link to the license's GitHub API endpoint.
                        ?: licenseFile?.url?.toString()
                        ?: throw GradleException("Unable to determine a suitible URL for the repo's license")

                // Determine the license's formal name using several fallback values.
                val licenseName: String =
                    licenseMeta.name // The license's formal name.
                        ?: licenseMeta.key // The license's internal name in GitHub's API.
                        ?: licenseMeta.body?.substringBefore("\n") // The first line of the license's contents.
                        ?: licenseUrl // The license's full URL, determined above.

                url.set(licenseUrl)
                name.set(licenseName)
                distribution.set("repo")

                if (licenseMeta.description != null)
                    comments.set(licenseMeta.description)
            }
        }

}