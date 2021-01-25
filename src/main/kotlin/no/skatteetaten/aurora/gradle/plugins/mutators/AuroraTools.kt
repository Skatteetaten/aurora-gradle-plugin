package no.skatteetaten.aurora.gradle.plugins.mutators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import org.gradle.api.Project
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.tasks.bundling.Tar
import org.gradle.kotlin.dsl.named

class AuroraTools(private val project: Project) {
    fun applyDeliveryBundleConfig(bootJar: Boolean): AuroraReport = when {
        bootJar -> {
            project.logger.lifecycle("Apply bootjar delivery bundle")

            with(project) {
                plugins.apply("distribution")

                with(extensions.getByName("distributions") as DistributionContainer) {
                    with(getByName("main")) {
                        with(contents) {
                            from("${project.buildDir}/libs") {
                                it.into("lib")
                            }

                            from("${project.projectDir}/src/main/dist/metadata") {
                                it.into("metadata")
                            }
                        }
                    }
                }

                with(tasks.named("distZip", org.gradle.api.tasks.bundling.Zip::class).get()) {
                    archiveClassifier.set("Leveransepakke")
                    duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE

                    dependsOn("bootJar")
                }

                disableSuperfluousArtifacts()
            }

            AuroraReport(
                name = "aurora.applyDeliveryBundleConfig",
                pluginsApplied = listOf("distribution"),
                description = "Configure Leveransepakke for bootJar"
            )
        }
        else -> {
            project.logger.lifecycle("Apply standard delivery bundle")

            with(project) {
                plugins.apply("application")

                with(tasks.named("distZip", org.gradle.api.tasks.bundling.Zip::class).get()) {
                    archiveClassifier.set("Leveransepakke")

                    dependsOn("jar")
                }

                disableSuperfluousArtifacts()

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

fun Project.disableSuperfluousArtifacts() {
    with(tasks.named("distTar", Tar::class).get()) {
        enabled = false
    }

    configurations.getByName("archives").artifacts.removeIf {
        it.extension == "tar" || it.name.endsWith("boot")
    }
}
