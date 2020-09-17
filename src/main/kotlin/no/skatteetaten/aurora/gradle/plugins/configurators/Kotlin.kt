package no.skatteetaten.aurora.gradle.plugins.configurators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraConfiguration
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import no.skatteetaten.aurora.gradle.plugins.mutators.KotlinTools
import org.gradle.api.Project

@ExperimentalStdlibApi
class Kotlin(
    private val project: Project,
    private val config: AuroraConfiguration
) : Configurator {
    private val tools = KotlinTools(project)

    override fun configure(): List<AuroraReport> = buildList {
        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            add(
                tools.applyKotlinSupport(
                    kotlinLoggingVersion = config.kotlinLoggingVersion
                )
            )
        }

        project.plugins.withId("org.jlleitschuh.gradle.ktlint") {
            add(tools.applyKtLint())
        }
    }
}
