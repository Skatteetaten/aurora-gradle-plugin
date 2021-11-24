@file:Suppress("DEPRECATION")

package no.skatteetaten.aurora.gradle.plugins.mutators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import org.asciidoctor.gradle.jvm.AsciidoctorTask
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.extra
import java.util.UUID

class JavaTools(private val project: Project) {
    fun applyDefaultPlugins(): AuroraReport {
        project.logger.lifecycle("Apply java and maven-publish plugins")

        with(project) {
            plugins.apply("java")
        }

        return AuroraReport(
            name = "aurora.applyDefaultPlugins",
            pluginsApplied = listOf("java", "maven-publish")
        )
    }

    fun applyJavaDefaults(compatibility: String): AuroraReport {
        with(project) {
            setProperty("sourceCompatibility", compatibility)

            version = version ?: extensions.extraProperties.properties["version"]?.let {
                extensions.extraProperties.properties["version"] as String
            } ?: "local-SNAPSHOT"

            group = group ?: extensions.extraProperties.properties["groupId"]?.let {
                extensions.extraProperties.properties["groupId"] as String
            } ?: "no.skatteetaten.noop.${UUID.randomUUID()}"
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
                setSourceDir(file("src/main/asciidoc"))

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
