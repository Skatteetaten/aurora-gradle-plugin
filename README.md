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
            'no.skatteetaten.aurora.gradle.plugins:aurora-gradle-plugin:1.0.0'
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
            'no.skatteetaten.aurora.gradle.plugins:aurora-gradle-plugin:1.0.0'
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

  
### Configuration of defaultTasks

defaultTasks will be set to `clean install` if this property has not already been set.


### Versioning from Git status

The plugin will automatically set the ```version``` property based on information collected from Git. The rules used
for determining the version is described in the [aurora-git-version](https://github.com/Skatteetaten/aurora-git-version)
module.


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
      applySpockSupport         : false,
      groovyVersion             : '2.4.4',
      spockVersion              : '1.1-groovy-2.4-rc-3',
      cglibVersion              : '3.1',
      objenesisVersion          : '2.1'
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

Aurora plugin vil som standard aktivere Gradle Checkstyle plugin og bruke standard Aurora Checkstyle-konfigurasjon. 

Feil fra Checkstyle vil som standard ikke avbryte bygget. Checkstyle-konfigurasjon blir hentet fra Maven artefakt `group: ske.aurora.checkstyle, name: checkstyle-config, version: 0.6`. 

Man kan gjøre tilpasninger ved å legge til følgende properties i konfigurasjonsblokken: 

* `applyCheckstylePlugin` - Angir om Aurora plugin skal aktivere Gradle Checkstyle plugin. Default: `true`.
* `checkstyleConfigVersion` - Versjon av artefakt checkstyle-config. Default: `0.6`.
* `checkstyleConfigFile` - Checkstyle-konfigurasjonsfil relativt til artefakt `checkstyle-config-0.6.jar`. Default: `checkstyle/checkstyle-with-metrics.xml`.

Øvrige Checkstyle-parametere kan man angi på vanlig måte i `build.gradle` (jfr [Checkstyle Plugin User Guide](https://docs.gradle.org/current/userguide/checkstyle_plugin.html)).

For eksempel: 

    checkstyle {
      ignoreFailures = false
      showViolations = false
    }

 
## Konfigurasjon

Du kan skru av individuelle features ved å legge inn en aurora konfigurasjonsblokk i byggescriptet ditt. Merk at konfigurasjonen skal stå før apply-statementet for pluginet.

    ...
    
    ext.aurora = [
      exportHeadMetaData        : false,
      setProjectVersionFromGit  : false,
      enforceTagOnMaster        : false,
      fallbackToTimestampVersion: false,
      applyNexusRepositories    : false,
      applyMavenDeployer        : false,
      setIgnoreTestFailures     : false
      applyCheckstylePlugin     : false
    }

    apply plugin: 'ske.plugins.aurora'
    

Dersom du har som konvensjon å ha et prefix foran tag navn som brukes for å indikere versjoner, f.eks. v1.0.0 eller version/1.0.0, kan du spesifisere dette med

    ext.aurora = [
        versionPrefix: 'version/'
    ]

Prefixen blir da fjernet fra tagnavnet før det brukes som versjon (f.eks. tagnavn version/1.0.0 => 1.0.0)


## Release notes

### 2.0.0 (2016.12.21)

* Endring - Bruker aurora-git-version biblioteket for å utlede versjon fra git

### 1.7.1 (2016.12.20)

* Bugfix - Endret defaultverdi for setIgnoreTestFailures til false


### 1.7.0 (2016.09.19)

* Feature - Kan nå falle tilbake på versjon basert på timestamp dersom prosjektet bygges uten .git mappe


### 1.6.1 (2016.08.22)

* Ingen funksjonelle endringer. Bare justeringer på bygg.


### 1.6.0 (2016.08.19)

* Feature - Sjekker nå om en artifact allerede eksisterer på nexus før opplasting


### 1.5.1 (2016.08.18)

* Bugfix - Fikset en feil som påvirket bygg uten test task


### 1.5.0 (2016.08.17)

* Feature - Pluginet setter nå automatisk `test.ignoreFailures = true` for at feilende bygg skal fungere bedre på Jenkins


### 1.4.0 (2016.08.17)

* Feature - Bygget vil nå feile dersom man bygger fra master uten å være på en tag


### 1.3.2 (2016.08.16)

* Endring - Endret rekkefølge på håndtering av BRANCH_NAME miljøvariabel


### 1.3.1 (2016.08.16)

* Bugfix - Fikset problem med at remote brancher ikke ble søkt i i detached head mode


### 1.3.0 (2016.08.15)

* Feature - Dersom man bygger fra Git detached HEAD så vil pluginet nå prøve finne hvilken branch denne committen er på.


### 1.2.0 (2016.08.02)

* Feature - defaultTasks settes nå til `clean install` dersom denne propertyen ikke allerede er satt og det finnes minst én modul som bruker maven pluginet.


### 1.1.0 (2016.06.28)

* Endring - Konfigurasjon gjøres nå via en egen project property istedenfor en plugin extension (for å kunne spesifisere konfigurasjon før pluginet applyes)
* Endring - Default prefix på versions-tag er nå 'v' (var blank)
* Bugfix - Version propertyen er nå satt rett etter at apply plugin statementet er kjørt.
