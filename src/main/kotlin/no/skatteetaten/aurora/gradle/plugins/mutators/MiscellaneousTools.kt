package no.skatteetaten.aurora.gradle.plugins.mutators

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.github.benmanes.gradle.versions.updates.resolutionstrategy.ComponentSelectionWithCurrent
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import org.gradle.api.Project
import org.gradle.kotlin.dsl.named

class MiscellaneousTools(private val project: Project) {
    fun applyVersions(): AuroraReport {
        project.logger.lifecycle("Apply versions support")

        with(project) {
            with(tasks.named("dependencyUpdates", DependencyUpdatesTask::class).get()) {
                revision = "release"
                checkForGradleUpdate = true
                outputFormatter = "json"
                outputDir = "build/dependencyUpdates"
                reportfileName = "report"
                resolutionStrategy { strategy ->
                    strategy.componentSelection { selection ->
                        selection.all { all: ComponentSelectionWithCurrent ->
                            val rejectionPatterns = listOf("alpha", "beta", "pr", "rc", "cr", "m", "preview")
                            val regex: (String) -> Regex = { Regex("(?i).*[.-]$it[.\\d-]*") }

                            if (rejectionPatterns.any { all.candidate.version.matches(regex(it)) }) {
                                all.reject("Release candidate")
                            }
                        }
                    }
                }
            }
        }

        return AuroraReport(
            name = "plugin com.github.ben-manes.versions",
            description = "only allow stable versions in upgrade"
        )
    }
}
