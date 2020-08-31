@file:Suppress("DEPRECATION")

package no.skatteetaten.aurora.gradle.plugins.unit

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsOnly
import assertk.assertions.endsWith
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import no.skatteetaten.aurora.gradle.plugins.model.getConfig
import no.skatteetaten.aurora.gradle.plugins.mutators.JavaApplicationTools
import no.skatteetaten.aurora.gradle.plugins.taskStatus
import org.asciidoctor.gradle.AsciidoctorTask
import org.gradle.api.JavaVersion
import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import java.io.File
import java.util.zip.ZipFile

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
            """.trimIndent()
        )
        (project as ProjectInternal).evaluate()
        javaApplicationTools.applyDefaultPlugins()
        val report = javaApplicationTools.applyJavaDefaults("1.8")

        assertThat(report.description).isEqualTo("Set groupId, version and add sourceCompability")
        assertThat(project.property("sourceCompatibility") as JavaVersion).isEqualTo(VERSION_1_8)
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

        jarAsZip.entries().toList().forEach {
            println(it.name)
        }

        val libEntry = zipEntries.find { it.name.endsWith("lib/") }
        val libEntryCount = zipEntries.filter { it.name.contains("Leveransepakke/lib") }
        val metaEntry = zipEntries.find { it.name.endsWith("metadata/") }
        val metaEntryCount = zipEntries.filter { it.name.contains("Leveransepakke/metadata") }

        assertThat(libEntry?.isDirectory ?: false).isTrue()
        assertThat(libEntryCount).hasSize(2)
        assertThat(metaEntry?.isDirectory ?: false).isTrue()
        assertThat(metaEntryCount).hasSize(2)
        result.taskStatus()
    }

    @Test
    fun `deliveryBundleConfig configured correctly for bootJar`() {
        buildFile.writeText(
            """
            plugins {
                id 'java'
                id 'org.springframework.boot' version '2.3.3.RELEASE'
            }
            """.trimIndent()
        )
        (project as ProjectInternal).evaluate()
        javaApplicationTools.applySpring(
            "1.0.9",
            "1.0.8",
            devTools = false,
            webFluxEnabled = false,
            bootJarEnabled = true
        )
        val report = javaApplicationTools.applyDeliveryBundleConfig(true)
        val distZip = project.tasks.named("distZip", Zip::class.java).get()

        assertThat(distZip.archiveClassifier.get()).isEqualTo("Leveransepakke")
        assertThat(report.description).isEqualTo("Configure Leveransepakke for bootJar")
    }

    @Test
    fun `kotlin configured correctly`() {
        buildFile.writeText(
            """
            plugins {
                id 'org.jetbrains.kotlin.jvm' version '1.3.72'
                id 'org.jetbrains.kotlin.plugin.spring' version '1.3.72'
            }
            """.trimIndent()
        )
        (project as ProjectInternal).evaluate()
        val config = project.getConfig()
        val springReport = javaApplicationTools.applyKotlinSpringSupport()
        val report = javaApplicationTools.applyKotlinSupport(config.kotlinLoggingVersion)

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
        assertThat(report.description).isEqualTo("jsr305 strict, jvmTarget 1.8, supress warnings")
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
        assertThat(libEntry?.isDirectory ?: false).isTrue()
        assertThat(metaEntry).isNotNull()
        assertThat(metaEntry?.isDirectory ?: false).isTrue()
        result.taskStatus()
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
        result.taskStatus()
    }

    @Test
    fun `ben-manes versions configured correctly`() {
        buildFile.writeText(
            """
            plugins {
                id 'com.github.ben-manes.versions' version '0.29.0'
            }
            """.trimIndent()
        )
        (project as ProjectInternal).evaluate()
        val report = javaApplicationTools.applyVersions()
        val dependencyUpdates = project.tasks.named("dependencyUpdates", DependencyUpdatesTask::class.java).get()

        assertThat(dependencyUpdates.revision).isEqualTo("release")
        assertThat(dependencyUpdates.checkForGradleUpdate).isEqualTo(true)
        assertThat(dependencyUpdates.outputFormatter).isEqualTo("json")
        assertThat(dependencyUpdates.outputDir).isEqualTo("build/dependencyUpdates")
        assertThat(dependencyUpdates.reportfileName).isEqualTo("report")
        assertThat(report.description).isEqualTo("only allow stable versions in upgrade")
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
        result.taskStatus(taskName = ":dependencyUpdates")
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
