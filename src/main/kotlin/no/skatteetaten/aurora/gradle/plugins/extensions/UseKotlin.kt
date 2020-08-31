package no.skatteetaten.aurora.gradle.plugins.extensions

import org.gradle.api.Project

@Suppress("unused")
open class UseKotlin(private val project: Project) {
    val useKtLint: UseKotlin
        get() {
            configureKtLint()

            return this
        }

    fun useKtLint() = configureKtLint()

    private fun configureKtLint() {
        if (!project.plugins.hasPlugin("org.jlleitschuh.gradle.ktlint")) {
            project.plugins.apply("org.jlleitschuh.gradle.ktlint")
        }
    }
}
