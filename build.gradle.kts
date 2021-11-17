@file:Suppress("UnstableApiUsage")

import com.adarshr.gradle.testlogger.theme.ThemeType.STANDARD_PARALLEL
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask

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
}

group = properties["groupId"] as String
version = properties["version"] as String

repositories {
    gradlePluginPortal()
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
    implementation("gradle.plugin.com.gorylenko.gradle-git-properties:gradle-git-properties:${PluginVersions.git_properties}")
    implementation("se.patrikerdes:gradle-use-latest-versions-plugin:${PluginVersions.latest_versions}")

    testImplementation(gradleTestKit())
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:${Versions.assertk}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junit5}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.junit5}")
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

val sourcesJar by tasks.getting(Jar::class)

artifacts {
    add("archives", sourcesJar)
}

tasks {
    val ktlintKotlinScriptFormat by existing(GenerateReportsTask::class)
    val ktlintFormat by existing(Task::class)

    wrapper {
        distributionType = Wrapper.DistributionType.ALL
    }

    withType(KotlinCompile::class).configureEach {
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
    withType(GenerateReportsTask::class.java).configureEach {
        dependsOn(named("runKtlintFormatOverKotlinScripts"))
    }
    withType(org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask::class.java).configureEach {
        dependsOn(named("runKtlintFormatOverKotlinScripts"))
    }
    with(named("runKtlintCheckOverMainSourceSet").get()) {
        dependsOn(named("runKtlintFormatOverMainSourceSet"))
    }
    with(named("runKtlintCheckOverTestSourceSet").get()) {
        dependsOn(named("runKtlintFormatOverTestSourceSet"))
    }
    with(named("processResources").get()) {
        dependsOn(named("runKtlintFormatOverKotlinScripts"))
    }
    with(named("processTestResources").get()) {
        dependsOn(named("runKtlintFormatOverKotlinScripts"))
    }
    with(named("sourcesJar").get()) {
        dependsOn(named("runKtlintFormatOverKotlinScripts"), named("runKtlintFormatOverMainSourceSet"))
    }
    withType(org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask::class.java).configureEach {
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

    withType<Test> {
        useJUnitPlatform()
    }
}

fun Project.hasKotlin(parentProject: Project) =
    parentProject.name != rootProject.name && parentProject.plugins.hasPlugin("org.jlleitschuh.gradle.ktlint")

publishing {
    publications {
        when {
            missingRepositoryConfiguration() -> repositories {
                mavenLocal()
            }
            else -> {
                val exProps = project.extensions.extraProperties.properties
                val repositoryReleaseUrl = exProps["repositoryReleaseUrl"] as String
                val repositorySnapshotUrl = exProps["repositorySnapshotUrl"] as String
                val repositoryUsername = exProps["repositoryUsername"] as String
                val repositoryPassword = exProps["repositoryPassword"] as String

                repositories {
                    when (version.toString().endsWith("SNAPSHOT")) {
                        true -> maven {
                            name = "snapshotRepository"
                            url = uri(repositorySnapshotUrl)
                            isAllowInsecureProtocol = true
                            credentials {
                                username = repositoryUsername
                                password = repositoryPassword
                            }
                        }
                        else -> maven {
                            name = "repository"
                            url = uri(repositoryReleaseUrl)
                            isAllowInsecureProtocol = true
                            credentials {
                                username = repositoryUsername
                                password = repositoryPassword
                            }
                        }
                    }
                }
            }
        }
    }
}

fun missingRepositoryConfiguration(): Boolean = !(
    project.extensions.extraProperties.properties.containsKey("repositoryUsername") &&
        project.extensions.extraProperties.properties.containsKey("repositoryPassword") &&
        project.extensions.extraProperties.properties.containsKey("repositoryReleaseUrl") &&
        project.extensions.extraProperties.properties.containsKey("repositorySnapshotUrl")
    )