package no.skatteetaten.aurora.gradle.plugins.mutators

import info.solidsoft.gradle.pitest.PitestPluginExtension
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import org.gradle.api.Project
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.named
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class TestTools(private val project: Project) {
    fun applyJunit5(junit5Version: String): AuroraReport {
        project.logger.lifecycle("Apply Junit 5 support")

        val testDeps = listOf(
            "org.junit.jupiter:junit-jupiter-api:$junit5Version",
            "org.junit.jupiter:junit-jupiter-params:$junit5Version"
        )

        with(project) {
            extensions.extraProperties["junit-jupiter.version"] = junit5Version

            with(dependencies) {
                testDeps.forEach { add("testImplementation", it) }

                add(
                    "testImplementation",
                    "org.junit.jupiter:junit-jupiter-engine:$junit5Version"
                )
            }

            tasks.withType(Test::class.java) {
                it.useJUnitPlatform()
                it.failFast = true
            }
        }

        return AuroraReport(
            name = "aurora.applyJunit5Support",
            description = "use jUnitPlattform",
            dependenciesAdded = listOf(
                "testImplementation org.junit.jupiter:junit-jupiter-api",
                "testImplementation org.junit.jupiter:junit-jupiter-params",
                "testImplementation org.junit.jupiter:junit-jupiter-engine"
            )
        )
    }

    fun applySpockSupport(
        groovyVersion: String,
        spockVersion: String,
        cglibVersion: String,
        objenesisVersion: String
    ): AuroraReport {
        project.logger.lifecycle("Applying spock support")

        val testDependencies = mutableListOf(
            "org.codehaus.groovy:groovy-all:$groovyVersion",
            "org.spockframework:spock-core:$spockVersion",
            "cglib:cglib-nodep:$cglibVersion",
            "org.objenesis:objenesis:$objenesisVersion"
        )

        project.plugins.withId("org.springframework.boot") {
            testDependencies.add("org.spockframework:spock-spring:$spockVersion")
        }

        with(project) {
            with(plugins) {
                apply("groovy")

                withId("org.jetbrains.kotlin.jvm") {
                    val kotlinTestCompile = (tasks.getByName("compileTestKotlin") as KotlinCompile)
                    val compileTestGroovy = tasks.named("compileTestGroovy", GroovyCompile::class).get()

                    with(compileTestGroovy) {
                        classpath += files(kotlinTestCompile.destinationDir)

                        dependsOn(kotlinTestCompile)
                    }

                    with(tasks.getByName("testClasses")) {
                        dependsOn(compileTestGroovy)
                    }
                }
            }

            with(dependencies) {
                testDependencies.forEach { add("testImplementation", it) }
            }
        }

        return AuroraReport(
            name = "aurora.applySpockSupport",
            pluginsApplied = listOf("groovy"),
            dependenciesAdded = testDependencies.map { "testImplementation $it" }
        )
    }

    fun applyJacocoTestReport(): AuroraReport {
        project.logger.lifecycle("Apply jacoco support")

        return when {
            project.plugins.hasPlugin("java") -> {
                with(project) {
                    plugins.apply("jacoco")

                    tasks.named("jacocoTestReport", JacocoReport::class.java) {
                        with(it.reports) {
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
}
