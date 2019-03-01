package no.skatteetaten.aurora.gradle.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project

import groovy.util.logging.Slf4j

@Slf4j
class AuroraPlugin implements Plugin<Project> {

  private static DEFAULT_CONFIG = [
      applyDefaultPlugins      : true,
      applyJavaDefaults        : true,
      applyDeliveryBundleConfig: true,
      applySpockSupport        : false,
      groovyVersion            : '2.5.4',
      spockVersion             : '1.2-groovy-2.5',
      cglibVersion             : '3.1',
      objenesisVersion         : '2.1',
      applyAsciiDocPlugin      : true,
      applyCheckstylePlugin    : true,
      applyJacocoTestReport    : true,
      applyPiTestSupport       : true,
      applySonarPlugin         : true,
      applyNexusRepositories   : true,
      applyMavenDeployer       : true,
      requireStaging           : true,
      stagingProfileId         : null,
      ktlintVersion            : "6.3.1",
      kotlinLoggingVersion     : "1.6.24",
      checkstyleConfigVersion  : "2.1.6",
      checkstyleConfigFile     : 'checkstyle/checkstyle-with-metrics.xml',
  ]

  void apply(Project p) {

    Map<String, Object> config = getConfiguration(p)

    onApplyPlugin(p, config)

    p.afterEvaluate {
      // We do everything on apply
    }
  }

  protected void onApplyPlugin(Project p, Map<String, Object> config) {

    def mavenTools = new MavenTools(p)
    if (config.applyNexusRepositories) {
      mavenTools.applyRepositories()
    }

    new JavaApplicationTools(p).applyJavaApplicationConfig(config)
    new CodeAnalysisTools(p).applyCodeAnalysisPlugins(config)

    mavenTools.with {

      setDefaultTasks()

      if (config.applyMavenDeployer) {
        addMavenDeployer(config.requireStaging, config.stagingProfileId)
      }
    }
  }

  private static Map<String, Object> getConfiguration(Project p) {

    def config = [:]
    config.putAll(DEFAULT_CONFIG)
    config.putAll(p.ext.properties.aurora ?: [:])
    config
  }

}
