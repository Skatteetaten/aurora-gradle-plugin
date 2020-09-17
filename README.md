# Aurora Gradle Plugin

[See This Section For Development](#Development)

The Aurora Gradle Plugin is a plugin that will apply a sensible yet configurable base line for JVM applications developed by the Norwegian
Tax Authority. The base line will make sure that applications will comply with most of the requirements set forth by
the Aurora team for applications that are to be deployed to the [Aurora OpenShift Platform](https://skatteetaten.github.io/aurora-openshift/).

When applying the plugin to a project it will modify the build by reacting to added plugins to make sure they are compliant with the
established standard, but it is possible to opt out of every change the plugin makes with a high degree of granularity.
So, although the Aurora team encourages every project to be built in pretty much the same way, and to some extent 
requires that static analysis is performed on the source code, it is possible to skip individual steps if the need
arises.

To get an overview of how applying the Aurora plugin will affect your project, see the [Features](#Features) section.

## Getting Started

Put the following snippet in your `~/.gradle/init.gradle` file

    allprojects {
        ext.repos= {
            mavenCentral()
            jcenter()
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
    
If you are not developing inhouse, remove the maven repositores that start with aurora/nexus. You also have to change the distributionUrl property in gradle-wrapper.properties to a public repo in order to use the gradlew command. `../gradle/wrapper/gradle-wrapper.properties`

    <...>
    distributionUrl=https\://services.gradle.org/distributions/gradle-<version>-bin.zip
    <...>

Make sure that `settings.gradle` contains
    
    rootProject.name = artifactId
    
Put the following snippet in your `gradle.properties` file

    version=local-SNAPSHOT
    groupId=no.skatteetaten.<you>.<groupId>
    artifactId=<your artifact name>    
   
If you want to configure this plugin you can do so in the `aurora` block in your build file 

    aurora {}

For a complete reference of options read through the following sections, and browse the reference at the end.

A complete example `build.gradle.kts` file can look like this

    plugins {
        id("no.skatteetaten.gradle.aurora") version "<version>"
    }
    
    dependencies {
        implementation("io.fabric8:openshift-client:4.1.2")
        implementation("org.springframework.boot:spring-boot-starter-security")
    
        testImplementation("org.springframework.security:spring-security-test")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("io.fabric8:openshift-server-mock:4.1.2")
        testImplementation("io.mockk:mockk:1.8.9")
        testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.13")
        testImplementation("com.fkorotkov:kubernetes-dsl:2.0.1")
        testImplementation("com.nhaarman:mockito-kotlin:1.6.0")
        testImplementation("com.squareup.okhttp3:mockwebserver:3.12.0")
    }
    
    aurora {
        useAuroraDefaults
    }
    
## Features

The following plugins will be reacted upon by this Aurora Plugin
 - spring-cloud-contract
 - info.solidsoft.pitest
 - com.adarshr.test-logger
 - org.sonarqube
 - org.asciidoctor.convert
 - com.github.ben-manes.versions
 - se.patrikerdes.use-latest-versions
 - com.gorylenko.gradle-git-properties
 - org.springframework.boot
 - org.springframework.cloud.contract
 - org.jetbrains.kotlin.jvm
 - org.jetbrains.kotlin.plugin.spring
 - org.jlleitschuh.gradle.ktlint
 
### Default Plugins and Repository Configuration

We bundled plugins corresponding to all use cases in this plugin, so all you have to include is this:

    plugins {
        id 'no.skatteetaten.gradle.aurora' version '<version>'
    }
    
One cavet with this approach is that type-safe accessors will not be generated for plugins applied in extensions, so for any of these you will have to use a configure block like so:

    configure<TestLoggerExtension> {}
    
as opposed to:

    testlogger {}
    
This is a [Known Issue](https://docs.gradle.org/current/userguide/kotlin_dsl.html#type-safe-accessors) in Gradle and will be fixed in a future release.

However any plugins specified with a version that differs from the compiled plugins - will be respected, and type-safe accessors will be available. As an example lets look at how you can override the version used for the kotlin jvm plugin:

    plugins {
        id 'no.skatteetaten.gradle.aurora' version '<version>'
        id 'org.jetbrains.kotlin.jvm' version '1.3.70'
    }

Since this plugin is intended for use by JVM based applications, and since the NTA heavily relies on Nexus for artifact
dependency management and artifact distribution, both the java and maven plugin will be applied automatically. The
```sourceCompatibility``` will be set to 1.8 by default. This can be changed in the `aurora` block like this:

    aurora {
        versions {
            javaSourceCompatibility = "1.6"
        }
    }

If you set the properties ```repositoryUsername```, ```repositoryPassword```, ```repositoryReleaseUrl``` and ```repositorySnapshotUrl``` in your ```~/gradle/.gradle.properties```-file the plugin will register Maven deployer for both snapshots and releases.

These features can be opted out of with the following config;

    aurora {
        features {
            mavenDeployer = false
        }
    }

### Delivery Bundle
 
The project will be set up to build the application into a format compatible with the Delivery Bundle (Leveransepakke)
format by applying the following configuration;

This will make sure that the application is packaged in a zip file with all its dependencies and that this zip
file will get the classifier Leveransepakke. Note that the default is to not generate a start script as this script,
by default, will be generated when a Docker image is produced for the Delivery Bundle. If you require a custom script
you will need to provide it itself, or configure the application plugin to generate it for you. The Leveransepakke format looks like this:

    <artifactiId>-<version>-Leveransepakke/
    <artifactiId>-<version>-Leveransepakke/metadata/<all contents of src/main/dist/metadata>
    <artifactiId>-<version>-Leveransepakke/lib/<either only fat packed spring-boot jar, or all jars>
    
You can disable this with;

    aurora {
        features {
            deliveryBundle = false
        }
    }
  
### Configuration of defaultTasks

defaultTasks will be set to `clean install` if this property has not already been set.

### Testing with Junit5

Testing with Junit5 is enabled by default, in order to turn off this feature specify

    aurora {
        features {
            junit5Support = false
        }
    }
    
### Spock for Testing

The plugin supports the [Spock Framework](http://spockframework.org/) can be turned on and configured with the following 
parameters (All versions are optional, run :auroraConfiguration to see what versions are configured in your build):

    aurora {
        features {
            applySpockSupport = true
        }
        
        versions {
            groovy = '2.5.7'
            spock = '1.3-groovy-2.5'
            cglib = '3.2.12'
            objenesis = '3.0.1'
        }
    }
    
### Asciidoc for Documentation

The Aurora plugin will react to the Asciidoc plugin

The jar task is modified to include the generated documentation into static/docs and also registers an attribute
snippetsDir at `${project.buildDir}/generated-snippets` for integration with [Spring Rest Docs](https://projects.spring.io/spring-restdocs/).

### Gradle test logger

The Aurora plugin will add the gradle test logger plugin on demand [Gradle Test Logger](https://github.com/radarsh/gradle-test-logger-plugin). It is enabled with default configuration, check docs for configuration.

    aurora {
        useGradleLogger
    }
 
### Sonar

The Aurora plugin will add the sonar plugin on demand [Sonar](https://docs.sonarqube.org/latest/analysis/scan/sonarscanner-for-gradle/). It is enabled with default configuration, check docs for configuration.

    aurora {
        useSonar
    }
 
### Ben Manes Versions

The Aurora plugin will add the ben manes versions plugin on demand [Ben Manes Versions](https://github.com/ben-manes/gradle-versions-plugin).

Check out [MiscellaneousTools](src/main/kotlin/no/skatteetaten/aurora/gradle/plugins/mutators/MiscellaneousTools.kt) to see configurations - most important of which is rejecting all non-release versions.

    aurora {
        useVersions
    }
    
### Patrikerdes Use Latest Versions

The Aurora plugin will add the use-latest-versions plugin on demand [Use Latest Versions](https://github.com/patrikerdes/gradle-use-latest-versions-plugin/blob/master/README.md).

    aurora {
        useLatestVersions
    }
    
### Gorylenko Git Properties

The Aurora plugin will add the git properties plugin on demand [Git Properties](https://github.com/n0mer/gradle-git-properties).

    aurora {
        useGitProperties
    }
 
### Source Code Analysis

The Aurora plugin will by default activate several plugins for source code analysis and test coverage. They can all
to some extent be changed or disabled by configuration.

**Checkstyle**

Checkstyle will be activated and configured to use the standard [Aurora Checkstyle configuration](https://github.com/Skatteetaten/checkstyle-config).
Errors from Checkstyle will by default not fail the build. The plugin can be configured with the following options (All versions are optional, run :auroraConfiguration to see what versions are configured in your build);

    aurora {
        features {
            checkstylePlugin = true
        }
        
        versions {
            checkstyleConfig = '2.1.7'
            checkstyleConfigFile = 'checkstyle/checkstyle-with-metrics.xml'
        }
    }
    
Other Checkstyle-parameters can be configured according to the [Checkstyle Plugin User Guide](https://docs.gradle.org/current/userguide/checkstyle_plugin.html)).
I.e.

    checkstyle {
      ignoreFailures = false
      showViolations = false
    }

**Jacoco**

By default the [jacoco plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html) will be activated. Xml is enabled, csv is disabled. Default value for xml.destination = file("${buildDir}/reports/jacoco/report.xml"). It can
be disabled with:

    aurora {
        features {
            jacocoTestReport = false
        }
    }
    
**PiTest**

If the [pitest plugin](http://gradle-pitest-plugin.solidsoft.info/) is applied it will produce reports in both HTML and XML. Apply the plugin like this, or roll your own version:
    
    aurora {
        usePitest
    }
    
### Spring 

The plugin will react to Spring plugin and modify it to produce DeliveryBundles the way we want it it. It will also add
the webmvc or webflux starters from Skatteetaten. Support for JSR310 to get dateTimes properly is also added. Config like so:

    aurora {
        useSpringBoot
    }
 
By default the plugin will include [Skatteetaten MVC Starter](https://github.com/Skatteetaten/aurora-spring-boot-mvc-starter), if you are running a webflux setup include [Skatteetaten WebFlux Starter](https://github.com/Skatteetaten/aurora-spring-boot-webflux-starter) with this configuration:

    aurora {
        useSpringBoot {
            useWebFlux
        }
    }
    
Should you choose to not use the starters or have other needs that perclude them, you can disable them like this:

    aurora {
        useSpringBoot
        
        features {
            auroraStarters = false
        }
    }
    
Remove that you can use the `versions` block within `aurora` to downgrade a version of the starter if you experience issues.

If devtools are enabled they will be included in the generated app. This instruction should be set globally
in your `~/.gradle/gradle.properties` file and be turned of in ci server. They can also be enabled on a project basis like this:

    aurora {
        features {
            springDevTools = true
        }
    }

### Kotlin
The Aurora plugin will react to Kotlin plugin and add dependencies on kotlin-reflect, stdlib-jdk8 and add 
kotlinLogging (wrapper for Logback) with the version of. Kotlin will be configured to target
java8, add `Xjsr305=strict` and suppress warnings. Version of kotlin-logging can be override like so:

    aurora {
        versions {
            kotlinLogging = 'some.version.here'
        }
    }

### Kotlin and Spring
The Aurora plugin will react to spring.kotlin plugin and the jackson Kotlin module

### Spring Cloud Contract
The Aurora plugin will react to spring cloud contract plugin and add dependencies from the bom as well as configure the 
correct packet with base class and test framework.

It will also create a proper stubs jar file based on contracts. The dependencies added are wiremock and verifier. Enable like so:

    aurora {
        useSpringBoot {
            useCloudContract
        }
    }

### Versions plugin from ben-manes
This plugin is configured to ignore snapshots, releases, miletones aso. The report will be generated in json.
    
### Config Overview

All configuration options and their default values are shown by running `:auroraConfiguration`

Complete configuration options for the `aurora` block looks like this:

    aurora { 
        useGitProperties
        useLatestVersions
        useAsciiDoctor
        useGradleLogger
        useSonar
        usePitest
        useVersions
        
        useKotlin {
            useKtLint
        }
        
        useSpringBoot {
            useWebFlux
            useBootJar
            useCloudContract
        }
        
        versions {
            javaSourceCompatibility = '<version>'
            groovy = '<version>'
            spock = '<version>'
            junit5 = '<version>'
            cglib = '<version>'
            objenesis = '<version>'
            auroraSpringBootMvcStarter = '<version>'
            auroraSpringBootWebFluxStarter = '<version>'
            springCloudContract = '<version>'
            kotlinLogging = '<version>'
            checkstyleConfig = '<version>'
            checkstyleConfigFile = '<version>'
        }
        
        features {
            defaultPlugins = '<enabled>'
            javaDefaults = '<enabled>'
            deliveryBundle = '<enabled>'
            spock = '<enabled>'
            checkstylePlugin = '<enabled>'
            jacocoTestReport = '<enabled>'
            mavenDeployer = '<enabled>'
            junit5Support = '<enabled>'
            springDevTools = '<enabled>'
        }
    }

In addition there exists the sensible default for Aurora applications. It is enabled like this:

    aurora { 
        useAuroraDefaults
    }

and corresponds to this

    aurora {
        useGitProperties
        useLatestVersions
        useVersions
        useSonar
        useGradleLogger
        useSpringBoot
        
        features {
            checkstylePlugin = true
        }
    }
    
### Included Versions

For version questions around bundled plugins you can verify it on a git tag of your choice in this file: [Dependencies](buildSrc/src/main/kotlin/Dependencies.kt)

### Configuration Reference

To see specifics on how plugins are mutated please check out our [Mutators](src/main/kotlin/no/skatteetaten/aurora/gradle/plugins/mutators)

### Development

* Run `./gradlew build publishToMavenLocal` to add the plugin to your local repository.
* Add this block to `settings.gradle` in the project in which you want to test the plugin:

    ```groovy
    pluginManagement {
        repositories {
            mavenLocal()
            gradlePluginPortal()
        }
    }
    ```

* Add `id("no.skatteetaten.gradle.aurora") version("local-SNAPSHOT")` to your `build.gradle.kts` file