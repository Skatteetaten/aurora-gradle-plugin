@file:Suppress("DEPRECATION")

package no.skatteetaten.aurora.gradle.plugins.unit

import PluginVersions
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import no.skatteetaten.aurora.gradle.plugins.mutators.MiscellaneousTools
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import java.io.File

@ExperimentalStdlibApi
@Execution(CONCURRENT)
class MiscellaneousToolsTest {
    @TempDir
    lateinit var testProjectDir: File
    private lateinit var buildFile: File
    private lateinit var project: Project
    private lateinit var miscellaneousTools: MiscellaneousTools

    @BeforeEach
    fun setup() {
        buildFile = testProjectDir.resolve("build.gradle")
        buildFile.createNewFile()
        project = ProjectBuilder.builder()
            .withProjectDir(testProjectDir)
            .build()
        miscellaneousTools = MiscellaneousTools(project)
    }

    @Test
    fun `ben-manes versions configured correctly`() {
        buildFile.writeText(
            """
            plugins {
                id 'com.github.ben-manes.versions' version '${PluginVersions.ben_manes_versions}'
            }
            """.trimIndent()
        )
        (project as ProjectInternal).evaluate()
        val report = miscellaneousTools.applyVersions()
        val dependencyUpdates = project.tasks.named("dependencyUpdates", DependencyUpdatesTask::class.java).get()

        assertThat(dependencyUpdates.revision).isEqualTo("release")
        assertThat(dependencyUpdates.checkForGradleUpdate).isEqualTo(true)
        assertThat(dependencyUpdates.outputFormatter).isEqualTo("json")
        assertThat(dependencyUpdates.outputDir).isEqualTo("build/dependencyUpdates")
        assertThat(dependencyUpdates.reportfileName).isEqualTo("report")
        assertThat(report.description).isEqualTo("only allow stable versions in upgrade")
    }
}
