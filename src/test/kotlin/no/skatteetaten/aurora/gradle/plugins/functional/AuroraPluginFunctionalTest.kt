package no.skatteetaten.aurora.gradle.plugins.functional

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
class AuroraPluginFunctionalTest {
    @TempDir
    lateinit var testProjectDir: File
    private lateinit var buildFile: File
    private lateinit var propertiesFile: File

    @BeforeEach
    fun setup() {
        buildFile = testProjectDir.resolve("build.gradle")
        buildFile.createNewFile()
        buildFile.writeText(
            """
            plugins {
                id 'no.skatteetaten.gradle.aurora'
            }
            
            aurora {}
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
    fun `build test default version and group`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("properties")
            .withPluginClasspath()
            .build()

        assertThat(result.taskOutcome(":properties")).isSuccessOrEqualTo()
        assertThat(result.output.contains("version: local-SNAPSHOT"))
        assertThat(result.output.contains("group: no.skatteetaten.noop"))
    }

    @Test
    fun `build test overriden version`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("properties")
            .withPluginClasspath()
            .build()
        propertiesFile = testProjectDir.resolve("gradle.properties")
        propertiesFile.createNewFile()
        propertiesFile.writeText(
            """
                version=local
        """
        )

        assertThat(result.taskOutcome(":properties")).isSuccessOrEqualTo()
        assertThat(result.output.contains("version: local"))
        assertThat(result.output.contains("group: no.skatteetaten.noop"))
    }

    @Test
    fun `build test overidden group`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("properties")
            .withPluginClasspath()
            .build()
        propertiesFile = testProjectDir.resolve("gradle.properties")
        propertiesFile.createNewFile()
        propertiesFile.writeText(
            """
                groupId=no.test
        """
        )

        assertThat(result.taskOutcome(":properties")).isSuccessOrEqualTo()
        assertThat(result.output.contains("version: local-SNAPSHOT"))
        assertThat(result.output.contains("group: no.test"))
    }

    @Test
    fun `build test overidden both`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("properties")
            .withPluginClasspath()
            .build()
        propertiesFile = testProjectDir.resolve("gradle.properties")
        propertiesFile.createNewFile()
        propertiesFile.writeText(
            """
                version=local
                groupId=no.test
        """
        )

        assertThat(result.taskOutcome(":properties")).isSuccessOrEqualTo()
        assertThat(result.output.contains("version: local"))
        assertThat(result.output.contains("group: no.test"))
    }

    @Test
    fun sunshineTest() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("aurora")
            .withPluginClasspath()
            .build()

        assertThat(result.output).contains("----- Aurora Plugin Report -----")
        assertThat(result.taskOutcome(taskName = ":aurora")).isSuccessOrEqualTo()
    }
}
