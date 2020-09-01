package no.skatteetaten.aurora.gradle.plugins.configurators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraConfiguration
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import no.skatteetaten.aurora.gradle.plugins.mutators.SpringTools
import org.gradle.api.Project

@ExperimentalStdlibApi
class Spring(
    private val project: Project,
    private val config: AuroraConfiguration
) : Configurator {
    val tools = SpringTools(project)

    override fun configure(): List<AuroraReport> = buildList {
        project.plugins.withId("org.springframework.boot") {
            add(
                tools.applySpring(
                    mvcStarterVersion = config.auroraSpringBootMvcStarterVersion,
                    webFluxStarterVersion = config.auroraSpringBootWebFluxStarterVersion,
                    devTools = config.springDevTools,
                    webFluxEnabled = config.useWebFlux,
                    bootJarEnabled = config.useBootJar
                )
            )
        }

        project.plugins.withId("spring-cloud-contract") {
            add(
                tools.applySpringCloudContract(
                    junit5 = config.applyJunit5Support,
                    springCloudContractVersion = config.springCloudContractVersion
                )
            )
        }

        project.plugins.withId("org.jetbrains.kotlin.plugin.spring") {
            add(tools.applyKotlinSpringSupport())
        }
    }
}
