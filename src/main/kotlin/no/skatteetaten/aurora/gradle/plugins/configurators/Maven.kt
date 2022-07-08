package no.skatteetaten.aurora.gradle.plugins.configurators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraConfiguration
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import no.skatteetaten.aurora.gradle.plugins.mutators.MavenTools
import org.gradle.api.Project

class Maven(
    private val project: Project,
    private val config: AuroraConfiguration
) : Configurator {
    private val tools = MavenTools(project)

    override fun configure(): List<AuroraReport> {
        val list = mutableListOf<AuroraReport>()

        tools.setDefaultTasks()

        if (config.applyMavenDeployer) {
            project.logger.lifecycle("Apply maven deployer")

            list.add(tools.addMavenDeployer())
        }

        return list.toList()
    }
}
