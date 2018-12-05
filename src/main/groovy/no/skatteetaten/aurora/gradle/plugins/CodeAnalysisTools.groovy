package no.skatteetaten.aurora.gradle.plugins

import org.gradle.api.Project

import groovy.transform.Canonical

@Canonical
/**
 * Helper class for applying code analysis plugins like checkstyle, jacoco, pitest and sonar with default configuration.
 */
class CodeAnalysisTools {

  Project project

  void applyCodeAnalysisPlugins(Map<String, Object> config) {

    if (config.applyCheckstylePlugin) {
      applyCheckstylePlugin(config.checkstyleConfigVersion as String, config.checkstyleConfigFile as String)
    }
    if (config.applyJacocoTestReport) {
      applyJacocoTestReport()
    }
    if (config.applyPiTestSupport) {
      applyPiTestSupport()
    }
    if (config.applySonarPlugin) {
      applySonarPlugin()
    }
  }

  void applyJacocoTestReport() {
    project.with {
      apply plugin: "jacoco"

      jacocoTestReport {
        reports {
          xml.enabled false
          csv.enabled false
        }
      }
    }
  }

  void applyPiTestSupport() {
    project.with {
      apply plugin: "info.solidsoft.pitest"

      pitest {
        outputFormats = ['XML', 'HTML']
      }
    }
  }

  void applySonarPlugin() {
    project.with {
      apply plugin: 'org.sonarqube'
    }
  }

  /**
   * Create a new configuration, "auroraCheckstyleConfig", and then add the Aurora Checkstyle configuration dependency
   * to this configuration.
   * Apply the Checkstyle plugin and assign the specified configuration file from Aurora Checkstyle configuration
   * archive.
   *
   * @param checkstyleConfigVersion
   * @param checkstyleConfigFile
   */
  private void applyCheckstylePlugin(String checkstyleConfigVersion, String checkstyleConfigFile) {
    def auroraCheckstyleConfig = project.configurations.create("auroraCheckstyleConfig")

    auroraCheckstyleConfig.defaultDependencies { dependencies ->
      dependencies.add(project.dependencies.create("no.skatteetaten.aurora.checkstyle:checkstyle-config:"
          + checkstyleConfigVersion))
    }

    project.plugins.apply("checkstyle")
    project.checkstyle.config = project.resources.text.fromArchiveEntry(auroraCheckstyleConfig,
        checkstyleConfigFile)
    project.checkstyle.ignoreFailures = true
  }
}
