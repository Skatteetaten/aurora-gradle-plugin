package no.skatteetaten.aurora.gradle.plugins.configurators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraConfiguration
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import no.skatteetaten.aurora.gradle.plugins.mutators.AnalysisTools
import org.gradle.api.Project

class Analysis(
    project: Project,
    private val config: AuroraConfiguration
) : Configurator {
    val tools = AnalysisTools(project)

    override fun configure(): List<AuroraReport> {
        val list = mutableListOf<AuroraReport>()

        if (config.applyCheckstylePlugin) {
            list.add(
                tools.applyCheckstylePlugin(
                    checkstyleConfigVersion = config.checkstyleConfigVersion,
                    checkstyleConfigFile = config.checkstyleConfigFile
                )
            )
        }

        return list.toList()
    }
}
