package no.skatteetaten.aurora.gradle.plugins

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class AuroraPluginArtifactsFunctionalTest extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            plugins {
                id 'no.skatteetaten.gradle.aurora'
                id 'org.springframework.boot' version '2.3.2.RELEASE'
            }
            
            repositories {
              jcenter()
              mavenCentral()
            }
        """
    }

    def "verify only one artifact"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build')
                .withPluginClasspath()
                .build()

        then:
        result.task(":build").outcome == SUCCESS
        testProjectDir.getRoot()
                .listFiles().find {it.path.endsWith("build") }
                .listFiles().find {it.path.endsWith("libs") }
                .listFiles().length == 1
    }
}
