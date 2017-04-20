package no.skatteetaten.aurora.gradle.plugins

import java.text.SimpleDateFormat

import org.ajoberstar.grgit.Grgit
import org.gradle.api.Plugin
import org.gradle.api.Project

import groovy.util.logging.Slf4j

@Slf4j
class AuroraPlugin implements Plugin<Project> {

  private static DEFAULT_CONFIG = [
      applyDefaultPlugins       : true,
      applyJavaDefaults         : true,
      applyDeliveryBundleConfig : true,
      applySpockSupport         : true,
      groovyVersion             : '2.4.4',
      spockVersion              : '1.1-groovy-2.4-rc-3',
      cglibVersion              : '3.1',
      objenesisVersion          : '2.1',
      applyAsciiDocPlugin       : true,
      applyCheckstylePlugin     : true,
      applyJacocoTestReport     : true,
      applyPiTestSupport        : true,
      applySonarPlugin          : true,
      setProjectVersionFromGit  : true,
      enforceTagOnMaster        : true,
      versionPrefix             : 'v',
      fallbackToTimestampVersion: true,
      applyNexusRepositories    : true,
      applyMavenDeployer        : true,
      checkstyleConfigVersion   : "0.6",
      checkstyleConfigFile      : 'checkstyle/checkstyle-with-metrics.xml'
  ]

  void apply(Project p) {

    Map<String, Object> config = getConfiguration(p)

    onApplyPlugin(p, config)

    p.afterEvaluate {
      // We do everything on apply
    }
  }

  protected void onApplyPlugin(Project p, Map<String, Object> config) {

    Grgit git = openGit()
    if (!git) {
      setProjectVersionFromTimestamp(p, config)
    } else {
      setProjectVersionFromGit(p, git, config)
    }

    def mavenTools = new MavenTools(p)
    if (config.applyNexusRepositories) {
      mavenTools.applyRepositories()
    }

    new JavaApplicationTools(p).applyJavaApplicationConfig(config)
    new CodeAnalysisTools(p).applyCodeAnalysisPlugins(config)

    mavenTools.with {
      if (config.applyMavenDeployer) {
        addMavenDeployer()
      }
      setDefaultTasks()
    }
  }

  private static Map<String, Object> getConfiguration(Project p) {

    def config = [:]
    config.putAll(DEFAULT_CONFIG)
    config.putAll(p.ext.properties.aurora ?: [:])
    config
  }

  private static Grgit openGit() {

    Grgit git = null
    try {
      git = Grgit.open(dir: ".")
    } catch (Exception e) {
      log.warn("Unable to read git repository information. Details: $e.message")
    }
    git
  }

  private static setProjectVersionFromGit(Project p, Grgit git, Map<String, Object> config) {

    new GitTools(p, git).with {
      enforceTagOnMaster = config.enforceTagOnMaster
      versionPrefix = config.versionPrefix

      if (config.setProjectVersionFromGit) {
        setProjectVersionFromGit()
      }
    }
  }

  private static setProjectVersionFromTimestamp(Project p, Map<String, Object> config) {

    if (!config.fallbackToTimestampVersion) {
      return
    }

    p.logger.info("Falling back to timestamp version")
    String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
    ProjectTools.setProjectVersion(p, timestamp)
  }
}
