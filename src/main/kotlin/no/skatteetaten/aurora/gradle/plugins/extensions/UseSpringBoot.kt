package no.skatteetaten.aurora.gradle.plugins.extensions

import org.gradle.api.Project

@Suppress("unused")
open class UseSpringBoot(private val project: Project) {
    var webFluxEnabled: Boolean = false

    val useWebFlux: UseSpringBoot
        get() {
            enableWebFlux()

            return this
        }

    fun useWebFlux() = enableWebFlux()

    private fun enableWebFlux() {
        webFluxEnabled = true
    }

    val useCloudContract: UseSpringBoot
        get() {
            configureCloudContract()

            return this
        }

    fun useCloudContract() = configureCloudContract()

    private fun configureCloudContract() {
        if (!project.plugins.hasPlugin("org.springframework.cloud.contract")) {
            project.plugins.apply("org.springframework.cloud.contract")
        }
    }
}
