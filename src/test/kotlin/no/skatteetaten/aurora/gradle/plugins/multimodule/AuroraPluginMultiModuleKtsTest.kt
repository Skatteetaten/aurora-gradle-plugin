package no.skatteetaten.aurora.gradle.plugins.multimodule

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import no.skatteetaten.aurora.gradle.plugins.isSuccessOrEqualTo
import no.skatteetaten.aurora.gradle.plugins.taskOutcome
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import java.io.File

@Execution(CONCURRENT)
class AuroraPluginMultiModuleKtsTest {
    @TempDir
    lateinit var testProjectDir: File
    private lateinit var settingsFile: File

    @BeforeEach
    fun setup() {
        File("src/test/resources/multiplatform").copyRecursively(testProjectDir)
        settingsFile = testProjectDir.resolve("settings.gradle.kts")
        settingsFile.appendText(
            """
            dependencyResolutionManagement {
                repositories {
                    mavenCentral()
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun `build test`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("build")
            .withPluginClasspath()
            .build()

        assertThat(result.taskOutcome(":build")).isSuccessOrEqualTo()
        assertThat(result.taskOutcome(":app:build")).isSuccessOrEqualTo()
        assertThat(result.taskOutcome(":lib:build")).isSuccessOrEqualTo()
    }

    @Test
    fun `lib default should disable deliverybundle`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("build")
            .withPluginClasspath()
            .build()

        assertThat(result.task(":lib:distZip")).isNull()
    }

    @Test
    fun `app default should build deliverybundle`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("build")
            .withPluginClasspath()
            .build()

        assertThat(result.taskOutcome(":app:distZip")).isSuccessOrEqualTo()
    }

    @Test
    fun `all should upload correcty`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("build", "upload")
            .withPluginClasspath()
            .build()

        assertThat(result.taskOutcome(":upload")).isSuccessOrEqualTo()
        assertThat(result.taskOutcome(":app:upload")).isSuccessOrEqualTo()
        assertThat(result.taskOutcome(":lib:upload")).isSuccessOrEqualTo()

        val pom = testProjectDir.resolve("build/publications/leveranse/pom-default.xml")
        val pomAsText = pom.readText()

        assertThat(pomAsText.split("/dependencyManagement").size).isEqualTo(1)
        assertThat(pomAsText.contains("software.amazon.awssdk"))
        assertThat(pomAsText.contains("spring-boot-dependencies"))
    }

    @Test
    fun `root app should build deliverybundle`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("build")
            .withPluginClasspath()
            .build()

        assertThat(result.taskOutcome(":distZip")).isSuccessOrEqualTo()
    }
}
