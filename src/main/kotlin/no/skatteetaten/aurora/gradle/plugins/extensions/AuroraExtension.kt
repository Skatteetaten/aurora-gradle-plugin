package no.skatteetaten.aurora.gradle.plugins.extensions

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.util.ConfigureUtil

@Suppress("unused")
open class AuroraExtension(private val project: Project) {
    val useAuroraDefaults: AuroraExtension
        get() {
            configureAuroraDefaults()

            return this
        }

    fun useAuroraDefaults() = configureAuroraDefaults()

    private fun configureAuroraDefaults() {
        useVersions()
        useSonar()
        useKotlin {
            useKtLint()
        }
        useSpringBoot {
            useWebFlux()
            useCloudContract()
        }
    }

    val useSonar: AuroraExtension
        get() {
            configureSonar()

            return this
        }

    fun useSonar() = configureSonar()

    private fun configureSonar() {
        if (!project.plugins.hasPlugin("org.sonarqube")) {
            project.plugins.apply("org.sonarqube")
        }
    }

    val useAsciiDoctor: AuroraExtension
        get() {
            configureAsciiDoctor()

            return this
        }

    fun useAsciiDoctor() = configureAsciiDoctor()

    private fun configureAsciiDoctor() {
        if (!project.plugins.hasPlugin("org.asciidoctor.convert")) {
            project.plugins.apply("org.asciidoctor.convert")
        }
    }

    val useVersions: AuroraExtension
        get() {
            configureVersions()

            return this
        }

    fun useVersions() = configureVersions()

    private fun configureVersions() {
        if (!project.plugins.hasPlugin("com.github.ben-manes.versions")) {
            project.plugins.apply("com.github.ben-manes.versions")
        }
    }

    val usePitest: AuroraExtension
        get() {
            configurePitest()

            return this
        }

    fun usePitest() = configurePitest()

    private fun configurePitest() {
        if (!project.plugins.hasPlugin("info.solidsoft.pitest")) {
            project.plugins.apply("info.solidsoft.pitest")
        }
    }

    val useKotlin: AuroraExtension
        get() {
            configureKotlin()

            return this
        }

    fun useKotlin() = configureKotlin()

    fun useKotlin(configuration: Closure<UseKotlin>) {
        configureKotlin()
        val useKotlinExt = project.getUseKotlinExtension()

        ConfigureUtil.configure(configuration, useKotlinExt)
    }

    fun useKotlin(configuration: Action<UseKotlin>) {
        configureKotlin()
        val useKotlinExt = project.getUseKotlinExtension()

        configuration.execute(useKotlinExt)
    }

    private fun configureKotlin() {
        if (!project.plugins.hasPlugin("org.jetbrains.kotlin.jvm")) {
            project.plugins.apply("org.jetbrains.kotlin.jvm")
        }

        if (project.hasSpringBootButNotKotlinSpringPlugin()) {
            project.plugins.apply("org.jetbrains.kotlin.plugin.spring")
        }
    }

    val useSpringBoot: UseSpringBoot
        get() {
            configureSpringBoot()

            return project.getUseSpringBootExtension()
        }

    fun useSpringBoot() = configureSpringBoot()

    fun useSpringBoot(configuration: Closure<UseSpringBoot>) {
        configureSpringBoot()
        val useSpringBootExt = project.getUseSpringBootExtension()

        ConfigureUtil.configure(configuration, useSpringBootExt)
    }

    fun useSpringBoot(configuration: Action<UseSpringBoot>) {
        configureSpringBoot()
        val useSpringBootExt = project.getUseSpringBootExtension()

        configuration.execute(useSpringBootExt)
    }

    private fun configureSpringBoot() {
        if (!project.plugins.hasPlugin("org.springframework.boot")) {
            project.plugins.apply("org.springframework.boot")
        }

        if (project.hasKotlinButNotKotlinSpringPlugin()) {
            project.plugins.apply("org.jetbrains.kotlin.plugin.spring")
        }
    }

    private fun Project.hasKotlinButNotKotlinSpringPlugin(): Boolean =
        plugins.hasPlugin("org.jetbrains.kotlin.jvm") && !plugins.hasPlugin("org.jetbrains.kotlin.plugin.spring")

    private fun Project.hasSpringBootButNotKotlinSpringPlugin(): Boolean =
        plugins.hasPlugin("org.springframework.boot") && !plugins.hasPlugin("org.jetbrains.kotlin.plugin.spring")

    private fun Project.getUseSpringBootExtension(): UseSpringBoot {
        val extension = project.extensions.getByType(AuroraExtension::class.java)

        return (extension as ExtensionAware).extensions.getByType(UseSpringBoot::class.java)
    }

    private fun Project.getUseKotlinExtension(): UseKotlin {
        val extension = project.extensions.getByType(AuroraExtension::class.java)

        return (extension as ExtensionAware).extensions.getByType(UseKotlin::class.java)
    }
}
