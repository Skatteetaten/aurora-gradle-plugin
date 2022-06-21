@file:Suppress("UnstableApiUsage")

import com.adarshr.gradle.testlogger.theme.ThemeType.STANDARD_PARALLEL
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask

plugins {
    kotlin("jvm") version(Versions.kotlin)

    id("idea")
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish") version(PluginVersions.gradle_plugin_publish)
    id("com.github.ben-manes.versions") version(PluginVersions.ben_manes_versions)
    id("org.jlleitschuh.gradle.ktlint") version(PluginVersions.ktlint)
    id("com.adarshr.test-logger") version(PluginVersions.gradle_test_logger)
    id("se.patrikerdes.use-latest-versions") version(PluginVersions.latest_versions)
    id("org.cyclonedx.bom") version(PluginVersions.cyclonedx_versions)
}

dependencies {
    implementation(gradleKotlinDsl())
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}")
    implementation(fileTree("$rootDir/buildSrc/build/libs") { include("*.jar") })
    implementation("io.github.microutils:kotlin-logging-jvm:${Versions.kotlinLogging}")
    implementation("org.springframework.boot:spring-boot-gradle-plugin:${PluginVersions.spring_boot}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-allopen:${Versions.kotlin}")
    implementation("org.asciidoctor:asciidoctor-gradle-jvm:${PluginVersions.asciidoctor}")
    implementation("com.github.ben-manes:gradle-versions-plugin:${PluginVersions.ben_manes_versions}")
    implementation("info.solidsoft.gradle.pitest:gradle-pitest-plugin:${PluginVersions.pitest}")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:${PluginVersions.ktlint}")
    implementation("gradle.plugin.org.springframework.cloud:spring-cloud-contract-gradle-plugin:${PluginVersions.cloud_contract}")
    implementation("io.spring.gradle:dependency-management-plugin:${PluginVersions.dependency_management}")
    implementation("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:${PluginVersions.sonar}")
    implementation("com.adarshr:gradle-test-logger-plugin:${PluginVersions.gradle_test_logger}")
    implementation("com.gorylenko.gradle-git-properties:gradle-git-properties:${PluginVersions.git_properties}")
    implementation("se.patrikerdes:gradle-use-latest-versions-plugin:${PluginVersions.latest_versions}")
    implementation("org.cyclonedx:cyclonedx-gradle-plugin:${PluginVersions.cyclonedx_versions}")

    testImplementation(gradleTestKit())
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:${Versions.assertk}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junit5}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.junit5}")
}

testlogger {
    theme = STANDARD_PARALLEL
    showStandardStreams = true
    showPassedStandardStreams = false
    showSkippedStandardStreams = false
    showFailedStandardStreams = true
}

java {
    withSourcesJar()
}

tasks {
    configureKotlin()

    withType<Test> { useJUnitPlatform() }
}

gradlePlugin {
    plugins {
        create("auroraPlugin") {
            id = "no.skatteetaten.gradle.aurora"
            displayName = "Skatteetaten Aurora Gradle plugin"
            implementationClass = "no.skatteetaten.aurora.gradle.plugins.AuroraPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/Skatteetaten/aurora-gradle-plugin"
    vcsUrl = "https://github.com/Skatteetaten/aurora-gradle-plugin"
    description = "Gradle plugin to apply Skatteetaten specific settings to java projects"
    tags = setOf("skatteetaten", "corporate")
}

publishing {
    publications {
        when {
            missingRepositoryConfiguration() -> repositories { mavenLocal() }
            else -> configureNexus(this@publishing)
        }
    }
}

fun TaskContainerScope.configureKotlin() {
    val ktlintKotlinScriptFormat by existing(GenerateReportsTask::class)
    val ktlintFormat by existing(Task::class)

    withType(KotlinCompile::class) {
        kotlinOptions {
            suppressWarnings = true
            jvmTarget = Versions.javaSourceCompatibility
            freeCompilerArgs = listOf(
                "-nowarn",
                "-Xopt-in=kotlin.ExperimentalStdlibApi",
                "-Xopt-in=kotlin.RequiresOptIn"
            )
            languageVersion = Versions.kotlin.substringBeforeLast(".")
        }

        dependsOn(listOf(ktlintKotlinScriptFormat, ktlintFormat))
    }

    configureKtLint()
}

fun TaskContainerScope.configureKtLint() {
    withType(GenerateReportsTask::class.java) {
        dependsOn(named("runKtlintFormatOverKotlinScripts"))
    }
    withType(KtLintCheckTask::class.java) {
        dependsOn(named("runKtlintFormatOverKotlinScripts"))
    }
    with(named("runKtlintCheckOverMainSourceSet").get()) {
        dependsOn(named("runKtlintFormatOverMainSourceSet"))
    }
    with(named("runKtlintCheckOverTestSourceSet").get()) {
        dependsOn(named("runKtlintFormatOverTestSourceSet"))
    }
    with(named("sourcesJar").get()) {
        dependsOn(
            named("runKtlintFormatOverKotlinScripts"),
            named("runKtlintFormatOverMainSourceSet"),
        )
    }
    withType(KtLintFormatTask::class.java) {
        if (name != "runKtlintFormatOverKotlinScripts") {
            dependsOn(named("runKtlintFormatOverKotlinScripts"))

            var parentProject = project.parent

            while (parentProject != null) {
                if (project.hasKotlin(parentProject)) {
                    dependsOn("${parentProject.path}:runKtlintFormatOverKotlinScripts")
                }

                parentProject = parentProject.parent
            }
        }
    }
}
