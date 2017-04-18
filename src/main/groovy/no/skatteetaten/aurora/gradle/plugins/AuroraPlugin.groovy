package no.skatteetaten.aurora.gradle.plugins

import java.text.SimpleDateFormat

import org.ajoberstar.grgit.Grgit
import org.gradle.api.Plugin
import org.gradle.api.Project

import groovy.util.logging.Slf4j

@Slf4j
class AuroraPlugin implements Plugin<Project> {

  private static DEFAULT_CONFIG = [
      setProjectVersionFromGit  : true,
      enforceTagOnMaster        : true,
      versionPrefix             : 'v',
      fallbackToTimestampVersion: true,
      applyNexusRepositories    : true,
      applyMavenDeployer        : true,
      setIgnoreTestFailures     : false,
      applyCheckstylePlugin     : true,
      checkstyleConfigVersion   : 0.6,
      checkstyleConfigFile      : 'checkstyle/checkstyle-with-metrics.xml'
  ]

  void apply(Project p) {

    Map<String, Object> config = getConfiguration(p)

    onApplyPlugin(p, config)

    p.afterEvaluate {
      onAfterEvaluate(p, config)
    }
  }

  protected void onApplyPlugin(Project p, Map<String, Object> config) {

    applyDefaultPlugins(p)
    applyJavaDefaults(p)
    applyGroovySupport(p)
    applyAsciiDocPlugin(p)

    Grgit git = openGit()
    if (!git) {
      handleGitNotAvailable(p, config)
    } else {
      handleConfigurationFromGit(p, git, config)
    }
  }

  protected void onAfterEvaluate(Project p, Map<String, Object> config) {

    new MavenTools(p).with {
      if (config.applyNexusRepositories) {
        applyRepositories()
      }
      if (config.applyMavenDeployer) {
        addMavenDeployer()
      }
      setDefaultTasks()
    }

    applyDeliveryBundleConfig(p)

    if (config.applyCheckstylePlugin) {
      applyCheckstylePlugin(p, config)
    }
    applyJacocoTestReport(p)
    applyPiTestSupport(p)
    applySonarPlugin(p)

    if (p.hasProperty('test') && config.setIgnoreTestFailures) {
      p.test {
        ignoreFailures = true
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

  void applyGroovySupport(Project project) {

    project.with {
      apply plugin: 'groovy'

      dependencies {
        testCompile("org.codehaus.groovy:groovy-all:2.4.4")
      }
    }
  }

  void applyDeliveryBundleConfig(Project project) {

    project.with {
      apply plugin: 'application'

      distZip.classifier = 'Leveransepakke'
    }
  }

  void applyJacocoTestReport(Project project) {
    project.with {
      apply plugin: "jacoco"

      jacocoTestReport {
        reports {
          xml.enabled false
          csv.enabled false
          html.destination "${buildDir}/reports/jacoco"
        }
      }
    }
  }

  void applyPiTestSupport(Project project) {
    project.with {
      apply plugin: "info.solidsoft.pitest"

      pitest {
        outputFormats = ['XML', 'HTML']
      }
    }
  }

  void applySonarPlugin(Project project) {
    project.with {
      apply plugin: 'org.sonarqube'
    }
  }

  void applyAsciiDocPlugin(Project project) {

    def asciiDoctorPluginOnClasspath = project.buildscript
        .configurations.find { it.name == 'classpath' }
        .dependencies.any { it.group == 'org.asciidoctor' && it.name == 'asciidoctor-gradle-plugin' }
    if (!asciiDoctorPluginOnClasspath) {
      return
    }
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

  private static handleConfigurationFromGit(Project p, Grgit git, Map<String, Object> config) {

    new GitTools(p, git).with {
      enforceTagOnMaster = config.enforceTagOnMaster
      versionPrefix = config.versionPrefix

      if (config.setProjectVersionFromGit) {
        setProjectVersionFromGit()
      }
    }
  }

  private static handleGitNotAvailable(Project p, Map<String, Object> config) {

    if (!config.fallbackToTimestampVersion) {
      return
    }

    p.logger.info("Falling back to timestamp version")
    String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
    ProjectTools.setProjectVersion(p, timestamp)
  }

  /**
   * Create a new configuration, "auroraCheckstyleConfig", and then add the Aurora Checkstyle configuration dependency
   * to this configuration.
   * Apply the Checkstyle plugin and assign the specified configuration file from Aurora Checkstyle configuration 
   * archive.
   *
   * @param project
   * @param pluginConfig
   */
  private void applyCheckstylePlugin(Project project, Map<String, Object> auroraPluginProperties) {
    def auroraCheckstyleConfig = project.configurations.create("auroraCheckstyleConfig")

    auroraCheckstyleConfig.defaultDependencies { dependencies ->
      dependencies.add(project.dependencies.create("ske.aurora.checkstyle:checkstyle-config:"
          + auroraPluginProperties.checkstyleConfigVersion))
    }

    project.plugins.apply("checkstyle")
    project.checkstyle.config = project.resources.text.fromArchiveEntry(auroraCheckstyleConfig,
        auroraPluginProperties.checkstyleConfigFile)
    project.checkstyle.ignoreFailures = true
  }
}
