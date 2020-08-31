import com.adarshr.gradle.testlogger.theme.ThemeType.STANDARD_PARALLEL
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintFormatTask

plugins {
    `kotlin-dsl`
    kotlin("jvm") version("1.3.72")

    id("idea")
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish") version("0.12.0")
    id("com.github.ben-manes.versions") version("0.29.0")
    id("org.jlleitschuh.gradle.ktlint") version("9.3.0")
    id("com.adarshr.test-logger") version("2.1.0")
}

group = properties["groupId"] as String
version = properties["version"] as String

repositories {
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib", "1.3.72"))
    implementation("io.github.microutils:kotlin-logging:1.8.3")
    implementation("org.springframework.boot:spring-boot-gradle-plugin:2.3.3.RELEASE")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.72")
    implementation("org.jetbrains.kotlin:kotlin-allopen:1.3.72")
    implementation("org.asciidoctor:asciidoctor-gradle-jvm:2.4.0")
    implementation("com.github.ben-manes:gradle-versions-plugin:0.29.0")
    implementation("info.solidsoft.gradle.pitest:gradle-pitest-plugin:1.5.2")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:9.3.0")
    implementation("gradle.plugin.org.springframework.cloud:spring-cloud-contract-gradle-plugin:2.2.4.RELEASE")
    implementation("io.spring.gradle:dependency-management-plugin:1.0.10.RELEASE")
    implementation("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:3.0")
    implementation("com.adarshr:gradle-test-logger-plugin:2.1.0")

    testImplementation(gradleTestKit())
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.22")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")
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

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

testlogger {
    theme = STANDARD_PARALLEL
    showStandardStreams = true
    showPassedStandardStreams = false
    showSkippedStandardStreams = false
    showFailedStandardStreams = true
}

tasks {
    val ktlintKotlinScriptFormat by existing(KtlintFormatTask::class)
    val ktlintFormat by existing(Task::class)

    wrapper {
        distributionType = Wrapper.DistributionType.ALL
    }

    @Suppress("UNUSED_VARIABLE")
    val compileKotlin by existing(KotlinCompile::class) {
        kotlinOptions {
            languageVersion = "1.3.72"
        }

        dependsOn(listOf(ktlintKotlinScriptFormat, ktlintFormat))
    }

    withType<Test> {
        useJUnitPlatform()
    }
}
