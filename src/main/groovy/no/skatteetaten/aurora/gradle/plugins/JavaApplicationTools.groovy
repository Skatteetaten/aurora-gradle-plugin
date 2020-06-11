package no.skatteetaten.aurora.gradle.plugins

import org.gradle.api.Project
import org.gradle.api.artifacts.ComponentSelection
import org.gradle.jvm.tasks.Jar

import groovy.transform.Canonical
import groovy.util.logging.Slf4j

@Canonical
@Slf4j
class JavaApplicationTools {

  Project project

  JavaApplicationTools(Project p) {

    this.project = p
  }

  AuroraReport applyKtLint() {
    def rulesToDisable = [
        "import-ordering"
    ]
    project.with {
      ktlint {
        android = false
        disabledRules.set(rulesToDisable)
      }

      compileKotlin.dependsOn ':ktlintMainSourceSetCheck'
      compileTestKotlin.dependsOn ':ktlintTestSourceSetCheck'
    }

    return new AuroraReport(name: "plugin org.jlleitschuh.gradle.ktlint", description: "disable android")
  }

  AuroraReport applySpringCloudContract(Boolean junit5, String springCloudContractVersion) {
    log.info("Apply spring-cloud-contract support")
    def testDependencies = [
        "org.springframework.cloud:spring-cloud-starter-contract-stub-runner",
        "org.springframework.cloud:spring-cloud-starter-contract-verifier",
        "org.springframework.cloud:spring-cloud-contract-wiremock",
        "org.springframework.restdocs:spring-restdocs-mockmvc"
    ]
    project.with {

      dependencyManagement {
        imports {
          mavenBom "org.springframework.cloud:spring-cloud-contract-dependencies:$springCloudContractVersion"
        }
      }
      contracts {
        packageWithBaseClasses = "${groupId}.${artifactId}.contracts"

        if (junit5) {
          testFramework = "JUNIT5"
        } else {
          testFramework = "SPOCK"
        }
      }
      dependencies {
        testDependencies.each { testImplementation it }
      }

      tasks.create("stubsJar", Jar) {
        dependsOn("test")
        archiveClassifier.set("stubs")
        into("META-INF/${project.group}/${project.name}/${project.version}/mappings") {
          include("**/*.*")
          from("${project.buildDir}/generated-snippets/stubs")
        }
      }

      verifierStubsJar.enabled = false

      artifacts {
        archives stubsJar
      }

    }

    return new AuroraReport(name: "plugin spring-cloud-contract",
        dependenciesAdded: testDependencies.collect {
          "testImplementation $it"
        } + "bom org.springframework.cloud:spring-cloud-contract-dependencies:$springCloudContractVersion",
        description: "Configure stubs, testframework"
    )
  }

  AuroraReport applyKotlinSpringSupport() {
    def implementationDependencies = [
        "com.fasterxml.jackson.module:jackson-module-kotlin"
    ]

    log.info("Apply spring kotlin support")
    project.with {
      dependencies {
        implementationDependencies.each { implementation it }
      }
    }
    return new AuroraReport(
        name: "plugin org.jetbrains.kotlin.plugin.spring",
        dependenciesAdded: implementationDependencies.collect {
          "implementation $it"
        })

  }

  AuroraReport applyKotlinSupport(String kotlinLoggingVersion) {

    def implementationDependencies = [

        'org.jetbrains.kotlin:kotlin-reflect',
        'org.jetbrains.kotlin:kotlin-stdlib-jdk8',
        "io.github.microutils:kotlin-logging:$kotlinLoggingVersion"
    ]

    log.info("Apply kotlin support")
    project.with {

      dependencies {

        implementationDependencies.each { implementation it }

        compileKotlin {
          kotlinOptions {
            suppressWarnings = true
            jvmTarget = 1.8
            freeCompilerArgs = ["-Xjsr305=strict"]
          }
        }

        compileTestKotlin {
          kotlinOptions {
            suppressWarnings = true
            jvmTarget = 1.8
            freeCompilerArgs = ["-Xjsr305=strict"]
          }
        }
      }

    }
    return new AuroraReport(name: "plugin org.jetbrains.kotlin.jvm",
        description: "jsr305 strict, jvmTarget 1.8, supress warnings",
        dependenciesAdded: implementationDependencies.collect {
          "implementation $it"
        })
  }

  AuroraReport applyJunit5() {
    log.info("Apply Junit 5 support")
    project.with {

      dependencies {
        [
            'org.junit.jupiter:junit-jupiter-api',
            "org.junit.jupiter:junit-jupiter-params",
        ].each { testImplementation(it) { exclude group: 'junit' } }

        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
      }

      ext["junit-jupiter.version"] = "5.5.2"

      test {
        useJUnitPlatform()
        failFast = true
      }
    }
    return new AuroraReport(name: "aurora.applyJunit5Support", description: "use jUnitPlattform",
        dependenciesAdded: [
            "testImplementation org.junit.jupiter:junit-jupiter-api",
            "testImplementation org.junit.jupiter:junit-jupiter-params",
            "testRuntimeOnly org.junit.jupiter:junit-jupiter-engine"
        ])
  }

  AuroraReport applySpring(
    String mvcStarterVersion,
    String webFluxStarterVersion,
    Boolean devTools,
    Boolean webFluxEnabled,
    Boolean bootJarEnabled
  ) {
    def implementationDependencies = [
        "com.fasterxml.jackson.datatype:jackson-datatype-jsr310",
    ]
    if (webFluxEnabled) {
      implementationDependencies.add("no.skatteetaten.aurora.springboot:aurora-spring-boot-webflux-starter:$webFluxStarterVersion")
    } else {
      implementationDependencies.add("no.skatteetaten.aurora.springboot:aurora-spring-boot-mvc-starter:$mvcStarterVersion")
    }
    if (devTools) {
      implementationDependencies.add("org.springframework.boot:spring-boot-devtools")
    }

    log.info("Apply Spring support")

    project.with {
      apply plugin: 'io.spring.dependency-management'

      if (!bootJarEnabled) {
        [jar, distZip]*.enabled = true
        [bootJar, distTar, bootDistTar, bootDistZip]*.enabled = false

        configurations.archives.artifacts.removeIf {
          if (it.hasProperty("archiveTask")) {
            !it.archiveTask.enabled
          } else {
            !it.delegate.archiveTask.enabled
          }
        }
      }

      if (webFluxEnabled) {
        configurations.implementation {
          exclude group: "org.springframework", module: "spring-webmvc"
          exclude group: "org.springframework.boot", module: "spring-boot-starter-tomcat"
        }
      }

      springBoot {
        buildInfo()
      }

      dependencies {
        implementationDependencies.each {
          implementation it
        }
      }
    }

    def resolvedBootJarText = bootJarEnabled ? ", bootJar enabled" : ", bootJar disabled"
    def resolvedWebFluxText = webFluxEnabled ? ", webflux enabled and webmvc + tomcat excluded" : ", webFlux disabled"

    return new AuroraReport(
        name: "plugin org.springframework.boot",
        dependenciesAdded: implementationDependencies.collect {
          "implementation $it"
        },
        description: "Build info$resolvedBootJarText$resolvedWebFluxText, Optional devtools",
        pluginsApplied: ["io.spring.dependency-management"])
  }

  AuroraReport applyDefaultPlugins() {

    project.with {
      apply plugin: 'java'
      apply plugin: 'maven'

    }

    return new AuroraReport(
        name: "aurora.applyDefaultPlugins",
        pluginsApplied: ["java", "maven"]
    )

  }

  AuroraReport applyJavaDefaults(String compability) {

    project.with {

      sourceCompatibility = compability
      if (ext.has("version")) {
        version = version
      }
      if (ext.has("groupId")) {
        group = groupId
      }

    }

    return new AuroraReport(name: "aurora.applyJavaDefaults",
        description: "Set groupId, version and add sourceCompability")
  }

  AuroraReport applySpockSupport(String groovyVersion, String spockVersion, String cglibVersion,
      String objenesisVersion) {

    def testDependencies = [
        "org.codehaus.groovy:groovy-all:${groovyVersion}",
        "org.spockframework:spock-core:${spockVersion}",
        "cglib:cglib-nodep:${cglibVersion}",
        "org.objenesis:objenesis:${objenesisVersion}",
    ]

    project.plugins.withId("org.springframework.boot") {
      testDependencies.add("org.spockframework:spock-spring:$spockVersion")
    }

    log.info("Applying spock support")
    project.with {
      apply plugin: 'groovy'

      plugins.withId("org.jetbrains.kotlin.jvm") {
        compileTestGroovy.dependsOn compileTestKotlin
        compileTestGroovy.classpath += files(compileTestKotlin.destinationDir)
        testClasses.dependsOn compileTestGroovy
      }

      dependencies {
        testDependencies.each { testImplementation it }
      }
    }
    return new AuroraReport(name: "aurora.applySpockSupport", pluginsApplied: ["groovy"],
        dependenciesAdded: testDependencies.collect { "testImplemenation $it" })
  }

  AuroraReport applyVersions() {
    project.with {
      dependencyUpdates.revision = "release"
      dependencyUpdates.checkForGradleUpdate = true
      dependencyUpdates.outputFormatter = "json"
      dependencyUpdates.outputDir = "build/dependencyUpdates"
      dependencyUpdates.reportfileName = "report"
      dependencyUpdates.resolutionStrategy {
        componentSelection { rules ->
          rules.all { ComponentSelection selection ->
            boolean rejected = ['alpha', 'beta', 'pr', 'rc', 'cr', 'm', 'preview'].any { qualifier ->
              selection.candidate.version ==~ /(?i).*[.-]${qualifier}[.\d-]*/
            }
            if (rejected) {
              selection.reject('Release candidate')
            }
          }
        }
      }
    }

    return new AuroraReport(
        name: "plugin com.github.ben-manes.versions",
        description: "only allow stable versions in upgrade")
  }

  AuroraReport applyDeliveryBundleConfig(Boolean bootJar) {
    if (bootJar) {
      project.with {
        apply plugin: 'distribution'

        distributions {
          main {
            contents {
              from("${buildDir}/libs") {
                into('lib')
              }

              from("${projectDir}/src/main/dist/metadata") {
                into('metadata')
              }
            }
          }
        }

        distZip {
          dependsOn 'bootJar'
          archiveClassifier = 'Leveransepakke'
        }
      }

      return new AuroraReport(name: "aurora.applyDeliveryBundleConfig",
              pluginsApplied: ["distribution"],
              description: "Configure Leveransepakke for bootJar")
    } else {
      project.with {
        apply plugin: 'application'

        distZip.classifier = 'Leveransepakke'
        startScripts.enabled = false
      }

      return new AuroraReport(name: "aurora.applyDeliveryBundleConfig", pluginsApplied: ["application"],
              description: "Configure Leveransepakke")
    }
  }

  AuroraReport applyAsciiDocPlugin() {

    project.with {
      ext.snippetsDir = file("$buildDir/generated-snippets")

      asciidoctor {
        attributes([
            snippets: snippetsDir,
            version : version
        ])
        inputs.dir snippetsDir
        outputDir "$buildDir/asciidoc"
        dependsOn test
        sourceDir 'src/main/asciidoc'
      }

      jar {
        dependsOn asciidoctor
        from("${asciidoctor.outputDir}/html5") {
          into 'static/docs'
        }
      }
    }
    return new AuroraReport(
        name: "plugin org.asciidoctor.convert",
        description: "configure html5 report in static/docs")

  }

}
