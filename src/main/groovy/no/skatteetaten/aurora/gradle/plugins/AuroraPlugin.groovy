package no.skatteetaten.aurora.gradle.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project

import groovy.util.logging.Slf4j

@Slf4j
class AuroraPlugin implements Plugin<Project> {

  private static DEFAULT_CONFIG = [
      applyDefaultPlugins           : true,
      applyJavaDefaults             : true,
      javaSourceCompatibility       : "1.8",
      applyDeliveryBundleConfig     : true,
      applySpockSupport             : false,
      groovyVersion                 : '2.5.4',
      spockVersion                  : '1.2-groovy-2.5',
      cglibVersion                  : '3.1',
      objenesisVersion              : '2.1',
      applyCheckstylePlugin         : true,
      applyJacocoTestReport         : true,
      applyMavenDeployer            : true,
      requireStaging                : false,
      auroraSpringBootStarterVersion: "2.0.0",
      springCloudContractVersion    : "2.1.1.RELEASE",
      stagingProfileId              : null,
      kotlinLoggingVersion          : "1.6.25",
      checkstyleConfigVersion       : "2.1.6",
      checkstyleConfigFile          : 'checkstyle/checkstyle-with-metrics.xml',
      applyJunit5Support            : true
  ]

  void apply(Project p) {
    Map<String, Object> config = getConfiguration(p)

    onApplyPlugin(p, config)

    p.afterEvaluate {
      p.plugins.withId("spring-cloud-contract") {
        new JavaApplicationTools().
            applySpringCloudContract(p, config.applyJunit5Support, config.springCloudContractVersion)
      }
      // We do everything on apply
    }
  }

  protected void onApplyPlugin(Project p, Map<String, Object> config) {

    def mavenTools = new MavenTools(p)

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

    def prefix = "aurora."
    def key = p.extensions.extraProperties.getProperties()
        .findAll { it.key.startsWith(prefix) }
        .collectEntries { k, v -> [k.substring(prefix.length()), v] }
    key.each { k, v -> log.debug("overrides -> ${k}:${v}") }
    config.putAll(key ?: [:])
    config.each { k, v -> log.debug("config -> ${k}:${v}") }
    config

  }

}
