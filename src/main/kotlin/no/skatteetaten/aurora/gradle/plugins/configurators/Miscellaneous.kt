@file:Suppress("unused")

package no.skatteetaten.aurora.gradle.plugins.configurators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraConfiguration
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import no.skatteetaten.aurora.gradle.plugins.mutators.MiscellaneousTools
import org.gradle.api.Project

@ExperimentalStdlibApi
class Miscellaneous(
    private val project: Project,
    private val config: AuroraConfiguration
) : Configurator {
    private val tools = MiscellaneousTools(project)

    override fun configure(): List<AuroraReport> = buildList {
        project.plugins.withId("com.github.ben-manes.versions") {
            add(tools.applyVersions())
        }
    }
}
