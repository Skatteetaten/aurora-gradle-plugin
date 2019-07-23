package no.skatteetaten.aurora.gradle.plugins

import org.gradle.api.Project

import groovy.transform.Canonical
import groovy.util.logging.Slf4j

@Canonical
@Slf4j
/**
 * Helper class for applying code analysis plugins like checkstyle, jacoco, pitest and sonar with default configuration.
 */
class CodeAnalysisTools {

  Project project

  CodeAnalysisTools(Project p) {

    this.project = p
  }

  AuroraReport applySonarqubeScan() {
    
    log.info("Apply sonarqube support")
    project.with {
        dependencies {
           testImplementation "org.jetbrains.kotlin:kotlin-test:1.3.30" }
    }
    
    return new AuroraReport(name:"aurora.applySonarqubeScan",
    dependenciesAdded: ["org.jetbrains.kotlin:kotlin-test:1.3.30"],
    description: "Sets dependency needed for autodiscovery of xml report.")
  }
  
  AuroraReport applyJacocoTestReport() {
    log.info("Apply jacoco support")
    project.with {
      apply plugin: "jacoco"

      jacocoTestReport {
        reports {
          xml.enabled = true
          xml.destination = file("${buildDir}/reports/jacoco/report.xml")
          csv.enabled = false
        }
      }
    }
    return new AuroraReport(name: "aurora.applyJacocoTestReport",
        pluginsApplied: ["jacoco"],
        description: "enable xml, disable csv report"
    )
  }

  AuroraReport applyPiTestSupport() {
    log.info("Apply pitest support")
    project.with {

      pitest {
        outputFormats = ['XML', 'HTML']
      }
    }

    return new AuroraReport(name : "plugin info.solidsoft.pitest", description: "output format xml and html")
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
  AuroraReport applyCheckstylePlugin(String checkstyleConfigVersion, String checkstyleConfigFile) {
    def auroraCheckstyleConfig = project.configurations.create("auroraCheckstyleConfig")

    auroraCheckstyleConfig.defaultDependencies { dependencies ->
      dependencies.add(project.dependencies.create("no.skatteetaten.aurora.checkstyle:checkstyle-config:"
          + checkstyleConfigVersion))
    }

    project.plugins.apply("checkstyle")
    project.checkstyle.config = project.resources.text.fromArchiveEntry(auroraCheckstyleConfig,
        checkstyleConfigFile)
    project.checkstyle.ignoreFailures = true
    return new AuroraReport(name: "aurora.applyCheckstylePlugin",
        dependenciesAdded: ["implementation no.skatteetaten.aurora.checkstyle:checkstyle-config:$checkstyleConfigVersion"],
        description: "with file $checkstyleConfigFile")
  }
}