package no.skatteetaten.aurora.gradle.plugins.functional

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import java.io.File

@Execution(CONCURRENT)
class AuroraPluginSpringBootKotlinFunctionalTest {
    @TempDir
    lateinit var testProjectDir: File
    private lateinit var buildFile: File

    @BeforeEach
    fun setup() {
        buildFile = testProjectDir.resolve("build.gradle")
        buildFile.createNewFile()
        buildFile.writeText(
            """
            plugins {
                id 'no.skatteetaten.gradle.aurora'
                id 'org.springframework.boot' version '2.3.3.RELEASE'
                id 'org.jetbrains.kotlin.jvm' version '1.3.72'
                id 'org.jetbrains.kotlin.plugin.spring' version '1.3.72'
            }
 
            repositories {
                mavenCentral()
            }
        """
        )
    }

    @Test
    fun `build test`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("build")
            .withPluginClasspath()
            .build()

        assertThat(result.task(":build")?.outcome == SUCCESS).isTrue()
    }

    @Test
    fun sunshineTest() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("aurora")
            .withPluginClasspath()
            .build()

        assertThat(result.output.contains("----- Aurora Plugin Report -----")).isTrue()
        assertThat(result.output.contains("com.fasterxml.jackson.module:jackson-module-kotlin")).isTrue()
        assertThat(result.output.contains("org.jetbrains.kotlin:kotlin-reflect")).isTrue()
        assertThat(result.output.contains("org.jetbrains.kotlin:kotlin-stdlib-jdk8")).isTrue()
        assertThat(result.output.contains("io.github.microutils:kotlin-logging")).isTrue()
        assertThat(result.task(":aurora")?.outcome == SUCCESS).isTrue()
    }

    @Test
    fun `mvc test`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("aurora")
            .withPluginClasspath()
            .build()

        assertThat(result.output.contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-mvc-starter")).isTrue()
        assertThat(result.output.contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-webflux-starter")).isFalse()
        assertThat(result.task(":aurora")?.outcome == SUCCESS).isTrue()
    }

    @Test
    fun `webflux test`() {
        val gradleProps = testProjectDir.resolve("gradle.properties")
        gradleProps.createNewFile()
        gradleProps.writeText("""aurora.useWebFlux=true""")

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("aurora")
            .withPluginClasspath()
            .build()

        assertThat(result.output.contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-mvc-starter")).isFalse()
        assertThat(result.output.contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-webflux-starter")).isTrue()
        assertThat(result.output.contains("webflux enabled and webmvc + tomcat excluded")).isTrue()
        assertThat(result.task(":aurora")?.outcome == SUCCESS).isTrue()
    }

    @Test
    fun `bootjar test`() {
        val gradleProps = testProjectDir.resolve("gradle.properties")
        gradleProps.createNewFile()
        gradleProps.writeText("""aurora.useBootJar=true""")

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("aurora")
            .withPluginClasspath()
            .build()

        assertThat(result.output.contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-mvc-starter")).isTrue()
        assertThat(result.output.contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-webflux-starter")).isFalse()
        assertThat(result.output.contains("bootJar enabled")).isTrue()
        assertThat(result.output.contains("bootJar disabled")).isFalse()
        assertThat(result.task(":aurora")?.outcome == SUCCESS).isTrue()
    }

    @Test
    fun `devtools test`() {
        val gradleProps = testProjectDir.resolve("gradle.properties")
        gradleProps.createNewFile()
        gradleProps.writeText(
            """
            aurora.useBootJar=true
            aurora.springDevTools=true
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("aurora")
            .withPluginClasspath()
            .build()

        assertThat(result.output.contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-mvc-starter")).isTrue()
        assertThat(result.output.contains("org.springframework.boot:spring-boot-devtools")).isTrue()
        assertThat(result.output.contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-webflux-starter")).isFalse()
        assertThat(result.output.contains("bootJar enabled")).isTrue()
        assertThat(result.output.contains("bootJar disabled")).isFalse()
        assertThat(result.task(":aurora")?.outcome == SUCCESS).isTrue()
    }
}
