package no.skatteetaten.aurora.gradle.plugins.extensions

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import no.skatteetaten.aurora.gradle.plugins.model.AuroraConfiguration
import no.skatteetaten.aurora.gradle.plugins.taskStatus
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import java.io.File

@Execution(CONCURRENT)
class AuroraExtensionFunctionalTest {
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
            }
            
            repositories {
                mavenCentral()
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

        result.taskStatus()
    }

    @Test
    fun `build test with kotlin, spring-boot and webflux support`() {
        buildFile.appendText(
            """
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
        result.taskStatus()
    }

    @Test
    fun `build test with kotlin, spring-boot and webflux support flipped`() {
        buildFile.appendText(
            """
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
        result.taskStatus()
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
        result.taskStatus()
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
            .withPluginClasspath()
            .build()

        assertThat(result.output).contains("Apply Spring support")
        assertThat(result.output).contains("Apply spring kotlin support")
        assertThat(result.output).contains("Apply kotlin support")
        assertThat(result.output).contains("Apply ktlint support")
        assertThat(result.output).contains("Apply versions support")
        assertThat(result.output).contains("Apply spring-cloud-contract support")
        result.taskStatus()
    }

    @Test
    fun `build test with kotlin, spring-boot and webflux support report`() {
        buildFile.appendText(
            """
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

        assertThat(result.output).doesNotContain("no.skatteetaten.aurora.springboot:aurora-spring-boot-mvc-starter")
        assertThat(result.output).contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-webflux-starter")
        assertThat(result.output).contains("webflux enabled and webmvc + tomcat excluded")
        result.taskStatus(taskName = ":aurora")
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

        assertThat(result.output).contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-mvc-starter")
        assertThat(result.output).doesNotContain("no.skatteetaten.aurora.springboot:aurora-spring-boot-webflux-starter")
        assertThat(result.output).doesNotContain("webflux enabled and webmvc + tomcat excluded")
        result.taskStatus(taskName = ":aurora")
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

        assertThat(result.output).contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-mvc-starter")
        assertThat(result.output).doesNotContain("no.skatteetaten.aurora.springboot:aurora-spring-boot-webflux-starter")
        assertThat(result.output).doesNotContain("webflux enabled and webmvc + tomcat excluded")
        result.taskStatus(taskName = ":aurora")
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

        assertThat(result.output).contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-mvc-starter")
        assertThat(result.output).doesNotContain("no.skatteetaten.aurora.springboot:aurora-spring-boot-webflux-starter")
        assertThat(result.output).doesNotContain("webflux enabled and webmvc + tomcat excluded")
        result.taskStatus(taskName = ":aurora")
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

        assertThat(result.output).doesNotContain("no.skatteetaten.aurora.springboot:aurora-spring-boot-mvc-starter")
        assertThat(result.output).contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-webflux-starter")
        assertThat(result.output).contains("webflux enabled and webmvc + tomcat excluded")
        result.taskStatus(taskName = ":aurora")
    }

    @Test
    fun `configuration overload is respected`() {
        buildFile.appendText(
            """
            aurora {
                useSpringBoot
                versions {
                    auroraSpringBootMvcStarter = "1.0.7"
                }
                features {
                    spock = true
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
        assertThat(result.output).contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-mvc-starter:1.0.7")
        assertThat(result.output).contains("testImplementation org.codehaus.groovy:groovy-all:3.0.5")
        result.taskStatus(taskName = ":aurora")
    }

    @Test
    fun `output computed config`() {
        buildFile.appendText(
            """
            aurora {}
        """
        )
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("auroraConfiguration")
            .withPluginClasspath()
            .build()

        assertThat(result.output).contains(AuroraConfiguration().toString())
        result.taskStatus(taskName = ":auroraConfiguration")
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

        assertThat(result.output).contains("----- Aurora Plugin Report -----")
        assertThat(result.output).contains("Use task :aurora to get full report on how AuroraPlugin modifies your gradle setup")
        result.taskStatus(taskName = ":aurora")
    }
}
