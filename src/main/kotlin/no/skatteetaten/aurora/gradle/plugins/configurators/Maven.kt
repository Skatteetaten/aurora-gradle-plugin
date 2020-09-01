package no.skatteetaten.aurora.gradle.plugins.configurators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraConfiguration
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import no.skatteetaten.aurora.gradle.plugins.mutators.MavenTools
import org.gradle.api.Project

@ExperimentalStdlibApi
class Maven(
    private val project: Project,
    private val config: AuroraConfiguration
) : Configurator {
    val tools = MavenTools(project)

    override fun configure(): List<AuroraReport> = buildList {
        tools.setDefaultTasks()

        if (config.applyMavenDeployer) {
            project.logger.lifecycle("Apply maven deployer")

            add(tools.addMavenDeployer())
        }
    }
}
