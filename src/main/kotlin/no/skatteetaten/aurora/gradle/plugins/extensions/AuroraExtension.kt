@file:Suppress("MemberVisibilityCanBePrivate")

package no.skatteetaten.aurora.gradle.plugins.extensions

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.getByType
import org.gradle.util.ConfigureUtil.configure

@Suppress("unused")
open class AuroraExtension(private val project: Project) {
    val versions: Versions
        get() = project.getVersionsExtension()
    val features: Features
        get() = project.getFeaturesExtension()

    fun versions(config: Closure<Versions>) = configure(config, project.getVersionsExtension())
    fun versions(configuration: Action<Versions>) = configuration.execute(project.getVersionsExtension())
    fun features(config: Closure<Features>) = configure(config, project.getFeaturesExtension())
    fun features(configuration: Action<Features>) = configuration.execute(project.getFeaturesExtension())

    val useAuroraDefaults: AuroraExtension
        get() = configureAuroraDefaults()

    fun useAuroraDefaults(): AuroraExtension = configureAuroraDefaults()

    private fun configureAuroraDefaults(): AuroraExtension {
        useGitProperties()
        useLatestVersions()
        useVersions()
        useSonar()
        useGradleLogger()
        useSpringBoot()

        features {
            with(it) {
                checkstylePlugin = true
            }
        }

        return this
    }

    val useLatestVersions: AuroraExtension
        get() = configureLatestVersions()

    fun useLatestVersions(): AuroraExtension = configureLatestVersions()

    private fun configureLatestVersions(): AuroraExtension {
        if (!project.plugins.hasPlugin("se.patrikerdes.use-latest-versions")) {
            project.plugins.apply("se.patrikerdes.use-latest-versions")

            project.logger.lifecycle("Applied missing plugin: Patrikerdes Use Latest Versions")
        }

        return this
    }

    val useGitProperties: AuroraExtension
        get() = configureGitProperties()

    fun useGitProperties(): AuroraExtension = configureGitProperties()

    private fun configureGitProperties(): AuroraExtension {
        if (!project.plugins.hasPlugin("com.gorylenko.gradle-git-properties")) {
            if (project.file(".git").exists()) {
                project.plugins.apply("com.gorylenko.gradle-git-properties")

                project.logger.lifecycle("Applied missing plugin: Gorylenko Git Properties")
            } else {
                project.logger.lifecycle("Cannot apply Gorylenko Git Properties! No .git Directory!")
            }
        }

        return this
    }

    val useGradleLogger: AuroraExtension
        get() = configureGradleLogger()

    fun useGradleLogger(): AuroraExtension = configureGradleLogger()

    private fun configureGradleLogger(): AuroraExtension {
        if (!project.plugins.hasPlugin("com.adarshr.test-logger")) {
            project.plugins.apply("com.adarshr.test-logger")

            project.logger.lifecycle("Applied missing plugin: Gradle Test Logger")
        }

        return this
    }

    val useSonar: AuroraExtension
        get() = configureSonar()

    fun useSonar(): AuroraExtension = configureSonar()

    private fun configureSonar(): AuroraExtension {
        if (!project.plugins.hasPlugin("org.sonarqube")) {
            project.plugins.apply("org.sonarqube")

            project.logger.lifecycle("Applied missing plugin: SonarQube")
        }

        return this
    }

    val useAsciiDoctor: AuroraExtension
        get() = configureAsciiDoctor()

    fun useAsciiDoctor(): AuroraExtension = configureAsciiDoctor()

    private fun configureAsciiDoctor(): AuroraExtension {
        if (!project.plugins.hasPlugin("org.asciidoctor.convert")) {
            project.plugins.apply("org.asciidoctor.convert")

            project.logger.lifecycle("Applied missing plugin: AsciiDoctor")
        }

        return this
    }

    val useVersions: AuroraExtension
        get() = configureVersions()

    fun useVersions(): AuroraExtension = configureVersions()

    private fun configureVersions(): AuroraExtension {
        if (!project.plugins.hasPlugin("com.github.ben-manes.versions")) {
            project.plugins.apply("com.github.ben-manes.versions")

            project.logger.lifecycle("Applied missing plugin: Ben Manes Versions")
        }

        return this
    }

    val usePitest: AuroraExtension
        get() = configurePitest()

    fun usePitest(): AuroraExtension = configurePitest()

    private fun configurePitest(): AuroraExtension {
        if (!project.plugins.hasPlugin("info.solidsoft.pitest")) {
            project.plugins.apply("info.solidsoft.pitest")

            project.logger.lifecycle("Applied missing plugin: PiTest")
        }

        return this
    }

    val useKotlin: UseKotlin
        get() = configureKotlin()

    fun useKotlin(): UseKotlin = configureKotlin()

    fun useKotlin(configuration: Closure<UseKotlin>): UseKotlin {
        val useKotlinExt = configureKotlin()

        configure(configuration, useKotlinExt)

        return useKotlinExt
    }

    fun useKotlin(configuration: Action<UseKotlin>): UseKotlin {
        val useKotlinExt = configureKotlin()

        configuration.execute(useKotlinExt)

        return useKotlinExt
    }

    private fun configureKotlin(): UseKotlin {
        if (!project.plugins.hasPlugin("org.jetbrains.kotlin.jvm")) {
            project.plugins.apply("org.jetbrains.kotlin.jvm")

            project.logger.lifecycle("Applied missing plugin: Kotlin")
        }

        if (project.hasSpringBootButNotKotlinSpringPlugin()) {
            project.plugins.apply("org.jetbrains.kotlin.plugin.spring")

            project.logger.lifecycle("Applied missing plugin: Kotlin Spring")
        }

        return project.getUseKotlinExtension()
    }

    val useSpringBoot: UseSpringBoot
        get() = configureSpringBoot()

    fun useSpringBoot() = configureSpringBoot()

    fun useSpringBoot(configuration: Closure<UseSpringBoot>): UseSpringBoot {
        val useSpringBootExt = configureSpringBoot()

        configure(configuration, useSpringBootExt)

        return useSpringBootExt
    }

    fun useSpringBoot(configuration: Action<UseSpringBoot>): UseSpringBoot {
        val useSpringBootExt = configureSpringBoot()

        configuration.execute(useSpringBootExt)

        return useSpringBootExt
    }

    private fun configureSpringBoot(): UseSpringBoot {
        if (!project.plugins.hasPlugin("org.springframework.boot")) {
            project.plugins.apply("org.springframework.boot")

            project.logger.lifecycle("Applied missing plugin: Spring Boot")
        }

        if (project.hasKotlinButNotKotlinSpringPlugin()) {
            project.plugins.apply("org.jetbrains.kotlin.plugin.spring")

            project.logger.lifecycle("Applied missing plugin: Kotlin Spring")
        }

        return project.getUseSpringBootExtension()
    }

    private fun Project.hasKotlinButNotKotlinSpringPlugin(): Boolean =
        plugins.hasPlugin("org.jetbrains.kotlin.jvm") && !plugins.hasPlugin("org.jetbrains.kotlin.plugin.spring")

    private fun Project.hasSpringBootButNotKotlinSpringPlugin(): Boolean =
        plugins.hasPlugin("org.springframework.boot") && !plugins.hasPlugin("org.jetbrains.kotlin.plugin.spring")

    private fun Project.getUseKotlinExtension(): UseKotlin {
        val extension = project.extensions.getByType(AuroraExtension::class.java)

        return (extension as ExtensionAware).extensions.getByType(UseKotlin::class.java)
    }
}

fun Project.getUseSpringBootExtension(): UseSpringBoot {
    val extension = project.extensions.getByType(AuroraExtension::class.java)

    return (extension as ExtensionAware).extensions.getByType(UseSpringBoot::class.java)
}

fun Project.getVersionsExtension(): Versions {
    val extension = project.extensions.getByType(AuroraExtension::class.java)

    return (extension as ExtensionAware).extensions.getByType(Versions::class.java)
}

fun Project.getFeaturesExtension(): Features {
    val extension = project.extensions.getByType(AuroraExtension::class.java)

    return (extension as ExtensionAware).extensions.getByType(Features::class.java)
}
