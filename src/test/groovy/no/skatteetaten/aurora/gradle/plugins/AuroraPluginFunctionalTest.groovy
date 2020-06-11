package no.skatteetaten.aurora.gradle.plugins

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class AuroraPluginFunctionalTest extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            plugins {
                id 'no.skatteetaten.gradle.aurora'
            }
        """
    }

    def "sunshine test"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('aurora')
                .withPluginClasspath()
                .build()

        then:
        result.output.contains("----- Aurora Plugin Report -----")
        result.task(":aurora").outcome == SUCCESS
    }
}
