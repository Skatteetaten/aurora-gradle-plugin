package no.skatteetaten.aurora.gradle.plugins.extensions

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import no.skatteetaten.aurora.gradle.plugins.isSuccessOrEqualTo
import no.skatteetaten.aurora.gradle.plugins.model.AuroraConfiguration
import no.skatteetaten.aurora.gradle.plugins.taskOutcome
import org.gradle.testkit.runner.GradleRunner
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

        assertThat(result.taskOutcome()).isSuccessOrEqualTo()
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
    
                versions {
                    auroraSpringBootWebFluxStarter = "1.2.+"
                    auroraSpringBootMvcStarter = "1.2.+"
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
        assertThat(result.taskOutcome()).isSuccessOrEqualTo()
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
    
                versions {
                    auroraSpringBootWebFluxStarter = "1.2.+"
                    auroraSpringBootMvcStarter = "1.2.+"
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
        assertThat(result.taskOutcome()).isSuccessOrEqualTo()
    }

    @Test
    fun `build test with kotlin with everything added`() {
        buildFile.writeText(
            """
            import com.adarshr.gradle.testlogger.TestLoggerExtension
            import com.adarshr.gradle.testlogger.theme.ThemeType
            
            plugins {
                id("no.skatteetaten.gradle.aurora")
            }
            
            repositories {
                mavenCentral()
            }
            
            aurora { 
                useAsciiDoctor
                usePitest
                useVersions
                useGradleLogger
                useKotlin {
                    useKtLint
                }
                useSpringBoot {
                    useWebFlux
                    useCloudContract
                }
    
                versions {
                    auroraSpringBootWebFluxStarter = "1.2.+"
                    auroraSpringBootMvcStarter = "1.2.+"
                }
            }
            
            configure<TestLoggerExtension> {
                theme = ThemeType.STANDARD_PARALLEL
                showStandardStreams = true
                showPassedStandardStreams = false
                showSkippedStandardStreams = false
                showFailedStandardStreams = true
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
        assertThat(result.output).contains("Apply ktlint support")
        assertThat(result.output).contains("Apply pitest support")
        assertThat(result.output).contains("Apply versions support")
        assertThat(result.output).contains("Apply asciiDoctor support")
        assertThat(result.output).contains("Apply spring-cloud-contract support")
        assertThat(result.taskOutcome()).isSuccessOrEqualTo()
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
    
                versions {
                    auroraSpringBootWebFluxStarter = "1.2.+"
                    auroraSpringBootMvcStarter = "1.2.+"
                }
            }
            """.trimMargin()
        )
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("build")
            .withPluginClasspath()
            .build()

        assertThat(result.output).contains("Apply Spring support")
        assertThat(result.output).contains("Apply versions support")
        assertThat(result.output).contains("Cannot apply Gorylenko Git Properties! No .git Directory!")
        assertThat(result.taskOutcome()).isSuccessOrEqualTo()
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
    
                versions {
                    auroraSpringBootWebFluxStarter = "1.2.+"
                    auroraSpringBootMvcStarter = "1.2.+"
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
        assertThat(result.taskOutcome(taskName = ":aurora")).isSuccessOrEqualTo()
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
    
                versions {
                    auroraSpringBootWebFluxStarter = "1.2.+"
                    auroraSpringBootMvcStarter = "1.2.+"
                }
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
        assertThat(result.taskOutcome(taskName = ":aurora")).isSuccessOrEqualTo()
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
    
                versions {
                    auroraSpringBootWebFluxStarter = "1.2.+"
                    auroraSpringBootMvcStarter = "1.2.+"
                }
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
        assertThat(result.taskOutcome(taskName = ":aurora")).isSuccessOrEqualTo()
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
    
                versions {
                    auroraSpringBootWebFluxStarter = "1.2.+"
                    auroraSpringBootMvcStarter = "1.2.+"
                }
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
        assertThat(result.taskOutcome(taskName = ":aurora")).isSuccessOrEqualTo()
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
    
                versions {
                    auroraSpringBootWebFluxStarter = "1.2.+"
                    auroraSpringBootMvcStarter = "1.2.+"
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
        assertThat(result.taskOutcome(taskName = ":aurora")).isSuccessOrEqualTo()
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
        assertThat(result.output).contains("testImplementation org.codehaus.groovy:groovy-all:3.0.8")
        assertThat(result.taskOutcome(taskName = ":aurora")).isSuccessOrEqualTo()
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
        assertThat(result.taskOutcome(taskName = ":auroraConfiguration")).isSuccessOrEqualTo()
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
        assertThat(result.taskOutcome(taskName = ":aurora")).isSuccessOrEqualTo()
    }

    @Test
    fun `custom implementation function`() {
        buildFile.writeText(
            """
                import no.skatteetaten.aurora.gradle.plugins.extensions.implementationTransitiveNexusIQIssue
                
                plugins {
                    java
                    id("no.skatteetaten.gradle.aurora")
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementationTransitiveNexusIQIssue("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.20")
                }
            """
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("build")
            .withPluginClasspath()
            .build()

        assertThat(result.output).contains("Nexus IQ transitive dependency issue")
        assertThat(result.taskOutcome()).isSuccessOrEqualTo()
    }

    @Test
    fun `dependency updates report`() {
        buildFile.writeText(
            """
                plugins {
                    java
                    id("no.skatteetaten.gradle.aurora")
                }
                
                aurora {
                    useKotlinDefaults
                }
            """
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("dependencyUpdates")
            .withPluginClasspath()
            .build()

        assertThat(result.output).contains("Project Dependency Updates ")
        assertThat(result.output).contains("Failed to determine the latest version for the following dependencies")
        assertThat(result.taskOutcome(":dependencyUpdates")).isSuccessOrEqualTo()
    }
}
