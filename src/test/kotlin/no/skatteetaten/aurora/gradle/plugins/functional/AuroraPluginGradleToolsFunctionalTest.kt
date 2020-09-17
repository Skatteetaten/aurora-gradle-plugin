package no.skatteetaten.aurora.gradle.plugins.functional

import PluginVersions
import assertk.assertThat
import assertk.assertions.contains
import no.skatteetaten.aurora.gradle.plugins.isSuccessOrEqualTo
import no.skatteetaten.aurora.gradle.plugins.taskOutcome
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import java.io.File

@Execution(CONCURRENT)
class AuroraPluginGradleToolsFunctionalTest {
    @TempDir
    lateinit var testProjectDir: File
    private lateinit var buildFile: File

    @BeforeEach
    fun setup() {
        buildFile = testProjectDir.resolve("build.gradle")
        buildFile.createNewFile()
        buildFile.writeText(
            """
            plugins {
                id 'no.skatteetaten.gradle.aurora'
                id 'com.github.ben-manes.versions' version '${PluginVersions.ben_manes_versions}'
                id 'info.solidsoft.pitest' version '${PluginVersions.pitest}'
                id 'org.asciidoctor.convert' version '${PluginVersions.asciidoctor}'
            }
        """
        )
    }

    @Test
    fun `build test`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("build")
            .withPluginClasspath()
            .build()

        assertThat(result.taskOutcome()).isSuccessOrEqualTo()
    }

    @Test
    fun `manes-version test`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("aurora")
            .withPluginClasspath()
            .build()

        assertThat(result.output).contains("----- Aurora Plugin Report -----")
        assertThat(result.output).contains("plugin com.github.ben-manes.versions")
        assertThat(result.taskOutcome(taskName = ":aurora")).isSuccessOrEqualTo()
    }

    @Test
    fun `spock test`() {
        val gradleProps = testProjectDir.resolve("gradle.properties")
        gradleProps.createNewFile()
        gradleProps.writeText(
            """
            aurora.applySpockSupport=true
            """.trimIndent()
        )
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("aurora")
            .withPluginClasspath()
            .build()

        assertThat(result.output).contains("----- Aurora Plugin Report -----")
        assertThat(result.output).contains("org.codehaus.groovy:groovy-all")
        assertThat(result.output).contains("org.spockframework:spock-core")
        assertThat(result.output).contains("cglib:cglib-nodep")
        assertThat(result.output).contains("org.objenesis:objenesis")
        assertThat(result.taskOutcome(taskName = ":aurora")).isSuccessOrEqualTo()
    }

    @Test
    fun `pitest test`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("aurora")
            .withPluginClasspath()
            .build()

        assertThat(result.output).contains("----- Aurora Plugin Report -----")
        assertThat(result.output).contains("output format xml and html")
        assertThat(result.taskOutcome(taskName = ":aurora")).isSuccessOrEqualTo()
    }

    @Test
    fun `asciidoctor test`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("aurora")
            .withPluginClasspath()
            .build()

        assertThat(result.output).contains("----- Aurora Plugin Report -----")
        assertThat(result.output).contains("configure html5 report in static/docs")
        assertThat(result.taskOutcome(taskName = ":aurora")).isSuccessOrEqualTo()
    }
}
