package no.skatteetaten.aurora.gradle.plugins.mutators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask

class KotlinTools(private val project: Project) {
    fun applyKotlinSupport(
        kotlinLoggingVersion: String,
        sourceCompatibility: String
    ): AuroraReport {
        project.logger.lifecycle("Apply kotlin support")

        val implementationDependencies = listOf(
            "org.jetbrains.kotlin:kotlin-reflect",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8",
            "io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion"
        )

        with(project) {
            with(dependencies) {
                implementationDependencies.forEach { add("implementation", it) }
            }

            tasks.withType(KotlinCompile::class).configureEach {
                with(it.kotlinOptions) {
                    suppressWarnings = true
                    jvmTarget = sourceCompatibility
                    freeCompilerArgs = listOf(
                        "-Xjsr305=strict",
                        "-nowarn",
                    )
                }
            }
        }

        return AuroraReport(
            name = "plugin org.jetbrains.kotlin.jvm",
            description = "jsr305 strict, jvmTarget 1.8, suppress warnings",
            dependenciesAdded = implementationDependencies.map {
                "implementation $it"
            }
        )
    }

    fun applyKtLint(): AuroraReport {
        project.logger.lifecycle("Apply ktlint support")

        with(project) {
            with(extensions.getByName("ktlint") as KtlintExtension) {
                android.set(false)
                disabledRules.set(listOf("import-ordering"))
            }

            with(tasks) {
                with(named("ktlintKotlinScriptCheck").get()) {
                    dependsOn("ktlintKotlinScriptFormat")
                }
                with(named("ktlintMainSourceSetCheck").get()) {
                    dependsOn("ktlintMainSourceSetFormat")
                }
                with(named("compileKotlin").get()) {
                    dependsOn("ktlintKotlinScriptCheck", "ktlintMainSourceSetCheck")
                }
                with(named("ktlintTestSourceSetCheck").get()) {
                    dependsOn("ktlintTestSourceSetFormat")
                }
                with(named("compileTestKotlin").get()) {
                    dependsOn("ktlintTestSourceSetCheck")
                }
                withType(GenerateReportsTask::class.java).configureEach {
                    it.dependsOn(named("runKtlintFormatOverKotlinScripts"))
                }
                withType(KtLintCheckTask::class.java).configureEach {
                    it.dependsOn(named("runKtlintFormatOverKotlinScripts"))
                }
                with(named("runKtlintCheckOverMainSourceSet").get()) {
                    dependsOn(named("runKtlintFormatOverMainSourceSet"))
                }
                with(named("runKtlintCheckOverTestSourceSet").get()) {
                    dependsOn(named("runKtlintFormatOverTestSourceSet"))
                }
                withType(KtLintFormatTask::class.java).configureEach {
                    with(it) {
                        if (name != "runKtlintFormatOverKotlinScripts") {
                            dependsOn(named("runKtlintFormatOverKotlinScripts"))

                            var parentProject = project.parent

                            while (parentProject != null) {
                                if (hasKotlin(parentProject)) {
                                    dependsOn("${parentProject.path}:runKtlintFormatOverKotlinScripts")
                                }

                                parentProject = parentProject.parent
                            }
                        }
                    }
                }
            }
        }

        return AuroraReport(
            name = "plugin org.jlleitschuh.gradle.ktlint",
            description = "disable android"
        )
    }

    private fun Project.hasKotlin(parentProject: Project) =
        parentProject.name != rootProject.name && parentProject.plugins.hasPlugin("org.jlleitschuh.gradle.ktlint")
}
