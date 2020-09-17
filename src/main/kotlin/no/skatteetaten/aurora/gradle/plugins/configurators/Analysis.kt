package no.skatteetaten.aurora.gradle.plugins.configurators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraConfiguration
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import no.skatteetaten.aurora.gradle.plugins.mutators.AnalysisTools
import org.gradle.api.Project

@ExperimentalStdlibApi
class Analysis(
    project: Project,
    private val config: AuroraConfiguration
) : Configurator {
    val tools = AnalysisTools(project)

    override fun configure(): List<AuroraReport> = buildList {
        if (config.applyCheckstylePlugin) {
            add(
                tools.applyCheckstylePlugin(
                    checkstyleConfigVersion = config.checkstyleConfigVersion,
                    checkstyleConfigFile = config.checkstyleConfigFile
                )
            )
        }
    }
}
