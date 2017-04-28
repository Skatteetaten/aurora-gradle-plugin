# Aurora Gradle Plugin

The Aurora Gradle Plugin is a plugin that will apply a sensible base line for JVM applications developed by the Norwegian
Tax Authority. The base line will make sure that the application will comply with most of the requirements set forth by
the Aurora team for applications that are to be deployed to the [Aurora OpenShift Platform](https://skatteetaten.github.io/aurora-openshift/).

When applying the plugin to a project it will modify the build by adding plugins and configuration based on the current
established standard, but it is possible to opt out of every change the plugin makes with a high degree of granularity.
So, although the Aurora team encourages every project to be build in pretty much the same way, and to some extent 
requires that static analysis is performed on the source code, it is possible to skip individual steps if the need
arises.

To get an overview of how applying the Aurora plugin will affect your project, see the [Features](#Features) section.

## Getting Started

Put the following snippet in your `build.gradle` file

    buildscript {
      repositories {
        maven {
          url "${nexusUrl}/content/groups/public"
        }
      }
    
      dependencies {
        classpath(
            'no.skatteetaten.aurora.gradle.plugins:aurora-gradle-plugin:1.1.0'
        )
      }
    }
    
    apply plugin: 'no.skatteetaten.plugins.aurora'

If you are developing and entirely open source module that does not have any internal dependencies you can use the
following configuration;

    buildscript {
      repositories {
        mavenCentral()
        jcenter()
      }
    
      dependencies {
        classpath(
            'no.skatteetaten.aurora.gradle.plugins:aurora-gradle-plugin:1.1.0'
        )
      }
    }
    
    apply plugin: 'no.skatteetaten.plugins.aurora'

You can configure the behaviour of the plugin by setting the ```aurora``` property before applying the plugin;

    ...
    
    ext.aurora = [
      // Configuration goes here
    ]
    
    apply plugin: 'no.skatteetaten.plugins.aurora'


## Features

### Default Plugins and Nexus Configuration

Since this plugin is intended for use by JVM based applications, and since the NTA heavily relies on Nexus for artifact
dependency management and artifact distribution, both the java and maven plugin will be applied automatically. The
```sourceCompatibility``` will be set to 1.8 by default.

    apply plugin: 'java'
    apply plugin: 'maven'
    
Also, if you have set the ```nexusUrl``` property in your ```~/gradle/.gradle.properties```-file the plugin will register
project and buildscript repositories for that Nexus instance;

    allprojects {
      buildscript {
        repositories {
          maven {
            url "${nexusUrl}/content/groups/public"
          }
        }
      }
      repositories {
        maven {
          url "${nexusUrl}/content/groups/public"
        }
      }
    }

and register a Maven deployer;

    uploadArchives {
        repositories {
            mavenDeployer {
                snapshotRepository(url: "${nexusUrl}/content/repositories/snapshots") {
                    authentication(userName: nexusUsername, password: nexusPassword)
                }
                repository(url: "${nexusUrl}/content/repositories/releases") {
                    authentication(userName: nexusUsername, password: nexusPassword)
                }
            }
        }
    }

The Maven deployer that is registered will first check if the artifact being deployed already exists before attempting
to upload to Nexus. This will prevent a failing build if you rerun a build for an existing artifact.

These features can be opted out of with the following config;

    ext.aurora = [
      applyNexusRepositories: false,
      applyDefaultPlugins: false,
      applyMavenDeployer: false,
      applyJavaDefaults: false
    ]


### Delivery Bundle
 
The project will be set up to build the application into a format compatible with the Delivery Bundle (Leveransepakke)
format by applying the following configuration;

    apply plugin: 'application'
  
    distZip.classifier = 'Leveransepakke'
    
This will make sure that the application will be packaged in a zip file with all its dependencies and that this zip
file will get the classifier Leveransepakke. Note that the default is to not generate a start script as this script,
by default, will be generated when a Docker image is produced for the Delivery Bundle. If you require a custom script
you will need to provide it itself, or configure the application plugin to generate it for you.

You can disable this with;

    ext.aurora = [
      applyDeliveryBundleConfig: false
    ]

  
### Configuration of defaultTasks

defaultTasks will be set to `clean install` if this property has not already been set.


### Versioning from Git status

The plugin will automatically set the ```version``` property based on information collected from Git. The rules used
for determining the version is described in the [aurora-git-version](https://github.com/Skatteetaten/aurora-git-version)
module.

If you have another prefix as convention for tags indicating a version, for instance ```version/``` than the
default ```v``` you can specify that using the versionPrefix config;

    ext.aurora = [
      versionPrefix: 'version/'
    ]

The prefix will always be removed from the name before determining the version.


### Nexus Staging

In the NTA it is mandatory for all released artifacts to go through a staging repository in Nexus before being released
to allow Nexus to run Nexus IQ tests. The necessary tasks for deploying to Nexus will applied by the plugin, and all
you need to do to use it them to supply the ```stagingProfileId``` configuration. You can also skip staging entirely
and deploy releases directly by setting the ```requireStaging``` configuration (but have a good reason for doing so).

    ext.aurora = [
      stagingProfileId: 'b62cca8083fa8',
      requireStaging: false
    ]

The plugin will also create a ```deploy``` task that will trigger the correct release behaviour based on the configuration
and the version of the artifact being released; SNAPSHOT-releases does not require staging.

    ./gradlew clean deploy
  

### Spock for Testing

By default, the Aurora Plugin will activate support for the [Spock Framework](http://spockframework.org/);

    apply plugin: 'groovy'

    dependencies {
      testCompile(
        "org.codehaus.groovy:groovy-all:${groovyVersion}",
        "org.spockframework:spock-core:${spockVersion}",
        "cglib:cglib-nodep:${cglibVersion}",
        "org.objenesis:objenesis:${objenesisVersion}",
      )
    }

Support for this can be disabled or configured with

    ext.aurora = [
      applySpockSupport: false,
      groovyVersion    : '2.4.4',
      spockVersion     : '1.1-groovy-2.4-rc-3',
      cglibVersion     : '3.1',
      objenesisVersion : '2.1'
    ]


### Asciidoc for Documentation

By default, the Aurora Plugin will activate support for [Asciidoc](http://www.methods.co.nz/asciidoc/) for documentation
by applying the following configuration;

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

The jar task is modified to include the generated documentation into static/docs and also registers an attribute
snippetsDir for integration with [Spring Rest Docs](https://projects.spring.io/spring-restdocs/).
 
### Source Code Analysis

The Aurora plugin will by default activate several plugins for source code analysis and test coverage. They can all
to some extent be changed or disabled by configuration.

**Checkstyle**

Checkstyle will be activated and configured to use the standard [Aurora Checkstyle configuration](https://github.com/Skatteetaten/checkstyle-config).
Errors from Checkstyle will by default not fail the build. The plugin can be configured with the following options;

    ext.aurora = [
      applyCheckstylePlugin  : true, // Should the Checkstyle plugin be activated
      checkstyleConfigVersion: "0.6", // The version of the Aurora Checkstyle config. Default 0.6.
      checkstyleConfigFile   : 'checkstyle/checkstyle-with-metrics.xml' // The Aurora Checkstyle config file to use.
    ]

Other Checkstyle-parameters can be configured according to the [Checkstyle Plugin User Guide](https://docs.gradle.org/current/userguide/checkstyle_plugin.html)).
I.e.

    checkstyle {
      ignoreFailures = false
      showViolations = false
    }

**Jacoco**

By default the [jacoco plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html) will be activated. It can
be disabled with;

    ext.aurora = [
      applyJacocoTestReport: false
    ]
    
**PiTest**

By default the [pitest plugin](http://gradle-pitest-plugin.solidsoft.info/) will be activated. Can be disabled with; 

    ext.aurora = [
      applyPiTestReport: false
    ]

**SonarQube**

By default the [SonarQube plugin](https://plugins.gradle.org/plugin/org.sonarqube) will be activated. Can be disabled with; 

    ext.aurora = [
      applySonarPlugin: false
    ]
    
    
### Config Overview

All configuration options and their default values are listed below;

    ext.aurora = [
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