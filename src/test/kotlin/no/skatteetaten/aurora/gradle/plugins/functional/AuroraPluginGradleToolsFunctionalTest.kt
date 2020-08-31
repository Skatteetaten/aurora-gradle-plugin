package no.skatteetaten.aurora.gradle.plugins.functional

import assertk.assertThat
import assertk.assertions.isTrue
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
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
                id 'com.github.ben-manes.versions' version '0.29.0'
                id 'info.solidsoft.pitest' version '1.5.2'
                id 'org.asciidoctor.convert' version '2.4.0'
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

        assertThat(result.task(":build")?.outcome == SUCCESS).isTrue()
    }

    @Test
    fun `manes-version test`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("aurora")
            .withPluginClasspath()
            .build()

        assertThat(result.output.contains("----- Aurora Plugin Report -----")).isTrue()
        assertThat(result.output.contains("plugin com.github.ben-manes.versions")).isTrue()
        assertThat(result.task(":aurora")?.outcome == SUCCESS).isTrue()
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

        assertThat(result.output.contains("----- Aurora Plugin Report -----")).isTrue()
        assertThat(result.output.contains("org.codehaus.groovy:groovy-all")).isTrue()
        assertThat(result.output.contains("org.spockframework:spock-core")).isTrue()
        assertThat(result.output.contains("cglib:cglib-nodep")).isTrue()
        assertThat(result.output.contains("org.objenesis:objenesis")).isTrue()
        assertThat(result.task(":aurora")?.outcome == SUCCESS).isTrue()
    }

    @Test
    fun `pitest test`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("aurora")
            .forwardOutput()
            .withPluginClasspath()
            .build()

        assertThat(result.output.contains("----- Aurora Plugin Report -----")).isTrue()
        assertThat(result.output.contains("output format xml and html")).isTrue()
        assertThat(result.task(":aurora")?.outcome == SUCCESS).isTrue()
    }

    @Test
    fun `asciidoctor test`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("aurora")
            .withPluginClasspath()
            .build()

        assertThat(result.output.contains("----- Aurora Plugin Report -----")).isTrue()
        assertThat(result.output.contains("configure html5 report in static/docs")).isTrue()
        assertThat(result.task(":aurora")?.outcome == SUCCESS).isTrue()
    }
}
