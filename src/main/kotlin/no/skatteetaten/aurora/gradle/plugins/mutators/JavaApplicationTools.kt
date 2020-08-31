package no.skatteetaten.aurora.gradle.plugins.mutators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.withGroovyBuilder

@ExperimentalStdlibApi
class JavaApplicationTools(private val project: Project) {
    fun applyKtLint(): AuroraReport {
        project.logger.lifecycle("Apply ktlint support")

        val rulesToDisable = listOf("import-ordering")

        with(project) {
            withGroovyBuilder {
                "ktlint" {
                    "android" to false
                    "disabledRules" to rulesToDisable
                }
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

            withGroovyBuilder {
                "dependencyManagement" {
                    "imports" {
                        "mavenBom" to springCloudDep
                    }
                }
            }

            val testFramework = when {
                junit5 -> "JUNIT5"
                else -> "SPOCK"
            }

            withGroovyBuilder {
                "contracts" {
                    "packageWithBaseClasses" to "$group.${project.name}.contracts"
                    "failOnNoContracts"(false)
                    "testFramework" to testFramework
                }
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

            withGroovyBuilder {
                "verifierStubsJar" {
                    "enabled" to false
                }
            }

            artifacts {
                withGroovyBuilder {
                    "archives" to stubsJar
                }
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

            withGroovyBuilder {
                "compileKotlin" {
                    "kotlinOptions" {
                        "suppressWarnings" to true
                        "jvmTarget" to 1.8
                        "freeCompilerArgs" to listOf("-Xjsr305=strict")
                    }
                }

                "compileTestKotlin" {
                    "kotlinOptions" {
                        "suppressWarnings" to true
                        "jvmTarget" to 1.8
                        "freeCompilerArgs" to listOf("-Xjsr305=strict")
                    }
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

                withGroovyBuilder {
                    """configurations.archives.artifacts.removeIf {
                        if (it.hasProperty("archiveTask")) {
                            !it.archiveTask.enabled
                        } else if (it.hasProperty("delegate") && it.delegate.hasProperty("archiveTask")) {
                            !it.delegate.archiveTask.enabled
                        } else {
                            true
                        }
                    }"""
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

            withGroovyBuilder {
                "springBoot" {
                    "buildInfo()"
                }
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

    fun applyJavaDefaults(compability: String): AuroraReport {
        with(project) {
            withGroovyBuilder {
                "sourceCompatibility" to compability
            }

            if (extensions.extraProperties.has("version")) {
                version = extensions.extraProperties["version"] as String
            }

            if (extensions.extraProperties.has("groupId")) {
                group = extensions.extraProperties["groupId"] as String
            }
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
                    withGroovyBuilder {
                        "compileTestGroovy" {
                            "dependsOn" to "compileTestKotlin"
                        }
                        "compileTestGroovy.classpath += files(compileTestKotlin.destinationDir)"
                        "testClasses" {
                            "dependsOn" to "compileTestGroovy"
                        }
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
            dependenciesAdded = testDependencies.map { "testImplemenation $it" }
        )
    }

    fun applyVersions(): AuroraReport {
        project.logger.lifecycle("Apply versions support")

        with(project) {
            withGroovyBuilder {
                "dependencyUpdates" {
                    "revision" to "release"
                    "checkForGradleUpdate" to true
                    "outputFormatter" to "json"
                    "outputDir" to "build/dependencyUpdates"
                    "reportfileName" to "report"
                    "resolutionStrategy" {
                        "componentSelection" {
                            "all" {
                                "boolean rejected = ['alpha', 'beta', 'pr', 'rc', 'cr', 'm', 'preview'].any { " +
                                    "qualifier -> candidate.version ==~ /(?i).*[.-]\${qualifier}[.\\d-]*/ }"
                                "if (rejected) { reject('Release candidate') }"
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

                withGroovyBuilder {
                    "distributions" {
                        "main" {
                            "contents" {
                                "from"("$buildDir/libs") {
                                    "into"("lib")
                                }

                                "from"("$projectDir/src/main/dist/metadata") {
                                    "into"("metadata")
                                }
                            }
                        }
                    }

                    "distZip" {
                        "dependsOn" to "bootJar"
                        "archiveClassifier" to "Leveransepakke"
                    }
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

                withGroovyBuilder {
                    "distZip.classifier" to "Leveransepakke"
                    "startScripts.enabled" to false
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
            withGroovyBuilder {
                "ext.snippetsDir" to "file"("$buildDir/generated-snippets")

                "asciidoctor" {
                    "attributes([ snippets: snippetsDir, version : version ])"
                    "inputs.dir" to "snippetsDir"
                    "outputDir" to "$buildDir/asciidoc"
                    "dependsOn" to "test"
                    "sourceDir" to "src/main/asciidoc"
                }

                "jar" {
                    "dependsOn" to "asciidoctor"
                    "from"("\${asciidoctor.outputDir}/html5") {
                        "into" to "static/docs"
                    }
                }
            }
        }

        return AuroraReport(
            name = "plugin org.asciidoctor.convert",
            description = "configure html5 report in static/docs"
        )
    }
}
