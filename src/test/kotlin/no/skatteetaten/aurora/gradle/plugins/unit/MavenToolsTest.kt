@file:Suppress("DEPRECATION")

package no.skatteetaten.aurora.gradle.plugins.unit

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import no.skatteetaten.aurora.gradle.plugins.mutators.MavenTools
import no.skatteetaten.aurora.gradle.plugins.mutators.SpringTools
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import java.io.File

@Execution(CONCURRENT)
class MavenToolsTest {
    @TempDir
    lateinit var testProjectDir: File
    private lateinit var buildFile: File
    private lateinit var project: Project
    private lateinit var mavenTools: MavenTools
    private lateinit var springTools: SpringTools

    @BeforeEach
    fun setup() {
        buildFile = testProjectDir.resolve("build.gradle")
        buildFile.createNewFile()
        buildFile.writeText("""""")
        project = ProjectBuilder.builder()
            .withProjectDir(testProjectDir)
            .build()
        mavenTools = MavenTools(project)
        springTools = SpringTools(project)
    }

    @Test
    fun `default tasks set correctly`() {
        mavenTools.setDefaultTasks()

        assertThat(project.defaultTasks).isEqualTo(listOf("clean", "install"))
    }

    @Test
    fun `default tasks not set if existing`() {
        project.defaultTasks = listOf("bogus")

        mavenTools.setDefaultTasks()

        assertThat(project.defaultTasks).isEqualTo(listOf("bogus"))
    }

    @Test
    fun `maven deployer configured correctly for release`() {
        project.extensions.extraProperties["repositoryUsername"] = "repositoryUsername"
        project.extensions.extraProperties["repositoryPassword"] = "repositoryPassword"
        project.extensions.extraProperties["repositoryReleaseUrl"] = "http://repositoryReleaseUrl"
        project.extensions.extraProperties["repositorySnapshotUrl"] = "http://repositorySnapshotUrl"
        project.plugins.apply("java")
        project.plugins.apply("maven-publish")
        project.plugins.apply("org.springframework.boot")
        project.plugins.apply("spring-cloud-contract")

        springTools.applySpring(
            mvcStarterVersion = Versions.auroraSpringBootMvcStarter,
            webFluxStarterVersion = Versions.auroraSpringBootWebFluxStarter,
            devTools = false,
            webFluxEnabled = false,
            bootJarEnabled = false,
            startersEnabled = false,
        )
        springTools.applySpringCloudContract(true, Versions.springCloudContract, webFluxEnabled = false)
        val report = mavenTools.addMavenDeployer()
        val deployTask = project.tasks.getByName("upload")

        assertThat(report.description).isEqualTo(
            "add deploy task and configure from repository* properties in .gradle.properties."
        )
        assertThat(deployTask.description).isEqualTo("Build and deploy artifacts to Nexus")

        val publish = project.extensions.getByType(PublishingExtension::class.java)
        val repo = publish.repositories.getByName("repository") as MavenArtifactRepository
        val snapShotRepo = publish.repositories.findByName("snapshotRepository")
        val publication = publish.publications.first() as MavenPublication
        val artifacts = publication.artifacts
        val stubsArtifact = artifacts.any { it.classifier == "stubs" }

        assertThat(snapShotRepo).isNull()
        assertThat(stubsArtifact).isTrue()
        assertThat(repo.url.toString()).isEqualTo("http://repositoryReleaseUrl")
        assertThat(repo.credentials.username).isEqualTo("repositoryUsername")
        assertThat(repo.credentials.password).isEqualTo("repositoryPassword")
    }

    @Test
    fun `maven deployer configured correctly for snapshot`() {
        project.extensions.extraProperties["repositoryUsername"] = "repositoryUsername"
        project.extensions.extraProperties["repositoryPassword"] = "repositoryPassword"
        project.extensions.extraProperties["repositoryReleaseUrl"] = "http://repositoryReleaseUrl"
        project.extensions.extraProperties["repositorySnapshotUrl"] = "http://repositorySnapshotUrl"
        project.plugins.apply("java")
        project.plugins.apply("maven-publish")
        project.version = "local-SNAPSHOT"

        val report = mavenTools.addMavenDeployer()
        val deployTask = project.tasks.getByName("upload")

        assertThat(report.description).isEqualTo(
            "add deploy task and configure from repository* properties in .gradle.properties."
        )
        assertThat(deployTask.description).isEqualTo("Build and deploy artifacts to Nexus")

        val publish = project.extensions.getByType(PublishingExtension::class.java)
        val repo = publish.repositories.findByName("repository")
        val snapShotRepo = publish.repositories.getByName("snapshotRepository") as MavenArtifactRepository

        assertThat(repo).isNull()
        assertThat(snapShotRepo.url.toString()).isEqualTo("http://repositorySnapshotUrl")
        assertThat(snapShotRepo.credentials.username).isEqualTo("repositoryUsername")
        assertThat(snapShotRepo.credentials.password).isEqualTo("repositoryPassword")
    }
}
