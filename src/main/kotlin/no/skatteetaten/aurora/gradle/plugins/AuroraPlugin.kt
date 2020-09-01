package no.skatteetaten.aurora.gradle.plugins

import no.skatteetaten.aurora.gradle.plugins.extensions.AuroraExtension
import no.skatteetaten.aurora.gradle.plugins.extensions.FeaturesConfiguration
import no.skatteetaten.aurora.gradle.plugins.extensions.UseKotlin
import no.skatteetaten.aurora.gradle.plugins.extensions.UseSpringBoot
import no.skatteetaten.aurora.gradle.plugins.extensions.VersionsConfiguration
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import no.skatteetaten.aurora.gradle.plugins.model.getConfig
import no.skatteetaten.aurora.gradle.plugins.mutators.CodeAnalysisTools
import no.skatteetaten.aurora.gradle.plugins.mutators.JavaApplicationTools
import no.skatteetaten.aurora.gradle.plugins.mutators.MavenTools
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType

@Suppress("unused")
@ExperimentalStdlibApi
class AuroraPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.configureExtensions()
        project.afterEvaluate {
            logger.lifecycle("After evaluate")

            val config = project.getConfig()
            val reports: MutableList<AuroraReport> = mutableListOf()
            val mavenTools = MavenTools(project)
            val tools = CodeAnalysisTools(project)
            val java = JavaApplicationTools(project)

            mavenTools.setDefaultTasks()

            if (config.applyDefaultPlugins) {
                reports.add(java.applyDefaultPlugins())
            }

            if (config.applyJavaDefaults) {
                reports.add(
                    java.applyJavaDefaults(
                        compatibility = config.javaSourceCompatibility
                    )
                )
            }

            plugins.withId("org.asciidoctor.convert") {
                reports.add(java.applyAsciiDocPlugin())
            }

            plugins.withId("com.github.ben-manes.versions") {
                reports.add(java.applyVersions())
            }

            if (config.applyDeliveryBundleConfig) {
                reports.add(
                    java.applyDeliveryBundleConfig(
                        bootJar = config.useBootJar
                    )
                )
            }

            plugins.withId("org.springframework.boot") {
                reports.add(
                    java.applySpring(
                        mvcStarterVersion = config.auroraSpringBootMvcStarterVersion,
                        webFluxStarterVersion = config.auroraSpringBootWebFluxStarterVersion,
                        devTools = config.springDevTools,
                        webFluxEnabled = config.useWebFlux,
                        bootJarEnabled = config.useBootJar
                    )
                )
            }

            if (config.applyJunit5Support) {
                reports.add(java.applyJunit5(config.junit5Version))
            }

            plugins.withId("org.jetbrains.kotlin.jvm") {
                reports.add(
                    java.applyKotlinSupport(
                        kotlinLoggingVersion = config.kotlinLoggingVersion
                    )
                )
            }

            plugins.withId("org.jetbrains.kotlin.plugin.spring") {
                reports.add(java.applyKotlinSpringSupport())
            }

            if (config.applyCheckstylePlugin) {
                reports.add(
                    tools.applyCheckstylePlugin(
                        checkstyleConfigVersion = config.checkstyleConfigVersion,
                        checkstyleConfigFile = config.checkstyleConfigFile
                    )
                )
            }

            if (config.applyJacocoTestReport) {
                reports.add(tools.applyJacocoTestReport())
            }

            plugins.withId("info.solidsoft.pitest") {
                reports.add(tools.applyPiTestSupport())
            }

            if (config.applyMavenDeployer) {
                logger.lifecycle("Apply maven deployer")

                reports.add(mavenTools.addMavenDeployer())
            }

            plugins.withId("spring-cloud-contract") {
                reports.add(
                    java.applySpringCloudContract(
                        junit5 = config.applyJunit5Support,
                        springCloudContractVersion = config.springCloudContractVersion
                    )
                )
            }

            plugins.withId("org.jlleitschuh.gradle.ktlint") {
                reports.add(java.applyKtLint())
            }

            if (config.applySpockSupport) {
                logger.lifecycle("SPOCK support")

                reports.add(
                    java.applySpockSupport(
                        groovyVersion = config.groovyVersion,
                        spockVersion = config.spockVersion,
                        cglibVersion = config.cglibVersion,
                        objenesisVersion = config.objenesisVersion
                    )
                )
            }

            tasks.register("aurora") {
                doLast {
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
            }

            tasks.register("auroraConfiguration") {
                doLast {
                    logger.lifecycle(config.toString())
                }
            }

            logger.lifecycle("Use task :aurora to get full report on how AuroraPlugin modify your gradle setup")
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
        VersionsConfiguration::class
    )
    (extension as ExtensionAware).extensions.create(
        "features",
        FeaturesConfiguration::class
    )

    return extension
}
