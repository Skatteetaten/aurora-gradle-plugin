package no.skatteetaten.aurora.gradle.plugins

import java.text.MessageFormat

import org.gradle.api.Project

class NexusTools {

  /**
   * Determines if the artifact represented by the specified object already exists in nexus
   * @param p
   * @param releasesUrl
   * @return
   */
  static boolean artifactExists(Project p, String nexusUrl) {
    def releasesUrl = "${nexusUrl}/content/repositories/releases"
    def artifactUrl = determineArtifactUrl(releasesUrl, p.group as String, p.name, p.version as String)
    try {
      new URL(artifactUrl).bytes
      p.logger.warn("Artifact $p.group:$p.name:$p.version already exist in nexus ($releasesUrl).")
      true
    } catch (FileNotFoundException e) {
      false
    }
  }

  /**
   * Resolves the absolute url to a nexus artifact
   * @param repositoryUrl
   * @param groupId
   * @param artifactId
   * @param version
   * @return
   */
  static determineArtifactUrl(String repositoryUrl, String groupId, String artifactId, String version) {

    if ([repositoryUrl, groupId, artifactId, version].any { !it }) {
      throw new IllegalArgumentException(
          "All parameters must be set, was: ${[repositoryUrl, groupId, artifactId, version]}")
    }
    // Replace dots with slashes in the groupId (ske.aurora.gradle.plugins => ske/aurora/gradle/plugins)
    String groupIdAdapted = groupId.replaceAll(/\./, '/')
    // Remove the last slash in repositoryUrl (if any)
    String repositoryUrlAdapted = repositoryUrl.replaceAll(/\/$/, '')

    MessageFormat urlFormat = new MessageFormat("{0}/{1}/{2}/{3}/{2}-{3}.pom");
    urlFormat.format([repositoryUrlAdapted, groupIdAdapted, artifactId, version].toArray())
  }
}
