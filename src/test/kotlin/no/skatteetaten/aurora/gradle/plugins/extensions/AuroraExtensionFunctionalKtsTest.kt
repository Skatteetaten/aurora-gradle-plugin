package no.skatteetaten.aurora.gradle.plugins.extensions

import assertk.assertThat
import assertk.assertions.contains
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
class AuroraExtensionFunctionalKtsTest {
    @TempDir
    lateinit var testProjectDir: File
    private lateinit var buildFile: File

    @BeforeEach
    fun setup() {
        buildFile = testProjectDir.resolve("build.gradle.kts")
        buildFile.createNewFile()
        buildFile.writeText(
            """
            plugins {
                id("no.skatteetaten.gradle.aurora")
            }
        """
        )
    }

    @Test
    fun `build test`() {
        buildFile.appendText(
            """
            aurora {}
        """
        )
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("build")
            .withPluginClasspath()
            .build()

        assertThat(result.task(":build")?.outcome == SUCCESS).isTrue()
    }

    @Test
    fun `build test with kotlin, spring-boot and webflux support`() {
        buildFile.appendText(
            """
            repositories {
                mavenCentral()
            }
            
            aurora {
                useKotlin()
                useSpringBoot {
                    useWebFlux()
                }
            }
        """
        )
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("build")
            .withPluginClasspath()
            .build()

        assertThat(result.output).contains("Apply Spring support")
        assertThat(result.output).contains("Apply spring kotlin support")
        assertThat(result.output).contains("Apply kotlin support")
        assertThat(result.task(":build")?.outcome == SUCCESS).isTrue()
    }

    @Test
    fun `build test with kotlin, spring-boot and webflux support flipped`() {
        buildFile.appendText(
            """
            repositories {
                mavenCentral()
            }
            
            aurora {
                useSpringBoot {
                    useWebFlux()
                }
                useKotlin()
            }
        """
        )
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("build")
            .withPluginClasspath()
            .build()

        assertThat(result.output).contains("Apply Spring support")
        assertThat(result.output).contains("Apply spring kotlin support")
        assertThat(result.output).contains("Apply kotlin support")
        assertThat(result.task(":build")?.outcome == SUCCESS).isTrue()
    }

    @Test
    fun `build test with kotlin with everything added`() {
        buildFile.appendText(
            """
            repositories {
                mavenCentral()
            }
            
            aurora { 
                useAsciiDoctor
                usePitest
                useVersions
                useKotlin {
                    useKtLint
                }
                useSpringBoot {
                    useWebFlux
                    useCloudContract
                }
            }
            """.trimMargin()
        )
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("ktlintKotlinScriptFormat", "build")
            .forwardOutput()
            .withPluginClasspath()
            .build()

        assertThat(result.output).contains("Apply Spring support")
        assertThat(result.output).contains("Apply spring kotlin support")
        assertThat(result.output).contains("Apply kotlin support")
        assertThat(result.output).contains("Apply ktlint support")
        assertThat(result.output).contains("Apply pitest support")
        assertThat(result.output).contains("Apply versions support")
        assertThat(result.output).contains("Apply asciiDoctor support")
        assertThat(result.output).contains("Apply spring-cloud-contract support")
        assertThat(result.task(":build")?.outcome == SUCCESS).isTrue()
    }

    @Test
    fun `build test with aurora defaults`() {
        buildFile.appendText(
            """
            repositories {
                mavenCentral()
            }
            
            aurora {
                useAuroraDefaults
            }
            """.trimMargin()
        )
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("ktlintKotlinScriptFormat", "build")
            .forwardOutput()
            .withPluginClasspath()
            .build()

        assertThat(result.output).contains("Apply Spring support")
        assertThat(result.output).contains("Apply spring kotlin support")
        assertThat(result.output).contains("Apply kotlin support")
        assertThat(result.output).contains("Apply ktlint support")
        assertThat(result.output).contains("Apply versions support")
        assertThat(result.output).contains("Apply spring-cloud-contract support")
        assertThat(result.task(":build")?.outcome == SUCCESS).isTrue()
    }

    @Test
    fun `build test with kotlin, spring-boot and webflux support report`() {
        buildFile.appendText(
            """
            repositories {
                mavenCentral()
            }
            
            aurora {
                useKotlin()
                useSpringBoot {
                    useWebFlux()
                }
            }
        """
        )
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
    fun `build test with kotlin, spring-boot support report`() {
        buildFile.appendText(
            """
            repositories {
                mavenCentral()
            }
            
            aurora {
                useKotlin()
                useSpringBoot()
            }
        """
        )
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("aurora")
            .withPluginClasspath()
            .build()

        assertThat(result.output.contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-mvc-starter")).isTrue()
        assertThat(result.output.contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-webflux-starter")).isFalse()
        assertThat(result.output.contains("webflux enabled and webmvc + tomcat excluded")).isFalse()
        assertThat(result.task(":aurora")?.outcome == SUCCESS).isTrue()
    }

    @Test
    fun `build test with kotlin, spring-boot support report shorthand`() {
        buildFile.appendText(
            """
            repositories {
                mavenCentral()
            }
            
            aurora {
                useKotlin()
                useSpringBoot {}
            }
        """
        )
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("aurora")
            .withPluginClasspath()
            .build()

        assertThat(result.output.contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-mvc-starter")).isTrue()
        assertThat(result.output.contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-webflux-starter")).isFalse()
        assertThat(result.output.contains("webflux enabled and webmvc + tomcat excluded")).isFalse()
        assertThat(result.task(":aurora")?.outcome == SUCCESS).isTrue()
    }

    @Test
    fun `build test with kotlin, spring-boot support report field shorthand`() {
        buildFile.appendText(
            """
            repositories {
                mavenCentral()
            }
            
            aurora {
                useKotlin
                useSpringBoot
            }
        """
        )
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("aurora")
            .withPluginClasspath()
            .build()

        assertThat(result.output.contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-mvc-starter")).isTrue()
        assertThat(result.output.contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-webflux-starter")).isFalse()
        assertThat(result.output.contains("webflux enabled and webmvc + tomcat excluded")).isFalse()
        assertThat(result.task(":aurora")?.outcome == SUCCESS).isTrue()
    }

    @Test
    fun `build test with kotlin, spring-boot and webflux support report field shorthand`() {
        buildFile.appendText(
            """
            repositories {
                mavenCentral()
            }
            
            aurora {
                useKotlin
                useSpringBoot {
                    useWebFlux
                }
            }
        """
        )
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
    fun sunshineTest() {
        buildFile.appendText(
            """
            aurora {}
        """
        )
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("aurora")
            .withPluginClasspath()
            .build()

        assertThat(result.output.contains("----- Aurora Plugin Report -----")).isTrue()
        assertThat(result.task(":aurora")?.outcome == SUCCESS).isTrue()
    }
}
