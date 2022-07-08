package no.skatteetaten.aurora.gradle.plugins

import no.skatteetaten.aurora.gradle.plugins.configurators.Analysis
import no.skatteetaten.aurora.gradle.plugins.configurators.Aurora
import no.skatteetaten.aurora.gradle.plugins.configurators.Java
import no.skatteetaten.aurora.gradle.plugins.configurators.Kotlin
import no.skatteetaten.aurora.gradle.plugins.configurators.Maven
import no.skatteetaten.aurora.gradle.plugins.configurators.Miscellaneous
import no.skatteetaten.aurora.gradle.plugins.configurators.Spring
import no.skatteetaten.aurora.gradle.plugins.configurators.Test
import no.skatteetaten.aurora.gradle.plugins.extensions.AuroraExtension
import no.skatteetaten.aurora.gradle.plugins.extensions.Features
import no.skatteetaten.aurora.gradle.plugins.extensions.UseKotlin
import no.skatteetaten.aurora.gradle.plugins.extensions.UseSpringBoot
import no.skatteetaten.aurora.gradle.plugins.extensions.Versions
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import no.skatteetaten.aurora.gradle.plugins.model.getConfig
import org.cyclonedx.gradle.CycloneDxTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

@Suppress("unused")
@ExperimentalStdlibApi
class AuroraPlugin : Plugin<Project> {
    override fun apply(p: Project) {
        p.plugins.apply("maven-publish")
        p.registerProjectConfiguration()

        if (p.subprojects.isNotEmpty()) {
            p.subprojects {
                it.group = p.group
                it.plugins.apply("java")
                it.plugins.apply("maven-publish")
                it.registerProjectConfiguration()
            }
        }
    }

    private fun Project.registerProjectConfiguration() {
        configureExtensions()

        afterEvaluate { project ->
            val config = project.getConfig()
            val java = Java(project, config)
            val aurora = Aurora(project, config)
            val maven = Maven(project, config)
            val kotlin = Kotlin(project, config)
            val test = Test(project, config)
            val spring = Spring(project, config)
            val analysis = Analysis(project, config)
            val miscellaneous = Miscellaneous(project, config)

            val reports = listOf(
                java.configure(),
                aurora.configure(),
                kotlin.configure(),
                test.configure(),
                spring.configure(),
                analysis.configure(),
                miscellaneous.configure(),
                maven.configure(),
            ).flatten()

            with(project.tasks) {
                register("aurora") { task ->
                    with(task) {
                        project.logger.lifecycle(
                            "Use task :aurora to get full report on how AuroraPlugin modifies your gradle setup"
                        )

                        doLast {
                            printReport(reports)
                        }
                    }
                }

                register("auroraConfiguration") { task ->
                    with(task) {
                        doLast {
                            project.logger.lifecycle(config.toString())
                        }
                    }
                }

                register<CycloneDxTask>("auroraCyclonedxBom") {
                    setIncludeConfigs(listOf("runtimeClasspath"))
                    projectType = "application"
                    schemaVersion = "1.4"
                    destination = project.file("build/reports")
                    outputName = "bom"
                }
            }
        }
    }

    private fun Task.printReport(reports: List<AuroraReport>) {
        if (reports.isNotEmpty()) {
            val sortedReport = reports.sortedBy { it.name }
            logger.lifecycle("----- Aurora Plugin Report -----")
            logger.lifecycle(
                "The aurora plugin can be configured via aurora.* feature " +
                    "flags in .gradle.properties or reacting on applied plugins.\n"
            )
            logger.lifecycle(
                "Each feature can add dependencies to your build, add another " +
                    "plugin or modify configuration\n"
            )
            sortedReport.forEach { logger.lifecycle(it.toString() + "\n") }
            logger.lifecycle("--------------------------------")
        }
    }

    private fun Project.isWebFluxSetByExtension() = (extensions.getByType(AuroraExtension::class) as ExtensionAware)
        .extensions.getByType(UseSpringBoot::class).webFluxEnabled

    private fun Project.isBootJarSetByExtension() = (extensions.getByType(AuroraExtension::class) as ExtensionAware)
        .extensions.getByType(UseSpringBoot::class).bootJarEnabled
}

fun Project.configureExtensions(): AuroraExtension {
    val extension = extensions.create(
        "aurora",
        AuroraExtension::class,
        project
    )
    (extension as ExtensionAware).extensions.create(
        "useSpringBoot",
        UseSpringBoot::class,
        project
    )
    (extension as ExtensionAware).extensions.create(
        "useKotlin",
        UseKotlin::class,
        project
    )
    (extension as ExtensionAware).extensions.create(
        "versions",
        Versions::class
    )
    (extension as ExtensionAware).extensions.create(
        "features",
        Features::class
    )

    return extension
}
