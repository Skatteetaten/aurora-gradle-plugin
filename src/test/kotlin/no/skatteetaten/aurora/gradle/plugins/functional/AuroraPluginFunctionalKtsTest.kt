package no.skatteetaten.aurora.gradle.plugins.functional

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
class AuroraPluginFunctionalKtsTest {
    @TempDir
    lateinit var testProjectDir: File
    private lateinit var buildFile: File

    @BeforeEach
    fun setup() {
        buildFile = testProjectDir.resolve("build.gradle.kts")
        buildFile.createNewFile()
    }

    @Test
    fun `build test`() {
        buildFile.writeText(
            """
            plugins {
                id("no.skatteetaten.gradle.aurora")
            }
            
            aurora {}
        """
        )
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("build")
            .withPluginClasspath()
            .build()

        result.taskStatus()
    }

    @Test
    fun sunshineTest() {
        buildFile.writeText(
            """
            plugins {
                id("no.skatteetaten.gradle.aurora")
            }
            
            aurora {}
        """
        )
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("aurora")
            .withPluginClasspath()
            .build()

        assertThat(result.output).contains("----- Aurora Plugin Report -----")
        result.taskStatus(taskName = ":aurora")
    }
}
