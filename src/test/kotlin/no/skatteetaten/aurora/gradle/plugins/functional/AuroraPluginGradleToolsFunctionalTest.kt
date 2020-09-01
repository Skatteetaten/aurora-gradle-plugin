package no.skatteetaten.aurora.gradle.plugins.functional

import PluginVersions
import assertk.assertThat
import assertk.assertions.contains
import no.skatteetaten.aurora.gradle.plugins.taskStatus
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

        result.taskStatus()
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
        result.taskStatus(taskName = ":aurora")
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
        result.taskStatus(taskName = ":aurora")
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
        result.taskStatus(taskName = ":aurora")
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
        result.taskStatus(taskName = ":aurora")
    }
}
