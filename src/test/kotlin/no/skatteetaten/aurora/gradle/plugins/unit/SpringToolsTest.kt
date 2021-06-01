@file:Suppress("DEPRECATION")

package no.skatteetaten.aurora.gradle.plugins.unit

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import no.skatteetaten.aurora.gradle.plugins.isSuccessOrEqualTo
import no.skatteetaten.aurora.gradle.plugins.mutators.SpringTools
import no.skatteetaten.aurora.gradle.plugins.taskOutcome
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import java.io.File

@ExperimentalStdlibApi
@Execution(CONCURRENT)
class SpringToolsTest {
    @TempDir
    lateinit var testProjectDir: File
    private lateinit var buildFile: File
    private lateinit var project: Project
    private lateinit var springTools: SpringTools

    @BeforeEach
    fun setup() {
        buildFile = testProjectDir.resolve("build.gradle")
        buildFile.createNewFile()
        project = ProjectBuilder.builder()
            .withProjectDir(testProjectDir)
            .build()
        springTools = SpringTools(project)
    }

    @Test
    fun `disable starters ok`() {
        buildFile.appendText(
            """
            plugins {
                id 'no.skatteetaten.gradle.aurora'
            }
            
            repositories {
                mavenCentral()
            }
            
            aurora {
                useSpringBoot
                
                features {
                    auroraStarters = false
                }
            }
        """
        )
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("aurora")
            .withPluginClasspath()
            .build()

        assertThat(result.output).contains("----- Aurora Plugin Report -----")
        assertThat(result.output).doesNotContain("no.skatteetaten.aurora.springboot:aurora-spring-boot-webflux-starter")
        assertThat(result.output).doesNotContain("no.skatteetaten.aurora.springboot:aurora-spring-boot-webflux-starter")
        assertThat(result.taskOutcome(taskName = ":aurora")).isSuccessOrEqualTo()
    }
}
