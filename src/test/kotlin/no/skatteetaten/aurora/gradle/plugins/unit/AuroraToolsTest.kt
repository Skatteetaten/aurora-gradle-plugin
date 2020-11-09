@file:Suppress("DEPRECATION")

package no.skatteetaten.aurora.gradle.plugins.unit

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.endsWith
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import no.skatteetaten.aurora.gradle.plugins.AuroraPlugin
import no.skatteetaten.aurora.gradle.plugins.isSuccessOrEqualTo
import no.skatteetaten.aurora.gradle.plugins.mutators.AuroraTools
import no.skatteetaten.aurora.gradle.plugins.taskOutcome
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import java.io.File
import java.util.zip.ZipFile

@ExperimentalStdlibApi
@Execution(CONCURRENT)
class AuroraToolsTest {
    @TempDir
    lateinit var testProjectDir: File
    private lateinit var buildFile: File
    private lateinit var project: Project
    private lateinit var auroraTools: AuroraTools

    @BeforeEach
    fun setup() {
        buildFile = testProjectDir.resolve("build.gradle")
        buildFile.createNewFile()
        project = ProjectBuilder.builder()
            .withProjectDir(testProjectDir)
            .build()
        auroraTools = AuroraTools(project)
    }

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun `deliveryBundleConfig built correctly for bootJar`() {
        testProjectDir.resolve("src/main/java").mkdirs()
        testProjectDir.resolve("src/main/dist/metadata").mkdirs()
        val openshiftMeta = testProjectDir.resolve("src/main/dist/metadata/openshift.json")
        openshiftMeta.createNewFile()
        openshiftMeta.writeText(
            """
            {}
            """.trimIndent()
        )
        val application = testProjectDir.resolve("src/main/java/Application.kt")
        application.createNewFile()
        application.writeText(
            """
            import org.springframework.boot.SpringApplication.run
            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class Application

            fun main(args: Array<String>) {
                run(Application::class.java, *args)
            }
            """.trimIndent()
        )
        buildFile.writeText(
            """
            plugins {
                id 'no.skatteetaten.gradle.aurora'
            }
            
            repositories {
                mavenCentral()
            }
            
            aurora {
                useKotlin
                useSpringBoot {
                    useBootJar
                }
            }
            """.trimIndent()
        )
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("build")
            .withPluginClasspath()
            .build()
        val jar = testProjectDir.resolve("build/distributions").listFiles()
            ?.find { it.path.contains("Leveransepakke") }
        val jarAsZip = ZipFile(jar)
        val zipEntries = jarAsZip.entries().toList()
        val libEntry = zipEntries.find { it.name.endsWith("lib/") }
        val libEntryCount = zipEntries.filter { it.name.contains("Leveransepakke/lib") }
        val metaEntry = zipEntries.find { it.name.endsWith("metadata/") }
        val metaEntryCount = zipEntries.filter { it.name.contains("Leveransepakke/metadata") }
        val distDir = testProjectDir.resolve("build/distributions")

        assertThat(libEntry?.isDirectory ?: false).isTrue()
        assertThat(libEntryCount.size).isEqualTo(2)
        assertThat(metaEntry?.isDirectory ?: false).isTrue()
        assertThat(metaEntryCount.size).isEqualTo(2)
        assertThat(distDir.listFiles().size).isEqualTo(1)
        assertThat(distDir.listFiles().first().name).contains("Leveransepakke")
        assertThat(result.taskOutcome()).isSuccessOrEqualTo()
    }

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun `deliveryBundleConfig built correctly for non-bootJar`() {
        testProjectDir.resolve("src/main/java").mkdirs()
        testProjectDir.resolve("src/main/dist/metadata").mkdirs()
        val openshiftMeta = testProjectDir.resolve("src/main/dist/metadata/openshift.json")
        openshiftMeta.createNewFile()
        openshiftMeta.writeText(
            """
            {}
            """.trimIndent()
        )
        val application = testProjectDir.resolve("src/main/java/Application.kt")
        application.createNewFile()
        application.writeText(
            """
            import org.springframework.boot.SpringApplication.run
            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class Application

            fun main(args: Array<String>) {
                run(Application::class.java, *args)
            }
            """.trimIndent()
        )
        buildFile.writeText(
            """
            plugins {
                id 'no.skatteetaten.gradle.aurora'
            }
            
            repositories {
                mavenCentral()
            }
            
            aurora {
                useKotlin
                useSpringBoot
            }
            """.trimIndent()
        )
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("build")
            .withPluginClasspath()
            .build()
        val jar = testProjectDir.resolve("build/distributions").listFiles()
            ?.find { it.path.contains("Leveransepakke") }
        val jarAsZip = ZipFile(jar)
        val libEntry = jarAsZip.entries().toList().find { it.name.endsWith("lib/") }
        val metaEntry = jarAsZip.entries().toList().find { it.name.endsWith("metadata/") }
        val distDir = testProjectDir.resolve("build/distributions")

        assertThat(libEntry).isNotNull()
        assertThat(libEntry?.isDirectory ?: false).isTrue()
        assertThat(metaEntry).isNotNull()
        assertThat(metaEntry?.isDirectory ?: false).isTrue()
        assertThat(result.taskOutcome()).isSuccessOrEqualTo()
        assertThat(distDir.listFiles()).hasSize(1)
        assertThat(distDir.listFiles().first().name).contains("Leveransepakke")
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
            .withPluginClasspath()
            .build()
        val jar = testProjectDir.resolve("build/libs").list()?.first() ?: "bogus"

        assertThat(jar).endsWith("-local.jar")
        assertThat(result.taskOutcome()).isSuccessOrEqualTo()
    }

    @Test
    fun `ben-manes versions test resolutionStrategy`() {
        buildFile.writeText(
            """
            plugins {
                id 'java'
                id 'no.skatteetaten.gradle.aurora'
            }
            
            repositories {
                mavenCentral()
            }
            
            aurora {
                useVersions
            }
            
            dependencies { 
                compile group: 'org.seleniumhq.selenium', name: 'selenium-leg-rc', version: '4.0.0-alpha-6'
            }
            """.trimIndent()
        )
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("dependencyUpdates")
            .withPluginClasspath()
            .build()

        assertThat(result.output).contains("The following dependencies exceed the version found at the release revision level")
        assertThat(result.output).contains("org.seleniumhq.selenium:selenium-leg-rc [4.0.0-alpha-6 <- ")
        assertThat(result.taskOutcome(taskName = ":dependencyUpdates")).isSuccessOrEqualTo()
    }

    @Test
    fun `override plugin test`() {
        buildFile.writeText(
            """
            plugins {
                id("org.jetbrains.kotlin.jvm") version("1.3.70")
            }
        """
        )
        val project = ProjectBuilder.builder()
            .withProjectDir(testProjectDir)
            .build()
        project.plugins.apply(AuroraPlugin::class)
        (project as ProjectInternal).evaluate()

        assertThat(
            project.buildscript.scriptClassPath.asURLs.any {
                it.toString().endsWith("kotlin-gradle-plugin-1.3.70.jar")
            }
        ).isTrue()
    }
}
