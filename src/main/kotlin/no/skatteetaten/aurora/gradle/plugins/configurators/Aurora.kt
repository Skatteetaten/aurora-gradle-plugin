package no.skatteetaten.aurora.gradle.plugins.configurators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraConfiguration
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import no.skatteetaten.aurora.gradle.plugins.mutators.AuroraTools
import org.gradle.api.Project

class Aurora(
    project: Project,
    private val config: AuroraConfiguration
) : Configurator {
    private val tools = AuroraTools(project)

    override fun configure(): List<AuroraReport> {
        val list = mutableListOf<AuroraReport>()

        if (config.applyDeliveryBundleConfig) {
            list.add(
                tools.applyDeliveryBundleConfig(
                    python = config.usePython,
                    bootJar = config.useBootJar
                )
            )
        }

        return list.toList()
    }
}
