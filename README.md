# Aurora Gradle Plugin

The contents of the README.md file will be translated into English soon.

Aurora Gradle Plugin er et gradle plugin som gjør det lettere å sette opp prosjekter som integrerer med Aurora tjenester (som Nexus) og følge konvensjoner etablert av Aurora (primært i forhold til
versjonering av artifakter).


## Kom i gang

Legg følgende i toppen av din `build.gradle` fil (i rotprosjektet hvis du har et multimodul bygg).

    buildscript {
        repositories {
            maven {
                url "http://aurora/nexus/content/groups/public"
            }
        }
    
        dependencies {
            classpath 'ske.aurora.gradle.plugins:aurora-gradle-plugin:1.7.0'
        }
    }
    
    apply plugin: 'ske.plugins.aurora'


Eventuelt kan du bytte ut maven-blokken med følgende

    ...
    maven {
        url "${nexusUrl}/content/groups/public"
    }
    ...

hvis du har konfigurert nexusUrl i din `~/.gradle/gradle.properties` fil.


## Funksjonalitet

### Eksport av git metadata til prosjekt properties 

Følgende variable blir hentet ut fra git og eksportert som project properties (som da kan brukes som parametre til andre plugins eller tasks)

 * revision (hashen til head)
 * lastUpdateMessage (heads commit message)
 * lastUpdateBy (epost til brukeren som utførte siste commit)


### Konfigurasjon av repositories

Pluginet vil legge til Aurora Nexus som buildscript repository og project repository for rotprosjektet og alle subprosjekter.


### Konfigurasjon av maven deployer

I moduler som har `maven plugin` aktivert konfigureres maven deployeren til å deploye mot Aurora Nexus automatisk. Typisk betyr dette at følgende konfigurasjon legges til av pluginet;

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
    
I tillegg til dette vil pluginet undersøke om artifacten du forsøker laste opp allerede eksisterer på nexus og i så fall hoppe over opplasting (for å unngå feilende bygg).


### Konfigurasjon av defaultTasks

defaultTasks på rotprosjektet settes til `clean install` dersom denne propertyen ikke allerede er satt og det finnes minst én modul som bruker maven pluginet.


### Versjonering basert på git-status

Pluginet vil lese informasjon fra git og sette version-property'en basert på følgende regler:

 * Dersom du står på en tag vil denne taggens navn (minus prefix, default 'v') brukes som versjon. F.eks. bygger du fra taggen `v1.0.0` settes prosjektversjon til `1.0.0`.
 * Dersom du står på en commit som ikke er tagget brukes navnet på branchen + -SNAPSHOT som versjon. F.eks. står du på `master` blir versjonen `master-SNAPSHOT`. Står du på `feature/PRJ-8-en-eller-annen-feature` blir versjonen `feature_PRJ_8_en_eller_annen_feature-SNAPSHOT`.
 * Dersom man bygger fra Git detached HEAD så vil pluginet prøve finne hvilken branch denne committen er på. Dersom committen finnes på flere brancher kan man sette miljøvariabelen `BRANCH_NAME` for avgjøre hvilken branch som skal brukes. Merk at `BRANCH_NAME` må inneholde committen det bygges fra.
 * Dersom du bygger fra master uten å stå på en tag vil bygget feile. Dette kan skrus av via konfigurasjonen `enforceTagOnMaster`.
 * Dersom du bygger prosjektet fra en eksportert mappe (uten .git-mappe) er default å falle tilbake på versjon basert på timestamp (yyyyMMddHHss). Denne oppførselen kan deaktiveres med konfigurasjonen `fallbackToTimestampVersion`.

 
### Tilpasninger for CI (Jenkins)

Mange CI systemer, blant annet Jenkins ved bruk av Jenkinsfile, vil feile bygget dersom byggekommandoen feiler. Dermed blir ikke øvrige steg i bygget utført, som f.eks. testrapporter o.l. For å unngå dette kan man i gradle
sette `test.ignoreFailures`. Da vil bygget lykkes selv om testene feiler. Jenkins vil likevel klare å avgjøre at det er testfeil. For å unngå å måtte sette denne propertyen hver gang i alle bygg gjør dette pluginet det
automatisk. I praksis legger den til blokken;

    test {
        ignoreFailures = true
    }
    
Dersom du ikke ønsker denne funksjonaliteten kan du skru av ved å sette `setIgnoreTestFailures` i configblokken.


### Checkstyle

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
