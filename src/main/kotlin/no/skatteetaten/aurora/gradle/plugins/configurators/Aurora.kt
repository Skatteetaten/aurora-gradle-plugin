package no.skatteetaten.aurora.gradle.plugins.configurators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraConfiguration
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import no.skatteetaten.aurora.gradle.plugins.mutators.AuroraTools
import org.gradle.api.Project

@ExperimentalStdlibApi
class Aurora(
    project: Project,
    private val config: AuroraConfiguration
) : Configurator {
    private val tools = AuroraTools(project)

    override fun configure(): List<AuroraReport> = buildList {
        if (config.applyDeliveryBundleConfig) {
            add(
                tools.applyDeliveryBundleConfig(
                    python = config.usePython,
                    bootJar = config.useBootJar
                )
            )
        }
    }
}
