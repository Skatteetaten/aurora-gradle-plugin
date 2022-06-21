@file:Suppress("DEPRECATION", "SENSELESS_NULL_IN_WHEN", "KotlinConstantConditions")

package no.skatteetaten.aurora.gradle.plugins.mutators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import org.asciidoctor.gradle.jvm.AsciidoctorTask
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.extra
import java.util.UUID.randomUUID

class JavaTools(private val project: Project) {
    fun applyDefaultPlugins(): AuroraReport {
        project.logger.lifecycle("Apply java and maven-publish plugins")

        with(project) {
            plugins.apply("java")
            plugins.apply("org.cyclonedx.bom")
        }

        return AuroraReport(
            name = "aurora.applyDefaultPlugins",
            pluginsApplied = listOf("java", "org.cyclonedx.bom", "maven-publish")
        )
    }

    fun applyJavaDefaults(compatibility: String): AuroraReport {
        with(project) {
            setProperty("sourceCompatibility", compatibility)

            version = when (version) {
                null,
                "unspecified" -> when (val propsVersion = extensions.extraProperties.properties["version"]) {
                    null,
                    "unspecified" -> "local-SNAPSHOT"
                    else -> propsVersion
                }
                else -> version
            }

            group = when (val propsGroup = extensions.extraProperties.properties["groupId"]) {
                null,
                "unspecified" -> "no.skatteetaten.noop.${randomUUID()}"
                else -> propsGroup
            }
        }

        return AuroraReport(
            name = "aurora.applyJavaDefaults",
            description = "Set groupId, version and add sourceCompatibility"
        )
    }

    fun applyAsciiDocPlugin(): AuroraReport {
        project.logger.lifecycle("Apply asciiDoctor support")

        with(project) {
            val snippetsDir = "$buildDir/generated-snippets"
            val prepareAsciiDoctor = tasks.create("prepareAsciiDoctor")

            with(prepareAsciiDoctor) {
                mustRunAfter("clean")
                doLast {
                    extra.set("snippetsDir", mkdir(snippetsDir))
                }
            }

            val asciidoctor = tasks.named("asciidoctor", AsciidoctorTask::class.java).get()

            with(asciidoctor) {
                mustRunAfter(prepareAsciiDoctor)
                attributes(
                    mapOf(
                        "snippets" to file(snippetsDir),
                        "version" to version
                    )
                )
                inputs.dir(file(snippetsDir))
                setOutputDir(file("$buildDir/asciidoc"))
                setSourceDir(file("$projectDir/src/main/asciidoc"))

                dependsOn(prepareAsciiDoctor, "test")
            }

            with(tasks.named("jar", Jar::class.java).get()) {
                dependsOn(asciidoctor)

                from(asciidoctor.outputDir) {
                    it.into("static/docs")
                }
            }
        }

        return AuroraReport(
            name = "plugin org.asciidoctor.jvm.convert",
            description = "configure html5 report in static/docs"
        )
    }
}
