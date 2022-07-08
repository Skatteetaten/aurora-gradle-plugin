package no.skatteetaten.aurora.gradle.plugins.configurators

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

        val stdlib: String? = project.properties["kotlin.stdlib.default.dependency"] as String?
        if (stdlib == "false") {
            project.logger.lifecycle("kotlin.stdlib.default.dependency set, kotlin-stdlib excluded")
        } else {
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
        }

        return list.toList()
    }
}
