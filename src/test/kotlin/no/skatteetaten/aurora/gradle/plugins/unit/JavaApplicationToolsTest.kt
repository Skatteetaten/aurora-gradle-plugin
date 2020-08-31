@file:Suppress("DEPRECATION")

package no.skatteetaten.aurora.gradle.plugins.unit

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsOnly
import assertk.assertions.endsWith
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import no.skatteetaten.aurora.gradle.plugins.mutators.JavaApplicationTools
import no.skatteetaten.aurora.gradle.plugins.taskStatus
import org.asciidoctor.gradle.AsciidoctorTask
import org.gradle.api.JavaVersion
import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.jvm.tasks.Jar
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
class JavaApplicationToolsTest {
    @TempDir
    lateinit var testProjectDir: File
    private lateinit var buildFile: File
    private lateinit var project: Project
    private lateinit var javaApplicationTools: JavaApplicationTools

    @BeforeEach
    fun setup() {
        buildFile = testProjectDir.resolve("build.gradle")
        buildFile.createNewFile()
        project = ProjectBuilder.builder()
            .withProjectDir(testProjectDir)
            .build()
        javaApplicationTools = JavaApplicationTools(project)
    }

    @Test
    fun `ktlint configured correctly`() {
        buildFile.writeText(
            """
            plugins {
                id 'org.jetbrains.kotlin.jvm' version '1.4.0'
                id 'org.jlleitschuh.gradle.ktlint' version '9.3.0'
            }
            """.trimIndent()
        )
        (project as ProjectInternal).evaluate()
        val report = javaApplicationTools.applyKtLint()
        val compileKotlin = project.tasks.getByName("compileKotlin")
        val compileTestKotlin = project.tasks.getByName("compileTestKotlin")

        assertThat(report.description).isEqualTo("disable android")
        assertThat(compileKotlin.dependsOn).containsOnly("ktlintMainSourceSetCheck")
        assertThat(compileTestKotlin.dependsOn).containsOnly("ktlintTestSourceSetCheck")
    }

    @Test
    fun `java defaults configured correctly`() {
        val gradleProps = testProjectDir.resolve("gradle.properties")
        gradleProps.createNewFile()
        gradleProps.writeText(
            """
            version=local
            groupId=no.test
            """.trimIndent()
        )
        buildFile.writeText(
            """
            plugins {
                id 'java'
            }
            """.trimIndent()
        )
        (project as ProjectInternal).evaluate()
        val report = javaApplicationTools.applyJavaDefaults("1.8")

        assertThat(report.description).isEqualTo("Set groupId, version and add sourceCompability")
        assertThat(project.property("sourceCompatibility") as JavaVersion).isEqualTo(VERSION_1_8)
    }

    @Test
    fun `java defaults configure version and group correctly`() {
        val gradleProps = testProjectDir.resolve("gradle.properties")
        gradleProps.createNewFile()
        gradleProps.writeText(
            """
            version=local
            groupId=no.test
            """.trimIndent()
        )
        buildFile.writeText(
            """
            plugins {
                id 'no.skatteetaten.gradle.aurora'
            }
            """.trimIndent()
        )
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("build")
            .forwardOutput()
            .withPluginClasspath()
            .build()
        val jar = testProjectDir.resolve("build/libs").list()?.first() ?: "bogus"

        assertThat(jar).endsWith("-local.jar")
        result.taskStatus()
    }

    @Test
    fun `asciiDoctor configured correctly`() {
        buildFile.writeText(
            """
            plugins {
                id 'java'
                id 'org.asciidoctor.convert' version '2.4.0'
            }
            """.trimIndent()
        )
        (project as ProjectInternal).evaluate()
        val report = javaApplicationTools.applyAsciiDocPlugin()
        val jar = project.tasks.named("jar", Jar::class.java).get()
        val asciiDoctor = project.tasks.named("asciidoctor", AsciidoctorTask::class.java).get()

        assertThat(report.description).isEqualTo("configure html5 report in static/docs")
        assertThat(asciiDoctor.outputDir.path).isEqualTo("${project.buildDir}/asciidoc")
        assertThat(asciiDoctor.sourceDir).isEqualTo(project.file("${project.projectDir}/src/main/asciidoc"))
        assertThat(asciiDoctor.dependsOn).contains("test")
        assertThat(jar).isNotNull()
        assertThat(jar.dependsOn).contains("asciidoctor")
    }
}
