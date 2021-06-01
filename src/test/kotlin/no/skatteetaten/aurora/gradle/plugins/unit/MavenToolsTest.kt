@file:Suppress("DEPRECATION")

package no.skatteetaten.aurora.gradle.plugins.unit

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import no.skatteetaten.aurora.gradle.plugins.mutators.MavenTools
import no.skatteetaten.aurora.gradle.plugins.mutators.MavenTools.Companion.MISSING_REPO_CREDS_MESSAGE
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.PublishingExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

    @BeforeEach
    fun setup() {
        buildFile = testProjectDir.resolve("build.gradle")
        buildFile.createNewFile()
        buildFile.writeText("""""")
        project = ProjectBuilder.builder()
            .withProjectDir(testProjectDir)
            .build()
        mavenTools = MavenTools(project)
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
    fun `maven deployer not configured if missing creds`() {
        val report = mavenTools.addMavenDeployer()

        assertThat(report.description).isEqualTo(MISSING_REPO_CREDS_MESSAGE)
        assertThrows<UnknownTaskException> { project.tasks.getByName("deploy") }
    }

    @Test
    fun `maven deployer not configured if missing creds user`() {
        project.extensions.extraProperties["repositoryPassword"] = "repositoryPassword"
        project.extensions.extraProperties["repositoryReleaseUrl"] = "http://repositoryReleaseUrl"
        project.extensions.extraProperties["repositorySnapshotUrl"] = "http://repositorySnapshotUrl"

        val report = mavenTools.addMavenDeployer()

        assertThat(report.description).isEqualTo(MISSING_REPO_CREDS_MESSAGE)
        assertThrows<UnknownTaskException> { project.tasks.getByName("deploy") }
    }

    @Test
    fun `maven deployer not configured if missing creds pass`() {
        project.extensions.extraProperties["repositoryUsername"] = "repositoryUsername"
        project.extensions.extraProperties["repositoryReleaseUrl"] = "http://repositoryReleaseUrl"
        project.extensions.extraProperties["repositorySnapshotUrl"] = "http://repositorySnapshotUrl"

        val report = mavenTools.addMavenDeployer()

        assertThat(report.description).isEqualTo(MISSING_REPO_CREDS_MESSAGE)
        assertThrows<UnknownTaskException> { project.tasks.getByName("deploy") }
    }

    @Test
    fun `maven deployer not configured if missing creds repoUrl`() {
        project.extensions.extraProperties["repositoryUsername"] = "repositoryUsername"
        project.extensions.extraProperties["repositoryPassword"] = "repositoryPassword"
        project.extensions.extraProperties["repositorySnapshotUrl"] = "http://repositorySnapshotUrl"

        val report = mavenTools.addMavenDeployer()

        assertThat(report.description).isEqualTo(MISSING_REPO_CREDS_MESSAGE)
        assertThrows<UnknownTaskException> { project.tasks.getByName("deploy") }
    }

    @Test
    fun `maven deployer not configured if missing creds snapshotUrl`() {
        project.extensions.extraProperties["repositoryUsername"] = "repositoryUsername"
        project.extensions.extraProperties["repositoryPassword"] = "repositoryPassword"
        project.extensions.extraProperties["repositoryReleaseUrl"] = "http://repositoryReleaseUrl"

        val report = mavenTools.addMavenDeployer()

        assertThat(report.description).isEqualTo(MISSING_REPO_CREDS_MESSAGE)
        assertThrows<UnknownTaskException> { project.tasks.getByName("deploy") }
    }

    @Test
    fun `maven deployer configured correctly for release`() {
        project.extensions.extraProperties["repositoryUsername"] = "repositoryUsername"
        project.extensions.extraProperties["repositoryPassword"] = "repositoryPassword"
        project.extensions.extraProperties["repositoryReleaseUrl"] = "http://repositoryReleaseUrl"
        project.extensions.extraProperties["repositorySnapshotUrl"] = "http://repositorySnapshotUrl"
        project.plugins.apply("java")
        project.plugins.apply("maven-publish")

        val report = mavenTools.addMavenDeployer()
        val deployTask = project.tasks.getByName("upload")

        assertThat(report.description).isEqualTo(
            "add deploy task and configure from repository* properties in .gradle.properties."
        )
        assertThat(deployTask.description).isEqualTo("Build and deploy artifacts to Nexus")

        val publish = project.extensions.getByType(PublishingExtension::class.java)
        val repo = publish.repositories.getByName("repository") as MavenArtifactRepository
        val snapShotRepo = publish.repositories.findByName("snapshotRepository")

        assertThat(snapShotRepo).isNull()
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
