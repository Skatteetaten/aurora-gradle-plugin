package no.skatteetaten.aurora.gradle.plugins.mutators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import org.gradle.api.Project
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import java.io.File

class MavenTools(private val project: Project) {
    fun addMavenDeployer(): AuroraReport = when {
        missingRepositoryConfiguration() -> addDeployer(
            AuroraReport(
                name = "aurora.applyMavenDeployer",
                description = MISSING_REPO_CREDS_MESSAGE
            )
        )
        else -> addDeployer(
            AuroraReport(
                name = "aurora.applyMavenDeployer",
                description = "add deploy task and configure from repository* properties in .gradle.properties."
            )
        )
    }

    private fun addDeployer(report: AuroraReport): AuroraReport {
        project.logger.lifecycle("Apply maven deployment support")

        val exProps = project.extensions.extraProperties.properties
        val repositoryReleaseUrl = exProps["repositoryReleaseUrl"] as? String
        val repositorySnapshotUrl = exProps["repositorySnapshotUrl"] as? String
        val repositoryUsername = exProps["repositoryUsername"] as? String
        val repositoryPassword = exProps["repositoryPassword"] as? String

        with(project) {
            configureDeployer(
                repositoryReleaseUrl,
                repositoryUsername,
                repositoryPassword,
                repositorySnapshotUrl
            )

            with(tasks) {
                register("upload") { task ->
                    with(task) {
                        description = "Build and deploy artifacts to Nexus"

                        dependsOn(tasks.named("publish"))

                        mustRunAfter("clean")
                    }
                }
            }
        }

        return report
    }

    private fun Project.configureDeployer(
        repositoryReleaseUrl: String?,
        repositoryUsername: String?,
        repositoryPassword: String?,
        repositorySnapshotUrl: String?,
    ) {
        with(extensions.getByType(PublishingExtension::class.java)) {
            val pubVersion = project.version as? String? ?: "local-SNAPSHOT"
            val pubGroup = when {
                (project.group as String).isBlank() -> "no.skatteetaten.aurora.noop"
                else -> project.group as String
            }

            with(publications) {
                create<MavenPublication>("leveranse") {
                    from(components.getByName("java"))
                    groupId = pubGroup
                    artifactId = project.name
                    version = when {
                        pubVersion.endsWith("-SNAPSHOT-SNAPSHOT") -> pubVersion.removeSuffix("-SNAPSHOT")
                        else -> pubVersion
                    }

                    tasks.findByName("distZip")?.let { artifact(tasks["distZip"]) }

                    configurations.findByName("archives")?.let { config ->
                        config.artifacts.filter {
                            it.isCustom()
                        }.forEach {
                            artifact(it)
                        }

                        tasks.findByName("stubsJar")?.let {
                            artifact(it)
                        }
                    }

                    versionMapping {
                        with(it) {
                            usage("java-runtime") { strategy ->
                                strategy.fromResolutionResult()
                            }
                        }
                    }
                }
            }

            with(repositories) {
                when {
                    missingRepositoryConfiguration() -> mavenLocal()
                    else -> {
                        mavenLocal()

                        when (pubVersion.endsWith("SNAPSHOT")) {
                            true -> maven {
                                it.name = "snapshotRepository"
                                it.url = uri(repositorySnapshotUrl!!)
                                it.isAllowInsecureProtocol = true
                                it.credentials { credentials ->
                                    credentials.username = repositoryUsername
                                    credentials.password = repositoryPassword
                                }
                            }
                            else -> maven {
                                it.name = "repository"
                                it.url = uri(repositoryReleaseUrl!!)
                                it.isAllowInsecureProtocol = true
                                it.credentials { credentials ->
                                    credentials.username = repositoryUsername
                                    credentials.password = repositoryPassword
                                }
                            }
                        }
                    }
                }
            }
        }

        tasks.withType(GenerateMavenPom::class.java) {
            it.doLast {
                val file = File("$buildDir/publications/leveranse/pom-default.xml")
                var text = file.readText()
                val regex =
                    "(?s)(<dependencyManagement>.+?<dependencies>)(.+?)(</dependencies>.+?</dependencyManagement>)"
                        .toRegex()

                if (text.contains("dependencyManagement")) {
                    val matcher = regex.find(text)
                    if (matcher != null) {
                        text = regex.replaceFirst(text, "")
                        val firstDeps = matcher.groups[2]!!.value
                        text = regex.replaceFirst(text, "$1$2$firstDeps$3")
                    }
                    file.writeText(text)
                }
            }
        }
    }

    private fun PublishArtifact.isCustom() = when (classifier) {
        "module",
        "jar",
        "sources",
        "stubs",
        "plain",
        "Leveransepakke",
        "" -> false
        else -> true
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
                                | repositorySnapshotUrl, only local depoy possible""".trimMargin()
    }
}
