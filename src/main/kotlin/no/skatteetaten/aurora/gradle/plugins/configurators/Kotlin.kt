package no.skatteetaten.aurora.gradle.plugins.configurators

import no.skatteetaten.aurora.gradle.plugins.extensions.isKotlinStdlibEnabled
import no.skatteetaten.aurora.gradle.plugins.model.AuroraConfiguration
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import no.skatteetaten.aurora.gradle.plugins.mutators.KotlinTools
import org.gradle.api.Project

class Kotlin(
    private val project: Project,
    private val config: AuroraConfiguration
) : Configurator {
    private val tools = KotlinTools(project)

    override fun configure(): List<AuroraReport> {
        val list = mutableListOf<AuroraReport>()

        if (project.isKotlinStdlibEnabled()) {
            project.plugins.withId("org.jetbrains.kotlin.jvm") {
                list.add(
                    tools.applyKotlinSupport(
                        kotlinLoggingVersion = config.kotlinLoggingVersion,
                        sourceCompatibility = config.javaSourceCompatibility
                    )
                )
            }

            project.plugins.withId("org.jlleitschuh.gradle.ktlint") {
                list.add(tools.applyKtLint())
            }
        } else {
            project.logger.lifecycle("kotlin.stdlib.default.dependency set, kotlin-stdlib excluded")
        }

        return list.toList()
    }
}
