package no.skatteetaten.aurora.gradle.plugins

import spock.lang.Specification

class MavenToolsTest extends Specification {

  def "Determine artifact url"() {

    expect:
      MavenTools.determineArtifactUrl(repoUrl, groupId, artifactId, version) == expectedUrl

    where:
      repoUrl                                              | groupId                     | artifactId             | version | expectedUrl
      "http://aurora/nexus/content/repositories/releases"  | "ske.aurora.gradle.plugins" | "aurora-gradle-plugin" | "1.5.1" | 'http://aurora/nexus/content/repositories/releases/ske/aurora/gradle/plugins/aurora-gradle-plugin/1.5.1/aurora-gradle-plugin-1.5.1.pom.md5'
      "http://aurora/nexus/content/repositories/releases/" | "ske.aurora.gradle.plugins" | "aurora-gradle-plugin" | "1.5.1" | 'http://aurora/nexus/content/repositories/releases/ske/aurora/gradle/plugins/aurora-gradle-plugin/1.5.1/aurora-gradle-plugin-1.5.1.pom.md5'
  }

  def "Determine artifact url with illegal args"() {

    when:
      MavenTools.determineArtifactUrl("http://aurora/nexus/content/repositories/releases", null, "aurora-gradle-plugin", null)
    then:
      thrown(IllegalArgumentException)
  }
}
