package no.skatteetaten.aurora.gradle.plugins

import org.gradle.api.Project

import groovy.transform.Canonical

@Canonical
class JavaApplicationTools {

  Project project

  void applyJavaApplicationConfig(Map<String, Object> config) {

    if (config.applyDefaultPlugins) {
      applyDefaultPlugins(project)
    }

    if (config.applyJavaDefaults) {
      applyJavaDefaults(project)
    }
    if (config.applySpockSupport) {
      applySpockSupport(project, config.groovyVersion, config.spockVersion, config.cglibVersion,
          config.objenesisVersion)
    }
    if (config.applyAsciiDocPlugin) {
      applyAsciiDocPlugin(project)
    }

    if (config.applyDeliveryBundleConfig) {
      applyDeliveryBundleConfig(project)
    }

    project.plugins.withId("org.springframework.boot") {
      applySpring(project, config.auroraSpringBootStarterVersion)
    }

    if (config.applyJunit5Support) {
      applyJunit5(project)
    }

    project.plugins.withId("org.springframework.boot") {
      applyKotlinSupport(project, config.kotlinLoggingVersion)
    }

    project.plugins.withId("org.jetbrains.kotlin.plugin.spring"){
      applyKotlinSpringSupport(project)
    }

  }

  void applyKotlinSpringSupport(Project project) {
    project.with {
      dependencies {
        compile('com.fasterxml.jackson.module:jackson-module-kotlin')
      }
    }
  }

  void applyKotlinSupport(Project project, String kotlinLoggingVersion) {

    project.with {

      dependencies {

        compile(
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
        ].each { testCompile(it) { exclude group: 'junit' } }

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
        compile(
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

  void applyJavaDefaults(Project project) {

    project.sourceCompatibility = '1.8'
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
      apply plugin: 'org.asciidoctor.convert'

      ext.snippetsDir = file("$buildDir/docs/generated-snippets")

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
