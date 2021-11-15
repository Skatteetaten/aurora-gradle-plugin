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

class SpringTools(private val project: Project) {
    fun applySpring(
        mvcStarterVersion: String,
        webFluxStarterVersion: String,
        devTools: Boolean,
        webFluxEnabled: Boolean,
        bootJarEnabled: Boolean,
        startersEnabled: Boolean
    ): AuroraReport {
        project.logger.lifecycle("Apply Spring support")

        val implementationDependencies = mutableListOf<String>()
        implementationDependencies.add("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

        if (startersEnabled) {
            implementationDependencies.add(
                when {
                    webFluxEnabled ->
                        "no.skatteetaten.aurora.springboot:aurora-spring-boot-webflux-starter:$webFluxStarterVersion"
                    else -> "no.skatteetaten.aurora.springboot:aurora-spring-boot-mvc-starter:$mvcStarterVersion"
                }
            )
        }

        if (devTools) implementationDependencies.add("org.springframework.boot:spring-boot-devtools")

        project.logger.lifecycle("Apply Spring support")

        with(project) {
            with(plugins) {
                apply("io.spring.dependency-management")
            }

            if (!bootJarEnabled) {
                with(tasks) {
                    findByName("jar")?.let { it.enabled = true }
                    findByName("distZip")?.let { it.enabled = true }
                    findByName("bootJar")?.let { it.enabled = false }
                    findByName("distTar")?.let { it.enabled = false }
                    findByName("bootDistTar")?.let { it.enabled = false }
                    findByName("bootDistZip")?.let { it.enabled = false }
                }
            }

            disableSuperfluousArtifacts()

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
            !startersEnabled -> ", starters disabled"
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
                    it.mavenBom(springCloudDep)
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

            val stubsJar = tasks.create("stubsJar", org.gradle.jvm.tasks.Jar::class.java) { jar ->
                with(jar) {
                    archiveClassifier.set("stubs")

                    jar.into("META-INF/${project.group}/${project.name}/${project.version}/mappings") {
                        it.from("${project.buildDir}/generated-snippets/stubs")
                        it.include("**/*.*")
                    }

                    dependsOn("test")
                }
            }

            with(tasks.named("verifierStubsJar", org.gradle.jvm.tasks.Jar::class).get()) {
                enabled = false
            }

            artifacts {
                it.add("archives", stubsJar)
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
