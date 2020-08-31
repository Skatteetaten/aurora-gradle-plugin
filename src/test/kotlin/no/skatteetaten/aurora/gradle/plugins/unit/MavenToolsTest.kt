@file:Suppress("DEPRECATION")

package no.skatteetaten.aurora.gradle.plugins.unit

import assertk.assertThat
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gradle.plugins.mutators.MavenTools
import no.skatteetaten.aurora.gradle.plugins.mutators.MavenTools.Companion.MISSING_REPO_CREDS_MESSAGE
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.maven.MavenDeployer
import org.gradle.api.publication.maven.internal.deployer.MavenRemoteRepository
import org.gradle.api.tasks.Upload
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
    fun `maven deployer configured correctly`() {
        project.extensions.extraProperties["repositoryUsername"] = "repositoryUsername"
        project.extensions.extraProperties["repositoryPassword"] = "repositoryPassword"
        project.extensions.extraProperties["repositoryReleaseUrl"] = "http://repositoryReleaseUrl"
        project.extensions.extraProperties["repositorySnapshotUrl"] = "http://repositorySnapshotUrl"
        project.plugins.apply("maven")

        val report = mavenTools.addMavenDeployer()
        val deployTask = project.tasks.getByName("deploy")
        val uploadArchives = project.tasks.getByName("uploadArchives") as Upload

        assertThat(report.description).isEqualTo(
            "add deploy task and configure from repository* properties in .gradle.properties."
        )
        assertThat(deployTask.description).isEqualTo("Build and deploy artifacts to Nexus")
        assertThat(deployTask.dependsOn).isEqualTo(setOf("uploadArchives"))
        assertThat(deployTask.mustRunAfter.getDependencies(deployTask).first().name).isEqualTo("clean")
        assertThat(uploadArchives.repositories[0].name).isEqualTo("mavenDeployer")

        val mavenDeployer = uploadArchives.repositories[0] as MavenDeployer
        val repo = mavenDeployer.repository as MavenRemoteRepository
        val snapShotRepo = mavenDeployer.snapshotRepository as MavenRemoteRepository

        assertThat(repo.url).isEqualTo("http://repositoryReleaseUrl")
        assertThat(snapShotRepo.url).isEqualTo("http://repositorySnapshotUrl")
        assertThat(repo.authentication.userName).isEqualTo("repositoryUsername")
        assertThat(snapShotRepo.authentication.userName).isEqualTo("repositoryUsername")
        assertThat(repo.authentication.password).isEqualTo("repositoryPassword")
        assertThat(snapShotRepo.authentication.password).isEqualTo("repositoryPassword")
    }
}
