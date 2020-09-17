package no.skatteetaten.aurora.gradle.plugins.mutators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import org.gradle.api.Project
import org.gradle.api.plugins.MavenRepositoryHandlerConvention
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.Upload
import org.gradle.kotlin.dsl.withConvention
import org.gradle.kotlin.dsl.withGroovyBuilder

class MavenTools(private val project: Project) {
    fun addMavenDeployer(): AuroraReport = when {
        missingRepositoryConfiguration() -> AuroraReport(
            name = "aurora.applyMavenDeployer",
            description = MISSING_REPO_CREDS_MESSAGE
        )
        else -> {
            project.logger.lifecycle("Apply maven deployment support")

            val exProps = project.extensions.extraProperties.properties
            val repositoryReleaseUrl = exProps["repositoryReleaseUrl"] as String
            val repositorySnapshotUrl = exProps["repositorySnapshotUrl"] as String
            val repositoryUsername = exProps["repositoryUsername"] as String
            val repositoryPassword = exProps["repositoryPassword"] as String

            with(project) {
                with(tasks) {
                    configureDeployer(
                        repositoryReleaseUrl,
                        repositoryUsername,
                        repositoryPassword,
                        repositorySnapshotUrl
                    )

                    configureDeployTask()
                }
            }

            AuroraReport(
                name = "aurora.applyMavenDeployer",
                description = "add deploy task and configure from repository* properties in .gradle.properties."
            )
        }
    }

    private fun TaskContainer.configureDeployTask() {
        with(register("deploy").get()) {
            description = "Build and deploy artifacts to Nexus"

            dependsOn("uploadArchives")
            mustRunAfter("clean")
        }
    }

    private fun TaskContainer.configureDeployer(
        repositoryReleaseUrl: String,
        repositoryUsername: String,
        repositoryPassword: String,
        repositorySnapshotUrl: String
    ) = named("uploadArchives", Upload::class.java) {
        repositories {
            withConvention(MavenRepositoryHandlerConvention::class) {
                mavenDeployer {
                    withGroovyBuilder {
                        "repository"("url" to repositoryReleaseUrl) {
                            "authentication"(
                                "userName" to repositoryUsername,
                                "password" to repositoryPassword
                            )
                        }
                        "snapshotRepository"("url" to repositorySnapshotUrl) {
                            "authentication"(
                                "userName" to repositoryUsername,
                                "password" to repositoryPassword
                            )
                        }
                    }
                }
            }
        }
    }

    private fun missingRepositoryConfiguration(): Boolean =
        !(
            project.extensions.extraProperties.properties.containsKey("repositoryUsername") &&
                project.extensions.extraProperties.properties.containsKey("repositoryPassword") &&
                project.extensions.extraProperties.properties.containsKey("repositoryReleaseUrl") &&
                project.extensions.extraProperties.properties.containsKey("repositorySnapshotUrl")
            )

    fun setDefaultTasks() {
        if (project.defaultTasks.isEmpty()) {
            project.logger.lifecycle("Apply clean and install as default tasks")

            project.defaultTasks = listOf("clean", "install")
        }
    }

    companion object {
        val MISSING_REPO_CREDS_MESSAGE =
            """One of the following properties are missing in your .gradle file
                                | repositoryUsername, repositoryPassword, repositoryReleaseUrl,
                                | repositorySnapshotUrl""".trimMargin()
    }
}
