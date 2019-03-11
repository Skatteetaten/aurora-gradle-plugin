package no.skatteetaten.aurora.gradle.plugins

import org.gradle.api.Project
import org.gradle.api.artifacts.ComponentSelection
import org.gradle.jvm.tasks.Jar

import groovy.transform.Canonical

@Canonical
class JavaApplicationTools {

  Project project

  void applyJavaApplicationConfig(Map<String, Object> config) {

    if (config.applyDefaultPlugins == true) {
      applyDefaultPlugins(project)
    }

    if (config.applyJavaDefaults == true) {
      applyJavaDefaults(project, config.javaSourceCompatibility)
    }

    if (config.applySpockSupport == true) {
      applySpockSupport(project, config.groovyVersion, config.spockVersion, config.cglibVersion,
          config.objenesisVersion)
    }

    project.plugins.withId("org.asciidoctor.convert") {
      applyAsciiDocPlugin(project)
    }

    project.plugins.withId("com.github.ben-manes.versions") {
      project.with {
        dependencyUpdates.revision = "release"
        dependencyUpdates.checkForGradleUpdate = true
        dependencyUpdates.outputFormatter = "json"
        dependencyUpdates.outputDir = "build/dependencyUpdates"
        dependencyUpdates.reportfileName = "report"
        dependencyUpdates.resolutionStrategy {
          componentSelection { rules ->
            rules.all { ComponentSelection selection ->
              boolean rejected = ['alpha', 'beta', 'rc', 'cr', 'm', 'preview'].any { qualifier ->
                selection.candidate.version ==~ /(?i).*[.-]${qualifier}[.\d-]*/
              }
              if (rejected) {
                selection.reject('Release candidate')
              }
            }
          }
        }
      }
    }

    if (config.applyDeliveryBundleConfig == true) {
      applyDeliveryBundleConfig(project)
    }

    project.plugins.withId("org.springframework.boot") {
      applySpring(project, config.auroraSpringBootStarterVersion)
    }

    if (config.applyJunit5Support == true) {
      applyJunit5(project)
    }

    project.plugins.withId("org.jetbrains.kotlin.jvm") {
      applyKotlinSupport(project, config.kotlinLoggingVersion)
    }

    project.plugins.withId("org.jetbrains.kotlin.plugin.spring") {
      applyKotlinSpringSupport(project)
    }

  }

  def applySpringCloudContract(Project project, Boolean junit5, String springCloudContractVersion) {
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
        testImplementation("org.springframework.cloud:spring-cloud-starter-contract-verifier")
        testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock")
      }

      tasks.create("stubsJar", Jar) {
        dependsOn("test")
        archiveClassifier.set("stubs")
        into("META-INF/${project.group}/${project.name}/${project.version}/mappings") {
          include("**/*.*")
          from("${project.buildDir}/generated-snippets/stubs")
        }
      }

// we want to disable the default Spring Cloud Contract stub jar generation
      verifierStubsJar.enabled = false

      artifacts {
        archives stubsJar
      }

    }
  }

  void applyKotlinSpringSupport(Project project) {
    project.with {
      dependencies {
        implementation('com.fasterxml.jackson.module:jackson-module-kotlin')
      }
    }
  }

  void applyKotlinSupport(Project project, String kotlinLoggingVersion) {

    project.with {

      dependencies {

        implementation(
            'org.jetbrains.kotlin:kotlin-reflect',
            'org.jetbrains.kotlin:kotlin-stdlib-jdk8',
            "io.github.microutils:kotlin-logging:$kotlinLoggingVersion"
        )

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
  }

  void applyJunit5(Project project) {
    project.with {

      dependencies {
        [
            'org.junit.jupiter:junit-jupiter-api',
            "org.junit.jupiter:junit-jupiter-params",
        ].each { testImplementation(it) { exclude group: 'junit' } }

        testImplementation(
            "org.junit.jupiter:junit-jupiter-api",
        )
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
      }
      test {
        useJUnitPlatform()
      }
    }
  }

  void applySpring(Project project, String starterVersion) {

    project.with {

      apply plugin: 'io.spring.dependency-management'

      [jar, distZip]*.enabled = true
      [bootJar, distTar, bootDistTar, bootDistZip]*.enabled = false

      configurations.archives.artifacts.removeIf {
        if (it.hasProperty("archiveTask")) {
          !it.archiveTask.enabled
        } else {
          !it.delegate.archiveTask.enabled
        }
      }

      springBoot {
        buildInfo()
      }
      dependencies {
        implementation(
            'com.fasterxml.jackson.datatype:jackson-datatype-jsr310',
            "no.skatteetaten.aurora.springboot:aurora-spring-boot2-starter:$starterVersion",
        )
      }

    }

  }

  void applyDefaultPlugins(Project project) {

    project.with {
      apply plugin: 'java'
      apply plugin: 'maven'

    }
  }

  void applyJavaDefaults(Project project, String compability) {

    project.with {

      sourceCompatibility = compability
      if (ext.has("version")) {
        version = version
      }
      if (ext.has("groupId")) {
        group = groupId
      }

    }

  }

  void applySpockSupport(Project project, String groovyVersion, String spockVersion, String cglibVersion,
      String objenesisVersion) {

    project.with {
      apply plugin: 'groovy'

      dependencies {
        testCompile(
            "org.codehaus.groovy:groovy-all:${groovyVersion}",
            "org.spockframework:spock-core:${spockVersion}",
            "cglib:cglib-nodep:${cglibVersion}",
            "org.objenesis:objenesis:${objenesisVersion}",
        )
      }
    }
  }

  void applyDeliveryBundleConfig(Project project) {

    project.with {
      apply plugin: 'application'

      distZip.classifier = 'Leveransepakke'
      startScripts.enabled = false
    }
  }

  void applyAsciiDocPlugin(Project project) {

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
  }

}
