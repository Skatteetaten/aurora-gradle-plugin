@file:Suppress("DEPRECATION")

package no.skatteetaten.aurora.gradle.plugins.unit

import Versions
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import no.skatteetaten.aurora.gradle.plugins.configureExtensions
import no.skatteetaten.aurora.gradle.plugins.model.getConfig
import no.skatteetaten.aurora.gradle.plugins.mutators.TestTools
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.kotlin.dsl.named
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
class TestToolsTest {
    @TempDir
    lateinit var testProjectDir: File
    private lateinit var buildFile: File
    private lateinit var project: Project
    private lateinit var testTools: TestTools

    @BeforeEach
    fun setup() {
        buildFile = testProjectDir.resolve("build.gradle")
        buildFile.createNewFile()
        project = ProjectBuilder.builder()
            .withProjectDir(testProjectDir)
            .build()
        testTools = TestTools(project)
    }

    @Test
    fun `spock configured correctly`() {
        buildFile.writeText(
            """
            plugins {
                id 'java'
                id 'org.jetbrains.kotlin.jvm' version '${Versions.kotlin}'
            }
            
            repositories {
                mavenCentral()
            }
            """.trimIndent()
        )
        project.configureExtensions()
        (project as ProjectInternal).evaluate()
        val config = project.getConfig()
        val report = testTools.applySpockSupport(
            groovyVersion = config.groovyVersion,
            spockVersion = config.spockVersion,
            cglibVersion = config.cglibVersion,
            objenesisVersion = config.objenesisVersion
        )
        val kotlinTestCompile = (project.tasks.getByName("compileTestKotlin") as KotlinCompile)
        val compileTestGroovy = project.tasks.named("compileTestGroovy", GroovyCompile::class).get()

        assertThat(project.plugins.hasPlugin("groovy")).isTrue()
        with(project.configurations.getByName("testImplementation").allDependencies) {
            assertThat(
                find {
                    it.group == "org.codehaus.groovy" &&
                        it.name == "groovy-all" &&
                        it.version == config.groovyVersion
                }
            ).isNotNull()
            assertThat(
                find {
                    it.group == "org.spockframework" &&
                        it.name == "spock-core" &&
                        it.version == config.spockVersion
                }
            ).isNotNull()
            assertThat(
                find {
                    it.group == "cglib" &&
                        it.name == "cglib-nodep" &&
                        it.version == config.cglibVersion
                }
            ).isNotNull()
            assertThat(
                find {
                    it.group == "org.objenesis" &&
                        it.name == "objenesis" &&
                        it.version == config.objenesisVersion
                }
            ).isNotNull()
        }
        with(compileTestGroovy) {
            assertThat(dependsOn).contains(kotlinTestCompile)
        }
        with(project.tasks.getByName("testClasses")) {
            assertThat(dependsOn).contains(compileTestGroovy)
        }
        assertThat(report.name).contains("aurora.applySpockSupport")
    }
}
