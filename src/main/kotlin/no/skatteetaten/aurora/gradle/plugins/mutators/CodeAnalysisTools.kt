package no.skatteetaten.aurora.gradle.plugins.mutators

import info.solidsoft.gradle.pitest.PitestPluginExtension
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import org.gradle.api.Project
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.testing.jacoco.tasks.JacocoReport

class CodeAnalysisTools(private val project: Project) {
    fun applyJacocoTestReport(): AuroraReport {
        project.logger.lifecycle("Apply jacoco support")

        return when {
            project.plugins.hasPlugin("java") -> {
                with(project) {
                    plugins.apply("jacoco")

                    tasks.named("jacocoTestReport", JacocoReport::class.java) {
                        with(reports) {
                            xml.isEnabled = true
                            xml.destination = file("$buildDir/reports/jacoco/report.xml")
                            csv.isEnabled = false
                        }
                    }
                }

                AuroraReport(
                    name = "aurora.applyJacocoTestReport",
                    pluginsApplied = listOf("jacoco"),
                    description = "enable xml, disable csv report"
                )
            }
            else -> AuroraReport(
                name = "aurora.applyJacocoTestReport",
                pluginsApplied = listOf(),
                description = "java plugin not available, cannot apply jacoco"
            )
        }
    }

    fun applyPiTestSupport(): AuroraReport {
        project.logger.lifecycle("Apply pitest support")

        return when {
            project.plugins.hasPlugin("java") -> {
                with(project) {
                    with(extensions.getByName("pitest") as PitestPluginExtension) {
                        outputFormats.set(listOf("XML", "HTML"))
                    }
                }

                AuroraReport(
                    name = "plugin info.solidsoft.pitest",
                    description = "output format xml and html"
                )
            }
            else -> AuroraReport(
                name = "plugin info.solidsoft.pitest",
                pluginsApplied = listOf(),
                description = "java plugin not available, cannot apply pitest"
            )
        }
    }

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
