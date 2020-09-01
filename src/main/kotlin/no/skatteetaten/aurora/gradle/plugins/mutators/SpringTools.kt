package no.skatteetaten.aurora.gradle.plugins.mutators

import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import org.gradle.api.Project
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.named
import org.springframework.boot.gradle.dsl.SpringBootExtension
import org.springframework.cloud.contract.verifier.config.TestFramework.JUNIT5
import org.springframework.cloud.contract.verifier.config.TestFramework.SPOCK
import org.springframework.cloud.contract.verifier.plugin.ContractVerifierExtension

@ExperimentalStdlibApi
class SpringTools(private val project: Project) {
    fun applySpring(
        mvcStarterVersion: String,
        webFluxStarterVersion: String,
        devTools: Boolean,
        webFluxEnabled: Boolean,
        bootJarEnabled: Boolean
    ): AuroraReport {
        val implementationDependencies = buildList {
            add("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

            add(
                when {
                    webFluxEnabled ->
                        "no.skatteetaten.aurora.springboot:aurora-spring-boot-webflux-starter:$webFluxStarterVersion"
                    else -> "no.skatteetaten.aurora.springboot:aurora-spring-boot-mvc-starter:$mvcStarterVersion"
                }
            )

            if (devTools) add("org.springframework.boot:spring-boot-devtools")
        }

        project.logger.lifecycle("Apply Spring support")

        with(project) {
            with(plugins) {
                apply("io.spring.dependency-management")
            }

            if (!bootJarEnabled) {
                with(tasks) {
                    getByName("jar") { enabled = true }
                    getByName("distZip") { enabled = true }
                    getByName("bootJar") { enabled = false }
                    getByName("distTar") { enabled = false }
                    getByName("bootDistTar") { enabled = false }
                    getByName("bootDistZip") { enabled = false }
                }
            }

            if (webFluxEnabled) {
                with(project) {
                    with(configurations) {
                        with(getByName("implementation")) {
                            exclude("org.springframework", "spring-webmvc")
                            exclude("org.springframework.boot", "spring-boot-starter-tomcat")
                        }
                    }
                }
            }

            with(extensions.getByName("springBoot") as SpringBootExtension) {
                buildInfo()
            }

            with(dependencies) {
                implementationDependencies.forEach { add("implementation", it) }
            }
        }

        val resolvedBootJarText = when {
            bootJarEnabled -> ", bootJar enabled"
            else -> ", bootJar disabled"
        }
        val resolvedWebFluxText = when {
            webFluxEnabled -> ", webflux enabled and webmvc + tomcat excluded"
            else -> ", webFlux disabled"
        }

        return AuroraReport(
            name = "plugin org.springframework.boot",
            dependenciesAdded = implementationDependencies.map {
                "implementation $it"
            },
            description = "Build info$resolvedBootJarText$resolvedWebFluxText, Optional devtools",
            pluginsApplied = listOf("io.spring.dependency-management")
        )
    }

    fun applySpringCloudContract(
        junit5: Boolean,
        springCloudContractVersion: String
    ): AuroraReport {
        project.logger.lifecycle("Apply spring-cloud-contract support")

        val testDependencies = listOf(
            "org.springframework.cloud:spring-cloud-starter-contract-stub-runner",
            "org.springframework.cloud:spring-cloud-starter-contract-verifier",
            "org.springframework.cloud:spring-cloud-contract-wiremock",
            "org.springframework.restdocs:spring-restdocs-mockmvc"
        )

        with(project) {
            val springCloudDep =
                "org.springframework.cloud:spring-cloud-contract-dependencies:$springCloudContractVersion"

            with(extensions.getByName("dependencyManagement") as DependencyManagementExtension) {
                imports {
                    mavenBom(springCloudDep)
                }
            }

            val testFrameworkToSet = when {
                junit5 -> JUNIT5
                else -> SPOCK
            }

            with(extensions.getByName("contracts") as ContractVerifierExtension) {
                packageWithBaseClasses.set("$group.${project.name}.contracts")
                failOnNoContracts.set(false)
                testFramework.set(testFrameworkToSet)
            }

            with(dependencies) {
                testDependencies.forEach { add("testImplementation", it) }
            }

            val stubsJar = tasks.create("stubsJar", org.gradle.jvm.tasks.Jar::class.java) {
                archiveClassifier.set("stubs")

                into("META-INF/${project.group}/${project.name}/${project.version}/mappings") {
                    include("**/*.*")
                    from("${project.buildDir}/generated-snippets/stubs")
                }

                dependsOn("test")
            }

            with(tasks.named("verifierStubsJar", org.gradle.jvm.tasks.Jar::class).get()) {
                enabled = false
            }

            artifacts {
                add("archives", stubsJar)
            }
        }

        return AuroraReport(
            name = "plugin spring-cloud-contract",
            dependenciesAdded = testDependencies.map {
                "testImplementation $it"
            } + "bom org.springframework.cloud:spring-cloud-contract-dependencies:$springCloudContractVersion",
            description = "Configure stubs, testframework"
        )
    }

    fun applyKotlinSpringSupport(): AuroraReport {
        val implementationDependencies = listOf("com.fasterxml.jackson.module:jackson-module-kotlin")

        project.logger.lifecycle("Apply spring kotlin support")

        with(project) {
            with(dependencies) {
                implementationDependencies.forEach { add("implementation", it) }
            }
        }

        return AuroraReport(
            name = "plugin org.jetbrains.kotlin.plugin.spring",
            dependenciesAdded = implementationDependencies.map {
                "implementation $it"
            }
        )
    }
}
