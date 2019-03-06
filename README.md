# Aurora Gradle Plugin

The Aurora Gradle Plugin is a plugin that will apply a sensible base line for JVM applications developed by the Norwegian
Tax Authority. The base line will make sure that the application will comply with most of the requirements set forth by
the Aurora team for applications that are to be deployed to the [Aurora OpenShift Platform](https://skatteetaten.github.io/aurora-openshift/).

When applying the plugin to a project it will modify the build by reacting to added plugins to make sure they are according to the
established standard, but it is possible to opt out of every change the plugin makes with a high degree of granularity.
So, although the Aurora team encourages every project to be build in pretty much the same way, and to some extent 
requires that static analysis is performed on the source code, it is possible to skip individual steps if the need
arises.

To get an overview of how applying the Aurora plugin will affect your project, see the [Features](#Features) section.

## Getting Started

Put the following snippet in your `~/.gradle/init.gradle` file

    allprojects {
        ext.repos= {
            maven { url "http://aurora/nexus/content/groups/public" }
            mavenCentral()
        }
        repositories repos
        buildscript {
         repositories repos
        }
    }
    settingsEvaluated { settings ->
      settings.pluginManagement {
        repositories {
          maven {
            url 'http://aurora/nexus/content/repositories/gradle-plugins/'
          }
          gradlePluginPortal()
          maven { url "http://aurora/nexus/content/groups/public" }
        }
      }
    }
    
If you are not developing inhourse remove the maven repositores that start with aurora/nexus.

Make sure that `settings.gradle` contains
    
    rootProject.name = artifactId
    
Put the following snippet in your `gradle.properties` file

    version=local-SNAPSHOT
    groupId=no.skatteetaten.<you>.<groupId>
    artifactId= <your name>    
   

If you want to configure this plugin you can do so in the `gradle.properties` file 

    aurora.applyCheckstylePlugin=false 

For a complete reference of options look at the bottom of this file.

An complete example `build.gradle.kts` file can look like this

    plugins {
        id("org.jetbrains.kotlin.jvm") version "1.3.21"
        id("org.jetbrains.kotlin.plugin.spring") version "1.3.21"
        id("org.springframework.boot") version "2.1.3.RELEASE"
        id("org.jlleitschuh.gradle.ktlint") version "6.3.1"
        id("com.github.ben-manes.versions") version "0.20.0"
        id("com.gorylenko.gradle-git-properties") version "2.0.0"
        id("org.sonarqube") version "2.7"
        id("org.asciidoctor.convert") version "1.6.0"
        id("no.skatteetaten.gradle.aurora"k version "1.0.0"
    }
    
    dependencies {
        implementation("io.fabric8:openshift-client:4.1.2")
        implementation("org.springframework.boot:spring-boot-starter-security")
        implementation("org.springframework.boot:spring-boot-starter-web")
    
        testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
        testImplementation("org.springframework.security:spring-security-test")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("io.fabric8:openshift-server-mock:4.1.2")
        testImplementation("io.mockk:mockk:1.8.9")
        testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.13")
        testImplementation("com.fkorotkov:kubernetes-dsl:2.0.1")
        testImplementation("com.nhaarman:mockito-kotlin:1.6.0")
        testImplementation("com.squareup.okhttp3:mockwebserver:3.12.0")
    }
    

## Features

### Default Plugins and Nexus Configuration

Since this plugin is intended for use by JVM based applications, and since the NTA heavily relies on Nexus for artifact
dependency management and artifact distribution, both the java and maven plugin will be applied automatically. The
```sourceCompatibility``` will be set to 1.8 by default. This can be changed with setting the 
`aurora.javaSourceCompatibility` property. 

Also, if you have set the ```nexusUrl``` property in your ```~/gradle/.gradle.properties```-file the plugin will register a Maven deployer for both snapshots and releases

The Maven deployer that is registered will first check if the artifact being deployed already exists before attempting
to upload to Nexus. This will prevent a failing build if you rerun a build for an existing artifact.

These features can be opted out of with the following config;

    aurora.applyNexusRepositories=false
    aurora.applyMavenDeployer=false
    aurora.applyDefaultPlugins=false
    aurora.applyJavaDefaults=false

### Delivery Bundle
 
The project will be set up to build the application into a format compatible with the Delivery Bundle (Leveransepakke)
format by applying the following configuration;

This will make sure that the application will be packaged in a zip file with all its dependencies and that this zip
file will get the classifier Leveransepakke. Note that the default is to not generate a start script as this script,
by default, will be generated when a Docker image is produced for the Delivery Bundle. If you require a custom script
you will need to provide it itself, or configure the application plugin to generate it for you.

You can disable this with;

    aurora.applyDeliveryBundleConfig: false
  
### Configuration of defaultTasks

defaultTasks will be set to `clean install` if this property has not already been set.


### Testing with Junit5

Testing with Junit5 is enabled by default, in order to turn off this feature specify

    aurora.junit5Support=false
    
### Spock for Testing

The plugin supports the [Spock Framework](http://spockframework.org/) can be turned on and configured with the following 
parameters:

    aurora.applySpockSupport=true
    aurora.groovyVersion    = '2.5.4',
    aurora.spockVersion     = '1.2-groovy-2.5'
    aurora.cglibVersion     = '3.1'
    aurora.objenesisVersion = '2.1'


### Asciidoc for Documentation

The Aurora plugin will react to the Asciidoc plugin

The jar task is modified to include the generated documentation into static/docs and also registers an attribute
snippetsDir for integration with [Spring Rest Docs](https://projects.spring.io/spring-restdocs/).
 
### Source Code Analysis

The Aurora plugin will by default activate several plugins for source code analysis and test coverage. They can all
to some extent be changed or disabled by configuration.

**Checkstyle**

Checkstyle will be activated and configured to use the standard [Aurora Checkstyle configuration](https://github.com/Skatteetaten/checkstyle-config).
Errors from Checkstyle will by default not fail the build. The plugin can be configured with the following options;

    aurora.applyCheckstylePlugin  : true, // Should the Checkstyle plugin be activated
    aurora.checkstyleConfigVersion: "2.1.6", // The version of the Aurora Checkstyle config. Default 2.1.6.
    aurora.checkstyleConfigFile   : 'checkstyle/checkstyle-with-metrics.xml' // The Aurora Checkstyle config file to use.

Other Checkstyle-parameters can be configured according to the [Checkstyle Plugin User Guide](https://docs.gradle.org/current/userguide/checkstyle_plugin.html)).
I.e.

    checkstyle {
      ignoreFailures = false
      showViolations = false
    }

**Jacoco**

By default the [jacoco plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html) will be activated. It can
be disabled with;

    aurora.applyJacocoTestReport=false
    
**PiTest**

If the [pitest plugin](http://gradle-pitest-plugin.solidsoft.info/) is applied it will produce reports in  both HTML and XML
    
### Spring 

The plugin will react to Spring plugin and modify it to produce DeliveryBundles the way we want it it. It will also add
the auroraSpringBootStarter as a dependency using the version specified in `aurora.auroraSpringBootStarterVersion`. 
Support for JSR310 to get dateTimes properly is also added

### Kotlin
The Aurora plugin will react to kotlin plugin and add dependencies on kotlin-reflect, stdlib-jdk8 and add 
kotlinLogging (wrapper for Logback) with the version of `aurora.kotlinLoggingVersion`. Kotln will be configured to target
java8, add `Xjsr305=strict` and supress warnings

### Kotlin and Spring
The Aurora plugin will react to spring.kotlin plugin and the jackson kotlin module

### Spring Cloud Contract
The Aurora plugin will react to spring cloud contract plugin and add dependencies from the bom as well as configure the 
correct packe with base class and test framework.

    
### Config Overview

All configuration options and their default values are listed below;

    aurora.applyDefaultPlugins           = true,
    aurora.applyJavaDefaults             = true,
    aurora.javaSourceCompatibility       = "1.8",
    aurora.applyDeliveryBundleConfig     = true,
    
    aurora.applySpockSupport             = false,
    aurora.groovyVersion                 = '2.5.4',
    aurora.spockVersion                  = '1.2-groovy-2.5',
    aurora.cglibVersion                  = '3.1',
    aurora.objenesisVersion              = '2.1',
    
    aurora.applyCheckstylePlugin         = true,
    aurora.checkstyleConfigVersion       = "2.1.6",
    aurora.checkstyleConfigFile          = 'checkstyle/checkstyle-with-metrics.xml',
    
    aurora.applyJacocoTestReport         = true,
    aurora.applyMavenDeployer            = true,
    
    aurora.requireStaging                = false,
    aurora.stagingProfileId              = null,
    
    aurora.springCloudContractVersion    : "2.1.0.RELEASE",
    aurora.auroraSpringBootStarterVersion= "2.0.0",
    aurora.kotlinLoggingVersion          = "1.6.24",
    aurora.applyJunit5Support            = true
    