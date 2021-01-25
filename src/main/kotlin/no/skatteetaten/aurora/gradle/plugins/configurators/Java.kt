package no.skatteetaten.aurora.gradle.plugins.configurators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraConfiguration
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import no.skatteetaten.aurora.gradle.plugins.mutators.JavaTools
import org.gradle.api.Project

class Java(
    private val project: Project,
    private val config: AuroraConfiguration
) : Configurator {
    private val tools = JavaTools(project)

    override fun configure(): List<AuroraReport> {
        val list = mutableListOf<AuroraReport>()

        if (config.applyDefaultPlugins) {
            list.add(tools.applyDefaultPlugins())
        }

        if (config.applyJavaDefaults) {
            list.add(
                tools.applyJavaDefaults(
                    compatibility = config.javaSourceCompatibility
                )
            )
        }

        project.plugins.withId("org.asciidoctor.jvm.convert") {
            list.add(tools.applyAsciiDocPlugin())
        }

        return list.toList()
    }
}
