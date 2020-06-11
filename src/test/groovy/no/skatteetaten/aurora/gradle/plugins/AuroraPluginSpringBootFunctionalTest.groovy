package no.skatteetaten.aurora.gradle.plugins

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class AuroraPluginSpringBootFunctionalTest extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            plugins {
                id 'no.skatteetaten.gradle.aurora'
                id 'org.springframework.boot' version '2.3.0.RELEASE'
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

    def "mvc test"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('aurora')
                .withPluginClasspath()
                .build()

        then:
        result.output.contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-mvc-starter")
        !result.output.contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-webflux-starter")
        result.task(":aurora").outcome == SUCCESS
    }

    def "webflux test"() {
        File gradleProps = testProjectDir.newFile('gradle.properties')
        gradleProps << """aurora.useWebFlux=true"""

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('aurora')
                .withPluginClasspath()
                .build()

        then:
        !result.output.contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-mvc-starter")
        result.output.contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-webflux-starter")
        result.output.contains("webflux enabled and webmvc + tomcat excluded")
        result.task(":aurora").outcome == SUCCESS
    }

    def "bootJar test"() {
        File gradleProps = testProjectDir.newFile('gradle.properties')
        gradleProps << """aurora.useBootJar=true"""

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('aurora')
                .withPluginClasspath()
                .build()

        then:
        result.output.contains("no.skatteetaten.aurora.springboot:aurora-spring-boot-mvc-starter")
        result.output.contains("bootJar enabled")
        !result.output.contains("bootJar disabled")
        result.task(":aurora").outcome == SUCCESS
    }
}
