package no.skatteetaten.aurora.gradle.plugins.configurators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraConfiguration
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import no.skatteetaten.aurora.gradle.plugins.mutators.TestTools
import org.gradle.api.Project

class Test(
    private val project: Project,
    private val config: AuroraConfiguration
) : Configurator {
    private val tools = TestTools(project)

    override fun configure(): List<AuroraReport> {
        val list = mutableListOf<AuroraReport>()

        if (config.applyJunit5Support) {
            list.add(tools.applyJunit5(config.junit5Version))
        }

        if (config.applyJacocoTestReport) {
            list.add(tools.applyJacocoTestReport(config.jacocoToolsVersion))
        }

        project.plugins.withId("info.solidsoft.pitest") {
            list.add(tools.applyPiTestSupport())
        }

        if (config.applySpockSupport) {
            project.logger.lifecycle("SPOCK support")

            list.add(
                tools.applySpockSupport(
                    groovyVersion = config.groovyVersion,
                    spockVersion = config.spockVersion,
                    cglibVersion = config.cglibVersion,
                    objenesisVersion = config.objenesisVersion
                )
            )
        }

        return list.toList()
    }
}
