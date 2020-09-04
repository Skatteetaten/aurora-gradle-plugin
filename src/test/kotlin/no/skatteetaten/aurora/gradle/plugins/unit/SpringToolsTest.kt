@file:Suppress("DEPRECATION")

package no.skatteetaten.aurora.gradle.plugins.unit

import PluginVersions
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import no.skatteetaten.aurora.gradle.plugins.configureExtensions
import no.skatteetaten.aurora.gradle.plugins.isSuccessOrEqualTo
import no.skatteetaten.aurora.gradle.plugins.model.getConfig
import no.skatteetaten.aurora.gradle.plugins.mutators.SpringTools
import no.skatteetaten.aurora.gradle.plugins.taskOutcome
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.springframework.cloud.contract.verifier.config.TestFramework.JUNIT5
import org.springframework.cloud.contract.verifier.plugin.ContractVerifierExtension
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

    @Test
    fun `spring cloud contract configured correctly`() {
        buildFile.writeText(
            """
            buildscript {
                repositories {
                    mavenCentral()
                }
            }
            
            plugins {
                id 'java'
                id 'distribution'
                id 'org.springframework.boot' version '${PluginVersions.spring_boot}'
                id 'io.spring.dependency-management' version '${PluginVersions.dependency_management}'
                id 'org.springframework.cloud.contract' version '${PluginVersions.cloud_contract}'
            }
            
            repositories {
                mavenCentral()
            }
            """.trimIndent()
        )
        project.tasks.create("bootDistTar")
        project.tasks.create("bootDistZip")
        project.configureExtensions()
        (project as ProjectInternal).evaluate()
        val config = project.getConfig()
        springTools.applySpring(
            "1.0.9",
            "1.0.8",
            devTools = false,
            webFluxEnabled = false,
            bootJarEnabled = false,
            startersEnabled = config.useAuroraStarters
        )
        val report = springTools.applySpringCloudContract(true, config.springCloudContractVersion)
        val stubsJar = project.tasks.named("stubsJar", Jar::class.java).get()

        with(project.configurations.getByName("testImplementation").allDependencies) {
            assertThat(
                find {
                    it.group == "org.springframework.cloud" &&
                        it.name == "spring-cloud-starter-contract-stub-runner" &&
                        it.version == null
                }
            ).isNotNull()
            assertThat(
                find {
                    it.group == "org.springframework.cloud" &&
                        it.name == "spring-cloud-starter-contract-verifier" &&
                        it.version == null
                }
            ).isNotNull()
            assertThat(
                find {
                    it.group == "org.springframework.cloud" &&
                        it.name == "spring-cloud-contract-wiremock" &&
                        it.version == null
                }
            ).isNotNull()
            assertThat(
                find {
                    it.group == "org.springframework.restdocs" &&
                        it.name == "spring-restdocs-mockmvc" &&
                        it.version == null
                }
            ).isNotNull()
        }
        with(project.extensions.getByName("contracts") as ContractVerifierExtension) {
            assertThat(packageWithBaseClasses.get()).isEqualTo("${project.group}.${project.name}.contracts")
            assertThat(failOnNoContracts.get()).isFalse()
            assertThat(testFramework.get()).isEqualTo(JUNIT5)
        }
        with(stubsJar) {
            assertThat(archiveClassifier.get()).isEqualTo("stubs")
            assertThat(dependsOn).contains("test")
        }
        with(project.tasks.named("verifierStubsJar", Jar::class).get()) {
            assertThat(enabled).isFalse()
        }
        with(project) {
            artifacts {
                assertThat(
                    configurations.getByName("archives").artifacts.any {
                        it.classifier === stubsJar.archiveClassifier.get()
                    }
                ).isTrue()
            }
        }
        assertThat(report.description).isEqualTo("Configure stubs, testframework")
    }
}
