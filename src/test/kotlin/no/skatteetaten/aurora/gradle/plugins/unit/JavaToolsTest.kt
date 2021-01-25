@file:Suppress("DEPRECATION")

package no.skatteetaten.aurora.gradle.plugins.unit

import PluginVersions
import Versions
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.endsWith
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import no.skatteetaten.aurora.gradle.plugins.isSuccessOrEqualTo
import no.skatteetaten.aurora.gradle.plugins.mutators.JavaTools
import no.skatteetaten.aurora.gradle.plugins.taskOutcome
import org.asciidoctor.gradle.jvm.AsciidoctorTask
import org.gradle.api.JavaVersion
import org.gradle.api.JavaVersion.VERSION_11
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
import java.util.zip.ZipFile

@ExperimentalStdlibApi
@Execution(CONCURRENT)
class JavaToolsTest {
    @TempDir
    lateinit var testProjectDir: File
    private lateinit var buildFile: File
    private lateinit var project: Project
    private lateinit var javaTools: JavaTools

    @BeforeEach
    fun setup() {
        buildFile = testProjectDir.resolve("build.gradle")
        buildFile.createNewFile()
        project = ProjectBuilder.builder()
            .withProjectDir(testProjectDir)
            .build()
        javaTools = JavaTools(project)
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
            """.trimIndent()
        )
        (project as ProjectInternal).evaluate()
        javaTools.applyDefaultPlugins()
        val report = javaTools.applyJavaDefaults(Versions.javaSourceCompatibility)

        assertThat(report.description).isEqualTo("Set groupId, version and add sourceCompatibility")
        assertThat(project.property("sourceCompatibility") as JavaVersion).isEqualTo(VERSION_11)
    }

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

        assertThat(libEntry?.isDirectory).isNotNull().isTrue()
        assertThat(libEntryCount.size).isEqualTo(2)
        assertThat(metaEntry?.isDirectory).isNotNull().isTrue()
        assertThat(metaEntryCount.size).isEqualTo(2)
        assertThat(result.taskOutcome()).isSuccessOrEqualTo()
    }

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

        assertThat(libEntry).isNotNull()
        assertThat(libEntry?.isDirectory).isNotNull().isTrue()
        assertThat(metaEntry).isNotNull()
        assertThat(metaEntry?.isDirectory).isNotNull().isTrue()
        assertThat(result.taskOutcome()).isSuccessOrEqualTo()
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
    fun `asciiDoctor configured correctly`() {
        buildFile.writeText(
            """
            plugins {
                id 'java'
                id 'org.asciidoctor.jvm.convert' version '${PluginVersions.asciidoctor}'
            }
            """.trimIndent()
        )
        (project as ProjectInternal).evaluate()
        val report = javaTools.applyAsciiDocPlugin()
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
