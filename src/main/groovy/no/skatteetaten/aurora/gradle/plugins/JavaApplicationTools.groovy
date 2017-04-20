package no.skatteetaten.aurora.gradle.plugins

import org.gradle.api.Project

import groovy.transform.Canonical

@Canonical
class JavaApplicationTools {

  Project project

  void applyJavaApplicationConfig(Map<String, Object> config) {

    if (config.applyDefaultPlugins) {
      applyDefaultPlugins(p)
    }

    if (config.applyJavaDefaults) {
      applyJavaDefaults(p)
    }
    if (config.applySpockSupport) {
      applySpockSupport(p, config.groovyVersion, config.spockVersion, config.cglibVersion, config.objenesisVersion)
    }
    if (config.applyAsciiDocPlugin) {
      applyAsciiDocPlugin(p)
    }

    if (config.applyDeliveryBundleConfig) {
      applyDeliveryBundleConfig(p)
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
