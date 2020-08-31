package no.skatteetaten.aurora.gradle.plugins.unit

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gradle.plugins.mutators.JavaApplicationTools
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
class JavaApplicationToolsKotlinTest {
    @TempDir
    lateinit var testProjectDir: File
    private lateinit var buildFile: File
    private lateinit var project: Project
    private lateinit var javaApplicationTools: JavaApplicationTools

    @BeforeEach
    fun setup() {
        buildFile = testProjectDir.resolve("build.gradle")
        buildFile.createNewFile()
        buildFile.writeText(
            """
            plugins {
                id 'org.jetbrains.kotlin.jvm' version '1.4.0'
                id 'org.jlleitschuh.gradle.ktlint' version '9.3.0'
            }
            """.trimIndent()
        )
        project = ProjectBuilder.builder()
            .withProjectDir(testProjectDir)
            .build()
        javaApplicationTools = JavaApplicationTools(project)
    }

    @Test
    fun `ktlint configured correctly`() {
        (project as ProjectInternal).evaluate()
        val report = javaApplicationTools.applyKtLint()
        val compileKotlin = project.tasks.getByName("compileKotlin")
        val compileTestKotlin = project.tasks.getByName("compileTestKotlin")

        assertThat(report.description).isEqualTo("disable android")
        assertThat(compileKotlin.dependsOn).containsOnly("ktlintMainSourceSetCheck")
        assertThat(compileTestKotlin.dependsOn).containsOnly("ktlintTestSourceSetCheck")
    }
}
