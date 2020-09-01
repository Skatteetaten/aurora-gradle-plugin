package no.skatteetaten.aurora.gradle.plugins.mutators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import org.gradle.api.Project
import org.gradle.api.distribution.DistributionContainer
import org.gradle.kotlin.dsl.named

class AuroraTools(private val project: Project) {
    fun applyDeliveryBundleConfig(bootJar: Boolean): AuroraReport = when {
        bootJar -> {
            with(project) {
                plugins.apply("distribution")

                with(extensions.getByName("distributions") as DistributionContainer) {
                    with(getByName("main")) {
                        contents {
                            from("${project.buildDir}/libs") {
                                into("lib")
                            }

                            from("${project.projectDir}/src/main/dist/metadata") {
                                into("metadata")
                            }
                        }
                    }
                }

                with(tasks.named("distZip", org.gradle.api.tasks.bundling.Zip::class).get()) {
                    archiveClassifier.set("Leveransepakke")
                    duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE

                    dependsOn("bootJar")
                }
            }

            AuroraReport(
                name = "aurora.applyDeliveryBundleConfig",
                pluginsApplied = listOf("distribution"),
                description = "Configure Leveransepakke for bootJar"
            )
        }
        else -> {
            with(project) {
                plugins.apply("application")

                with(tasks.named("distZip", org.gradle.api.tasks.bundling.Zip::class).get()) {
                    archiveClassifier.set("Leveransepakke")
                }

                with(tasks.getByName("startScripts")) {
                    enabled = false
                }
            }

            AuroraReport(
                name = "aurora.applyDeliveryBundleConfig",
                pluginsApplied = listOf("application"),
                description = "Configure Leveransepakke"
            )
        }
    }
}
