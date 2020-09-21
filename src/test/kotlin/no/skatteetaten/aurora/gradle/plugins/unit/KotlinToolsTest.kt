@file:Suppress("DEPRECATION")

package no.skatteetaten.aurora.gradle.plugins.unit

import PluginVersions
import Versions
import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import no.skatteetaten.aurora.gradle.plugins.configureExtensions
import no.skatteetaten.aurora.gradle.plugins.model.getConfig
import no.skatteetaten.aurora.gradle.plugins.mutators.KotlinTools
import no.skatteetaten.aurora.gradle.plugins.mutators.SpringTools
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import java.io.File

@ExperimentalStdlibApi
@Execution(CONCURRENT)
class KotlinToolsTest {
    @TempDir
    lateinit var testProjectDir: File
    private lateinit var buildFile: File
    private lateinit var project: Project
    private lateinit var kotlinTools: KotlinTools
    private lateinit var springTools: SpringTools

    @BeforeEach
    fun setup() {
        buildFile = testProjectDir.resolve("build.gradle")
        buildFile.createNewFile()
        project = ProjectBuilder.builder()
            .withProjectDir(testProjectDir)
            .build()
        kotlinTools = KotlinTools(project)
        springTools = SpringTools(project)
    }

    @Test
    fun `kotlin configured correctly`() {
        buildFile.writeText(
            """
            plugins {
                id 'org.jetbrains.kotlin.jvm' version '${Versions.kotlin}'
                id 'org.jetbrains.kotlin.plugin.spring' version '${Versions.kotlin}'
            }
            """.trimIndent()
        )
        project.configureExtensions()
        (project as ProjectInternal).evaluate()
        val config = project.getConfig()
        val report = kotlinTools.applyKotlinSupport(config.kotlinLoggingVersion)
        val springReport = springTools.applyKotlinSpringSupport()

        project.tasks.withType(KotlinCompile::class).forEach {
            with(it.kotlinOptions) {
                assertThat(suppressWarnings).isTrue()
                assertThat(jvmTarget).isEqualTo("1.8")
                assertThat(freeCompilerArgs).isEqualTo(listOf("-Xjsr305=strict"))
            }
        }

        with(project.configurations.getByName("implementation").allDependencies) {
            assertThat(
                find {
                    it.group == "com.fasterxml.jackson.module" &&
                        it.name == "jackson-module-kotlin" &&
                        it.version == null
                }
            ).isNotNull()
            assertThat(
                find {
                    it.group == "org.jetbrains.kotlin" &&
                        it.name == "kotlin-reflect" &&
                        it.version == null
                }
            ).isNotNull()
            assertThat(
                find {
                    it.group == "org.jetbrains.kotlin" &&
                        it.name == "kotlin-stdlib-jdk8" &&
                        it.version == null
                }
            ).isNotNull()
            assertThat(
                find {
                    it.group == "io.github.microutils" &&
                        it.name == "kotlin-logging" &&
                        it.version == config.kotlinLoggingVersion
                }
            ).isNotNull()
        }

        assertThat(springReport.name).isEqualTo("plugin org.jetbrains.kotlin.plugin.spring")
        assertThat(report.description).isEqualTo("jsr305 strict, jvmTarget 1.8, suppress warnings")
    }

    @Test
    fun `ktlint configured correctly`() {
        buildFile.writeText(
            """
            plugins {
                id 'org.jetbrains.kotlin.jvm' version '${Versions.kotlin}'
                id 'org.jlleitschuh.gradle.ktlint' version '${PluginVersions.ktlint}'
            }
            """
        )
        (project as ProjectInternal).evaluate()
        val report = kotlinTools.applyKtLint()
        val compileKotlin = project.tasks.getByName("compileKotlin")
        val compileTestKotlin = project.tasks.getByName("compileTestKotlin")
        val ktlintKotlinScriptCheck = project.tasks.getByName("ktlintKotlinScriptCheck")
        val ktlintMainSourceSetCheck = project.tasks.getByName("ktlintMainSourceSetCheck")
        val ktlintTestSourceSetCheck = project.tasks.getByName("ktlintTestSourceSetCheck")

        assertThat(report.description).isEqualTo("disable android")
        assertThat(compileKotlin.dependsOn).containsOnly("ktlintMainSourceSetCheck", "ktlintKotlinScriptCheck")
        assertThat(compileTestKotlin.dependsOn).containsOnly("ktlintTestSourceSetCheck")
        assertThat(ktlintKotlinScriptCheck.dependsOn).containsOnly("ktlintKotlinScriptFormat")
        assertThat(ktlintTestSourceSetCheck.dependsOn).containsOnly("ktlintTestSourceSetFormat")
        assertThat(ktlintMainSourceSetCheck.dependsOn).containsOnly("ktlintMainSourceSetFormat")
    }
}
