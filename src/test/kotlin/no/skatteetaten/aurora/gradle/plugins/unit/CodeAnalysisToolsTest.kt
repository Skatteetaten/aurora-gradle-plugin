package no.skatteetaten.aurora.gradle.plugins.unit

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import no.skatteetaten.aurora.gradle.plugins.model.AuroraConfiguration
import no.skatteetaten.aurora.gradle.plugins.mutators.CodeAnalysisTools
import org.gradle.api.Project
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import java.io.File

@Execution(CONCURRENT)
class CodeAnalysisToolsTest {
    @TempDir
    lateinit var testProjectDir: File
    private lateinit var buildFile: File
    private lateinit var project: Project
    private lateinit var codeAnalysisTools: CodeAnalysisTools
    private val defaultConfig = AuroraConfiguration()

    @BeforeEach
    fun setup() {
        buildFile = testProjectDir.resolve("build.gradle")
        buildFile.createNewFile()
        buildFile.writeText("""""".trimIndent())
        project = ProjectBuilder.builder()
            .withProjectDir(testProjectDir)
            .build()
        codeAnalysisTools = CodeAnalysisTools(project)
    }

    @Test
    fun `jacoco not configured if no java plugin`() {
        val report = codeAnalysisTools.applyJacocoTestReport()

        assertThat(report.description).isEqualTo("java plugin not available, cannot apply jacoco")
    }

    @Test
    fun `jacoco configured correctly`() {
        project.plugins.apply("java")
        val report = codeAnalysisTools.applyJacocoTestReport()

        assertThat(report.description).isEqualTo("enable xml, disable csv report")
        assertThat(project.plugins.hasPlugin("jacoco")).isTrue()

        val jacocoReport = project.tasks.named("jacocoTestReport", JacocoReport::class.java).get()

        assertThat(jacocoReport.reports.xml.isEnabled).isTrue()
        assertThat(jacocoReport.reports.xml.destination).isEqualTo(
            project.file("${project.buildDir}/reports/jacoco/report.xml")
        )
        assertThat(jacocoReport.reports.csv.isEnabled).isFalse()
    }
    @Test
    fun `pitest not configured if no java plugin`() {
        val report = codeAnalysisTools.applyPiTestSupport()

        assertThat(report.description).isEqualTo("java plugin not available, cannot apply pitest")
    }

    @Test
    fun `checkstyle not configured if no java plugin`() {
        val report = codeAnalysisTools.applyCheckstylePlugin(
            checkstyleConfigVersion = defaultConfig.checkstyleConfigVersion,
            checkstyleConfigFile = defaultConfig.checkstyleConfigFile
        )

        assertThat(report.description).isEqualTo("java plugin not available, will not apply checkstyle")
    }

    @Test
    fun `checkstyle configured correctly`() {
        project.plugins.apply("java")
        val report = codeAnalysisTools.applyCheckstylePlugin(
            checkstyleConfigVersion = defaultConfig.checkstyleConfigVersion,
            checkstyleConfigFile = defaultConfig.checkstyleConfigFile
        )

        assertThat(report.description).isEqualTo("with file checkstyle/checkstyle-with-metrics.xml")
        assertThat(project.plugins.hasPlugin("checkstyle")).isTrue()

        val auroraCheckstyleConfig = project.configurations.getByName("auroraCheckstyleConfig")

        assertThat(auroraCheckstyleConfig).isNotNull()
        assertThat(auroraCheckstyleConfig.dependencies).hasSize(1)

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
