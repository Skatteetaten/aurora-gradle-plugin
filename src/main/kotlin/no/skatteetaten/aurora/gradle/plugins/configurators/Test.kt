package no.skatteetaten.aurora.gradle.plugins.configurators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraConfiguration
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import no.skatteetaten.aurora.gradle.plugins.mutators.TestTools
import org.gradle.api.Project

@ExperimentalStdlibApi
class Test(
    private val project: Project,
    private val config: AuroraConfiguration
) : Configurator {
    private val tools = TestTools(project)

    override fun configure(): List<AuroraReport> = buildList {
        if (config.applyJunit5Support) {
            add(tools.applyJunit5(config.junit5Version))
        }

        if (config.applyJacocoTestReport) {
            add(tools.applyJacocoTestReport())
        }

        project.plugins.withId("info.solidsoft.pitest") {
            add(tools.applyPiTestSupport())
        }

        if (config.applySpockSupport) {
            project.logger.lifecycle("SPOCK support")

            add(
                tools.applySpockSupport(
                    groovyVersion = config.groovyVersion,
                    spockVersion = config.spockVersion,
                    cglibVersion = config.cglibVersion,
                    objenesisVersion = config.objenesisVersion
                )
            )
        }
    }
}
