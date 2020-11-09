package no.skatteetaten.aurora.gradle.plugins.unit

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import no.skatteetaten.aurora.gradle.plugins.model.AuroraConfiguration
import no.skatteetaten.aurora.gradle.plugins.mutators.AnalysisTools
import org.gradle.api.Project
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import java.io.File

@Execution(CONCURRENT)
class AnalysisToolsTest {
    @TempDir
    lateinit var testProjectDir: File
    private lateinit var buildFile: File
    private lateinit var project: Project
    private lateinit var analysisTools: AnalysisTools
    private val defaultConfig = AuroraConfiguration()

    @BeforeEach
    fun setup() {
        buildFile = testProjectDir.resolve("build.gradle")
        buildFile.createNewFile()
        buildFile.writeText("""""".trimIndent())
        project = ProjectBuilder.builder()
            .withProjectDir(testProjectDir)
            .build()
        analysisTools = AnalysisTools(project)
    }

    @Test
    fun `checkstyle not configured if no java plugin`() {
        val report = analysisTools.applyCheckstylePlugin(
            checkstyleConfigVersion = defaultConfig.checkstyleConfigVersion,
            checkstyleConfigFile = defaultConfig.checkstyleConfigFile
        )

        assertThat(report.description).isEqualTo("java plugin not available, will not apply checkstyle")
    }

    @Test
    fun `checkstyle configured correctly`() {
        project.plugins.apply("java")
        val report = analysisTools.applyCheckstylePlugin(
            checkstyleConfigVersion = defaultConfig.checkstyleConfigVersion,
            checkstyleConfigFile = defaultConfig.checkstyleConfigFile
        )

        assertThat(report.description).isEqualTo("with file checkstyle/checkstyle-with-metrics.xml")
        assertThat(project.plugins.hasPlugin("checkstyle")).isTrue()

        val auroraCheckstyleConfig = project.configurations.getByName("auroraCheckstyleConfig")

        assertThat(auroraCheckstyleConfig).isNotNull()
        assertThat(auroraCheckstyleConfig.dependencies.size).isEqualTo(1)

        val deps = auroraCheckstyleConfig.dependencies
        val auroraDep = deps.first()

        assertThat(auroraDep.group).isEqualTo("no.skatteetaten.aurora.checkstyle")
        assertThat(auroraDep.name).isEqualTo("checkstyle-config")
        assertThat(auroraDep.version).isEqualTo(defaultConfig.checkstyleConfigVersion)

        val checkstyleExtension = project.extensions.getByType(CheckstyleExtension::class.java)

        assertThat(checkstyleExtension.config).isNotNull()
        assertThat(checkstyleExtension.config.inputFiles).isNotNull()
        assertThat(checkstyleExtension.isIgnoreFailures).isTrue()
    }
}
