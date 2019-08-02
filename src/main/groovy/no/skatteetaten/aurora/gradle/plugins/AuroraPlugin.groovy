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
      auroraSpringBootStarterVersion: "2.2.1",
      springCloudContractVersion    : "2.1.1.RELEASE",
      stagingProfileId              : null,
      kotlinLoggingVersion          : "1.6.26",
      kotlinTestVersion             : "1.3.30",
      checkstyleConfigVersion       : "2.1.6",
      checkstyleConfigFile          : 'checkstyle/checkstyle-with-metrics.xml',
      applyJunit5Support            : true,
      springDevTools : false
  ]

  void apply(Project p) {
    Map<String, Object> config = getConfiguration(p)

    List<AuroraReport> reports = []
    def mavenTools = new MavenTools(p)
    def tools = new CodeAnalysisTools(p)
    def java = new JavaApplicationTools(p)

    p.afterEvaluate {
      log.info("After evaluate")

      mavenTools.setDefaultTasks()

      if (config.applyJavaDefaults.toBoolean()) {
        reports.add(java.applyJavaDefaults(config.javaSourceCompatibility))
      }

      if (config.applyDefaultPlugins.toBoolean()) {
        reports.add(java.applyDefaultPlugins())
      }

      p.plugins.withId("org.asciidoctor.convert") {
        reports.add(java.applyAsciiDocPlugin())
      }

      p.plugins.withId("com.github.ben-manes.versions") {
        reports.add(java.applyVersions())
      }

      if (config.applyDeliveryBundleConfig.toBoolean()) {
        reports.add(java.applyDeliveryBundleConfig())
      }

      p.plugins.withId("org.springframework.boot") {
        reports.add(java.applySpring(config.auroraSpringBootStarterVersion, config.springDevTools.toBoolean()))
      }

      if (config.applyJunit5Support.toBoolean()) {
        reports.add(java.applyJunit5())
      }

      p.plugins.withId("org.jetbrains.kotlin.jvm") {
        reports.add(java.applyKotlinSupport(config.kotlinLoggingVersion))
      }

      p.plugins.withId("org.jetbrains.kotlin.plugin.spring") {
        reports.add(java.applyKotlinSpringSupport())
      }

      if (config.applyCheckstylePlugin.toBoolean()) {
        reports.add(tools.
            applyCheckstylePlugin(config.checkstyleConfigVersion as String, config.checkstyleConfigFile as String))
      }

      if (config.applyJacocoTestReport.toBoolean()) {
        reports.add(tools.applyJacocoTestReport())
      }

      p.plugins.withId("info.solidsoft.pitest") {
        reports.add(tools.applyPiTestSupport())
      }

      if (config.applyMavenDeployer) {
        log.info("Apply maven deployer")
        reports.add(mavenTools.addMavenDeployer(config.requireStaging, config.stagingProfileId))
      }

      p.plugins.withId("spring-cloud-contract") {
        reports.add(java.
            applySpringCloudContract(config.applyJunit5Support.toBoolean(), config.springCloudContractVersion))
      }

      p.plugins.withId("org.jlleitschuh.gradle.ktlint"){
        reports.add(java.applyKtLint())
      }

      if (config.applySpockSupport.toBoolean()) {
        log.info("SPOCK support")
        reports.add(java.applySpockSupport(config.groovyVersion, config.spockVersion, config.cglibVersion,
            config.objenesisVersion))
      }

      p.with {

        tasks.register("aurora") {
          doLast {
            printReport(reports)
          }
        }
      }

      log.lifecycle("Use task :aurora to get full report on how AuroraPlugin modify your gradle setup")

    }
  }

  private printReport(List<AuroraReport> reports) {
    if (!reports.isEmpty()) {

      reports.each {
        println(it)
      }
      def sortedReport=reports.sort { it.name}
      log.lifecycle("----- Aurora Plugin Report -----")
      log.lifecycle("The aurora plugin can be configured via aurora.* feature flags in .gradle.properties or reacting on applied plugins.\n")
      log.lifecycle("Each feature can add dependencies to your build, add another plugin or modify configuration\n")
      sortedReport.each { log.lifecycle(it.toString() + "\n") }
      log.lifecycle("--------------------------------")
    }

  }

  private static Map<String, Object> getConfiguration(Project p) {

    def config = [:]
    config.putAll(DEFAULT_CONFIG)

    def prefix = "aurora."
    def key = p.extensions.extraProperties.getProperties()
        .findAll { it.key.startsWith(prefix) }
        .collectEntries { k, v -> [k.substring(prefix.length()), v] }
    key.each { k, v -> log.info("overrides -> ${k}:${v}") }
    config.putAll(key ?: [:])
    config.each { k, v -> log.info("config -> ${k}:${v}") }
    config

  }

}
