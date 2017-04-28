package no.skatteetaten.aurora.gradle.plugins

import spock.lang.Specification

class NexusToolsTest extends Specification {

  def "Determine artifact url"() {

    expect:
      NexusTools.determineArtifactUrl(repoUrl, groupId, artifactId, version) == expectedUrl

    where:
      repoUrl                                              | groupId                     | artifactId             | version | expectedUrl
      "http://aurora/nexus/content/repositories/releases"  | "ske.aurora.gradle.plugins" | "aurora-gradle-plugin" | "1.5.1" | 'http://aurora/nexus/content/repositories/releases/ske/aurora/gradle/plugins/aurora-gradle-plugin/1.5.1/aurora-gradle-plugin-1.5.1.pom'
      "http://aurora/nexus/content/repositories/releases/" | "ske.aurora.gradle.plugins" | "aurora-gradle-plugin" | "1.5.1" | 'http://aurora/nexus/content/repositories/releases/ske/aurora/gradle/plugins/aurora-gradle-plugin/1.5.1/aurora-gradle-plugin-1.5.1.pom'
  }

  def "Determine artifact url with illegal args"() {

    when:
      NexusTools.determineArtifactUrl("http://aurora/nexus/content/repositories/releases", null, "aurora-gradle-plugin", null)
    then:
      thrown(IllegalArgumentException)
  }
}
