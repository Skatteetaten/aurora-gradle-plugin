package no.skatteetaten.aurora.gradle.plugins.extensions

import org.gradle.api.Project

@Suppress("unused")
open class UseSpringBoot(private val project: Project) {
    var webFluxEnabled: Boolean? = null
    var bootJarEnabled: Boolean? = null

    val useWebFlux: UseSpringBoot
        get() {
            enableWebFlux()

            return this
        }

    fun useWebFlux() = enableWebFlux()

    private fun enableWebFlux() {
        webFluxEnabled = true
    }

    val useBootJar: UseSpringBoot
        get() {
            enableBootJar()

            return this
        }

    fun useBootJar() = enableBootJar()

    private fun enableBootJar() {
        bootJarEnabled = true
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
