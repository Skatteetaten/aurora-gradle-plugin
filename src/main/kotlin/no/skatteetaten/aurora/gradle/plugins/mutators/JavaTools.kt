@file:Suppress("DEPRECATION")

package no.skatteetaten.aurora.gradle.plugins.mutators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import org.asciidoctor.gradle.jvm.AsciidoctorTask
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.extra

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

            extensions.extraProperties.properties["version"]?.let {
                version = extensions.extraProperties.properties["version"] as String
            }
            extensions.extraProperties.properties["groupId"]?.let {
                group = extensions.extraProperties.properties["groupId"] as String
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
            val snippetsDir = project.file("${project.buildDir}/generated-snippets")
            extra.set("snippetsDir", snippetsDir)

            with(tasks.named("asciidoctor", AsciidoctorTask::class.java).get()) {
                attributes(
                    mapOf(
                        "snippets" to snippetsDir,
                        "version" to project.version
                    )
                )
                inputs.dir(snippetsDir)
                setOutputDir(project.file("${project.buildDir}/asciidoc"))
                setSourceDir(project.file("src/main/asciidoc"))

                dependsOn("test")
            }

            val asciidoctor = tasks.named("asciidoctor", AsciidoctorTask::class.java).get()

            with(tasks.named("jar", Jar::class.java).get()) {
                from("${asciidoctor.outputDir}/html5") {
                    into("static/docs")
                }
            }
        }

        return AuroraReport(
            name = "plugin org.asciidoctor.jvm.convert",
            description = "configure html5 report in static/docs"
        )
    }
}
