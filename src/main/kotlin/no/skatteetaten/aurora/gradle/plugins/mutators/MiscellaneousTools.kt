package no.skatteetaten.aurora.gradle.plugins.mutators

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import java.util.Locale

class MiscellaneousTools(private val project: Project) {

    // Taken from https://github.com/ben-manes/gradle-versions-plugin#rejectversionsif-and-componentselection
    private fun isNonStable(version: String): Boolean {
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase(Locale.getDefault()).contains(it) }
        val regex = "^[0-9,.v-]+(-r)?$".toRegex()
        val isStable = stableKeyword || regex.matches(version)
        return isStable.not()
    }
    fun applyVersions(): AuroraReport {
        project.logger.lifecycle("Apply versions support")

        with(project) {
            tasks.withType<DependencyUpdatesTask> {
                revision = "release"
                checkForGradleUpdate = true
                outputFormatter = "json"
                outputDir = "build/dependencyUpdates"
                reportfileName = "report"
                rejectVersionIf {
                    isNonStable(it.candidate.version)
                }
            }
        }

        return AuroraReport(
            name = "plugin com.github.ben-manes.versions",
            description = "only allow stable versions in upgrade"
        )
    }
}
