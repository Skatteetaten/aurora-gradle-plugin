package no.skatteetaten.aurora.gradle.plugins.mutators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import org.gradle.api.Project
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.kotlin.dsl.configure

class AnalysisTools(private val project: Project) {
    /**
     * Create a new configuration, "auroraCheckstyleConfig", and then add the Aurora Checkstyle configuration dependency
     * to this configuration.
     * Apply the Checkstyle plugin and assign the specified configuration file from Aurora Checkstyle configuration
     * archive.
     *
     * @param checkstyleConfigVersion
     * @param checkstyleConfigFile
     */
    fun applyCheckstylePlugin(
        checkstyleConfigVersion: String,
        checkstyleConfigFile: String
    ): AuroraReport = when {
        project.plugins.hasPlugin("java") -> {
            project.logger.lifecycle("Apply checkstyle support")

            val auroraCheckstyleConfig = project.configurations.create("auroraCheckstyleConfig")
            val dep = project.dependencies.create(
                "no.skatteetaten.aurora.checkstyle:checkstyle-config:$checkstyleConfigVersion"
            )

            auroraCheckstyleConfig.dependencies.add(dep)

            with(project) {
                with(plugins) {
                    apply("checkstyle")
                }

                configure<CheckstyleExtension> {
                    config = project.resources.text.fromArchiveEntry(auroraCheckstyleConfig, checkstyleConfigFile)
                    isIgnoreFailures = true
                }
            }

            AuroraReport(
                name = "aurora.applyCheckstylePlugin",
                dependenciesAdded = listOf(
                    "implementation no.skatteetaten.aurora.checkstyle:checkstyle-config:$checkstyleConfigVersion"
                ),
                description = "with file $checkstyleConfigFile"
            )
        }
        else -> AuroraReport(
            name = "aurora.applyCheckstylePlugin",
            pluginsApplied = listOf(),
            description = "java plugin not available, will not apply checkstyle"
        )
    }
}
