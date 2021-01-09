@file:Suppress("unused")

package no.skatteetaten.aurora.gradle.plugins.configurators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraConfiguration
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import no.skatteetaten.aurora.gradle.plugins.mutators.MiscellaneousTools
import org.gradle.api.Project

@OptIn(ExperimentalStdlibApi::class)
class Miscellaneous(
    private val project: Project,
    private val config: AuroraConfiguration
) : Configurator {
    private val tools = MiscellaneousTools(project)

    override fun configure(): List<AuroraReport> {
        val list = mutableListOf<AuroraReport>()

        project.plugins.withId("com.github.ben-manes.versions") {
            list.add(tools.applyVersions())
        }

        return list.toList()
    }
}
