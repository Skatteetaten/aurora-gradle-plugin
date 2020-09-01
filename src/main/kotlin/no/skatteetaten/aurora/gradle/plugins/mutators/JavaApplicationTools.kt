@file:Suppress("DEPRECATION")

package no.skatteetaten.aurora.gradle.plugins.mutators

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import org.asciidoctor.gradle.AsciidoctorTask
import org.gradle.api.Project
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.file.DuplicatesStrategy.EXCLUDE
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.springframework.boot.gradle.dsl.SpringBootExtension
import org.springframework.cloud.contract.verifier.config.TestFramework.JUNIT5
import org.springframework.cloud.contract.verifier.config.TestFramework.SPOCK
import org.springframework.cloud.contract.verifier.plugin.ContractVerifierExtension

@ExperimentalStdlibApi
class JavaApplicationTools(private val project: Project) {
    fun applyKtLint(): AuroraReport {
        project.logger.lifecycle("Apply ktlint support")

        with(project) {
            with(extensions.getByName("ktlint") as KtlintExtension) {
                android.set(false)
                disabledRules.set(listOf("import-ordering"))
            }

            with(tasks) {
                with(named("compileKotlin").get()) {
                    dependsOn("ktlintMainSourceSetCheck")
                }
                with(named("compileTestKotlin").get()) {
                    dependsOn("ktlintTestSourceSetCheck")
                }
            }
        }

        return AuroraReport(
            name = "plugin org.jlleitschuh.gradle.ktlint",
            description = "disable android"
        )
    }

    fun applySpringCloudContract(
        junit5: Boolean,
        springCloudContractVersion: String
    ): AuroraReport {
        project.logger.lifecycle("Apply spring-cloud-contract support")

        val testDependencies = listOf(
            "org.springframework.cloud:spring-cloud-starter-contract-stub-runner",
            "org.springframework.cloud:spring-cloud-starter-contract-verifier",
            "org.springframework.cloud:spring-cloud-contract-wiremock",
            "org.springframework.restdocs:spring-restdocs-mockmvc"
        )

        with(project) {
            val springCloudDep =
                "org.springframework.cloud:spring-cloud-contract-dependencies:$springCloudContractVersion"

            with(extensions.getByName("dependencyManagement") as DependencyManagementExtension) {
                imports {
                    mavenBom(springCloudDep)
                }
            }

            val testFrameworkToSet = when {
                junit5 -> JUNIT5
                else -> SPOCK
            }

            with(extensions.getByName("contracts") as ContractVerifierExtension) {
                packageWithBaseClasses.set("$group.${project.name}.contracts")
                failOnNoContracts.set(false)
                testFramework.set(testFrameworkToSet)
            }

            with(dependencies) {
                testDependencies.forEach { add("testImplementation", it) }
            }

            val stubsJar = tasks.create("stubsJar", Jar::class.java) {
                archiveClassifier.set("stubs")

                into("META-INF/${project.group}/${project.name}/${project.version}/mappings") {
                    include("**/*.*")
                    from("${project.buildDir}/generated-snippets/stubs")
                }

                dependsOn("test")
            }

            with(tasks.named("verifierStubsJar", Jar::class).get()) {
                enabled = false
            }

            artifacts {
                add("archives", stubsJar)
            }
        }

        return AuroraReport(
            name = "plugin spring-cloud-contract",
            dependenciesAdded = testDependencies.map {
                "testImplementation $it"
            } + "bom org.springframework.cloud:spring-cloud-contract-dependencies:$springCloudContractVersion",
            description = "Configure stubs, testframework"
        )
    }

    fun applyKotlinSpringSupport(): AuroraReport {
        val implementationDependencies = listOf("com.fasterxml.jackson.module:jackson-module-kotlin")

        project.logger.lifecycle("Apply spring kotlin support")

        with(project) {
            with(dependencies) {
                implementationDependencies.forEach { add("implementation", it) }
            }
        }

        return AuroraReport(
            name = "plugin org.jetbrains.kotlin.plugin.spring",
            dependenciesAdded = implementationDependencies.map {
                "implementation $it"
            }
        )
    }

    fun applyKotlinSupport(kotlinLoggingVersion: String): AuroraReport {
        val implementationDependencies = listOf(
            "org.jetbrains.kotlin:kotlin-reflect",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8",
            "io.github.microutils:kotlin-logging:$kotlinLoggingVersion"
        )

        project.logger.lifecycle("Apply kotlin support")

        with(project) {
            with(dependencies) {
                implementationDependencies.forEach { add("implementation", it) }
            }

            tasks.withType(KotlinCompile::class).configureEach {
                kotlinOptions {
                    suppressWarnings = true
                    jvmTarget = "1.8"
                    freeCompilerArgs = listOf("-Xjsr305=strict")
                }
            }
        }

        return AuroraReport(
            name = "plugin org.jetbrains.kotlin.jvm",
            description = "jsr305 strict, jvmTarget 1.8, supress warnings",
            dependenciesAdded = implementationDependencies.map {
                "implementation $it"
            }
        )
    }

    fun applyJunit5(junit5Version: String): AuroraReport {
        project.logger.lifecycle("Apply Junit 5 support")

        val testDeps = listOf(
            "org.junit.jupiter:junit-jupiter-api:$junit5Version",
            "org.junit.jupiter:junit-jupiter-params:$junit5Version"
        )

        with(project) {
            extensions.extraProperties["junit-jupiter.version"] = junit5Version

            with(dependencies) {
                testDeps.forEach { add("testImplementation", it) }

                add(
                    "testRuntimeOnly",
                    "org.junit.jupiter:junit-jupiter-engine:$junit5Version"
                )
            }

            tasks.withType(Test::class.java) {
                useJUnitPlatform()
                failFast = true
            }
        }

        return AuroraReport(
            name = "aurora.applyJunit5Support",
            description = "use jUnitPlattform",
            dependenciesAdded = listOf(
                "testImplementation org.junit.jupiter:junit-jupiter-api",
                "testImplementation org.junit.jupiter:junit-jupiter-params",
                "testRuntimeOnly org.junit.jupiter:junit-jupiter-engine"
            )
        )
    }

    fun applySpring(
        mvcStarterVersion: String,
        webFluxStarterVersion: String,
        devTools: Boolean,
        webFluxEnabled: Boolean,
        bootJarEnabled: Boolean
    ): AuroraReport {
        val implementationDependencies = buildList {
            add("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

            add(
                when {
                    webFluxEnabled ->
                        "no.skatteetaten.aurora.springboot:aurora-spring-boot-webflux-starter:$webFluxStarterVersion"
                    else -> "no.skatteetaten.aurora.springboot:aurora-spring-boot-mvc-starter:$mvcStarterVersion"
                }
            )

            if (devTools) add("org.springframework.boot:spring-boot-devtools")
        }

        project.logger.lifecycle("Apply Spring support")

        with(project) {
            with(plugins) {
                apply("io.spring.dependency-management")
            }

            if (!bootJarEnabled) {
                with(tasks) {
                    getByName("jar") { enabled = true }
                    getByName("distZip") { enabled = true }
                    getByName("bootJar") { enabled = false }
                    getByName("distTar") { enabled = false }
                    getByName("bootDistTar") { enabled = false }
                    getByName("bootDistZip") { enabled = false }
                }
            }

            if (webFluxEnabled) {
                with(project) {
                    with(configurations) {
                        with(getByName("implementation")) {
                            exclude("org.springframework", "spring-webmvc")
                            exclude("org.springframework.boot", "spring-boot-starter-tomcat")
                        }
                    }
                }
            }

            with(extensions.getByName("springBoot") as SpringBootExtension) {
                buildInfo()
            }

            with(dependencies) {
                implementationDependencies.forEach { add("implementation", it) }
            }
        }

        val resolvedBootJarText = when {
            bootJarEnabled -> ", bootJar enabled"
            else -> ", bootJar disabled"
        }
        val resolvedWebFluxText = when {
            webFluxEnabled -> ", webflux enabled and webmvc + tomcat excluded"
            else -> ", webFlux disabled"
        }

        return AuroraReport(
            name = "plugin org.springframework.boot",
            dependenciesAdded = implementationDependencies.map {
                "implementation $it"
            },
            description = "Build info$resolvedBootJarText$resolvedWebFluxText, Optional devtools",
            pluginsApplied = listOf("io.spring.dependency-management")
        )
    }

    fun applyDefaultPlugins(): AuroraReport {
        with(project) {
            plugins.apply("java")
            plugins.apply("maven")
        }

        return AuroraReport(
            name = "aurora.applyDefaultPlugins",
            pluginsApplied = listOf("java", "maven")
        )
    }

    fun applyJavaDefaults(compatibility: String): AuroraReport {
        with(project) {
            setProperty("sourceCompatibility", compatibility)

            extensions.extraProperties.properties["version"]?.let { version = extensions.extraProperties.properties["version"] as String }
            extensions.extraProperties.properties["groupId"]?.let { group = extensions.extraProperties.properties["groupId"] as String }
        }

        return AuroraReport(
            name = "aurora.applyJavaDefaults",
            description = "Set groupId, version and add sourceCompability"
        )
    }

    fun applySpockSupport(
        groovyVersion: String,
        spockVersion: String,
        cglibVersion: String,
        objenesisVersion: String
    ): AuroraReport {
        val testDependencies = mutableListOf(
            "org.codehaus.groovy:groovy-all:$groovyVersion",
            "org.spockframework:spock-core:$spockVersion",
            "cglib:cglib-nodep:$cglibVersion",
            "org.objenesis:objenesis:$objenesisVersion"
        )

        project.plugins.withId("org.springframework.boot") {
            testDependencies.add("org.spockframework:spock-spring:$spockVersion")
        }

        project.logger.lifecycle("Applying spock support")

        with(project) {
            with(plugins) {
                apply("groovy")

                withId("org.jetbrains.kotlin.jvm") {
                    val kotlinTestCompile = (tasks.getByName("compileTestKotlin") as KotlinCompile)
                    val compileTestGroovy = tasks.named("compileTestGroovy", GroovyCompile::class).get()

                    with(compileTestGroovy) {
                        classpath += files(kotlinTestCompile.destinationDir)

                        dependsOn(kotlinTestCompile)
                    }

                    with(tasks.getByName("testClasses")) {
                        dependsOn(compileTestGroovy)
                    }
                }
            }

            with(dependencies) {
                testDependencies.forEach { add("testImplementation", it) }
            }
        }

        return AuroraReport(
            name = "aurora.applySpockSupport",
            pluginsApplied = listOf("groovy"),
            dependenciesAdded = testDependencies.map { "testImplementation $it" }
        )
    }

    fun applyVersions(): AuroraReport {
        project.logger.lifecycle("Apply versions support")

        with(project) {
            with(tasks.named("dependencyUpdates", DependencyUpdatesTask::class).get()) {
                revision = "release"
                checkForGradleUpdate = true
                outputFormatter = "json"
                outputDir = "build/dependencyUpdates"
                reportfileName = "report"
                resolutionStrategy {
                    componentSelection {
                        all {
                            val rejectionPatterns = listOf("alpha", "beta", "pr", "rc", "cr", "m", "preview")
                            val regex: (String) -> Regex = { Regex("(?i).*[.-]$it[.\\d-]*") }

                            if (rejectionPatterns.any { candidate.version.matches(regex(it)) }) {
                                reject("Release candidate")
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

    fun applyDeliveryBundleConfig(bootJar: Boolean): AuroraReport = when {
        bootJar -> {
            with(project) {
                plugins.apply("distribution")

                with(extensions.getByName("distributions") as DistributionContainer) {
                    with(getByName("main")) {
                        contents {
                            from("${project.buildDir}/libs") {
                                into("lib")
                            }

                            from("${project.projectDir}/src/main/dist/metadata") {
                                into("metadata")
                            }
                        }
                    }
                }

                with(tasks.named("distZip", Zip::class).get()) {
                    archiveClassifier.set("Leveransepakke")
                    duplicatesStrategy = EXCLUDE

                    dependsOn("bootJar")
                }
            }

            AuroraReport(
                name = "aurora.applyDeliveryBundleConfig",
                pluginsApplied = listOf("distribution"),
                description = "Configure Leveransepakke for bootJar"
            )
        }
        else -> {
            with(project) {
                plugins.apply("application")

                with(tasks.named("distZip", Zip::class).get()) {
                    archiveClassifier.set("Leveransepakke")
                }

                with(tasks.getByName("startScripts")) {
                    enabled = false
                }
            }

            AuroraReport(
                name = "aurora.applyDeliveryBundleConfig",
                pluginsApplied = listOf("application"),
                description = "Configure Leveransepakke"
            )
        }
    }

    fun applyAsciiDocPlugin(): AuroraReport {
        project.logger.lifecycle("Apply asciiDoctor support")

        with(project) {
            val snippetsDir = project.file("${project.buildDir}/generated-snippets")
            extra.set("snippetsDir", snippetsDir)

            with(tasks.named("asciidoctor", AsciidoctorTask::class.java).get()) {
                attributes(
                    mapOf(
                        "snippets" to snippetsDir,
                        "version" to project.version
                    )
                )
                inputs.dir(snippetsDir)
                outputDir = project.file("${project.buildDir}/asciidoc")
                sourceDir = project.file("src/main/asciidoc")

                dependsOn("test")
            }

            val asciidoctor = tasks.named("asciidoctor", AsciidoctorTask::class.java).get()

            with(tasks.named("jar", Jar::class.java).get()) {
                from("${asciidoctor.outputDir}/html5") {
                    into("static/docs")
                }

                dependsOn("asciidoctor")
            }
        }

        return AuroraReport(
            name = "plugin org.asciidoctor.convert",
            description = "configure html5 report in static/docs"
        )
    }
}
