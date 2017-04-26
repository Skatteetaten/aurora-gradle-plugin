package no.skatteetaten.aurora.gradle.plugins

import java.text.MessageFormat

import org.gradle.api.Project

class MavenTools {

  Project project

  MavenTools(Project p) {

    this.project = p
  }

  void applyRepositories() {

    if (!project.hasProperty("nexusUrl")) {
      return
    }

    project.with {

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
    }
  }

  void addMavenDeployer(boolean requireStaging = true, String stagingProfileId = null) {

    if (!(project.ext.has("nexusUsername") && project.ext.has("nexusPassword"))) {
      return
    }
    if (requireStaging) {
      if (!stagingProfileId) {
        throw new IllegalArgumentException("Required stagingProfileId property not set")
      }
      NexusStagingTools.addNexusStagingTasks(project, stagingProfileId)
    }
    project.with {

//      def stagingUrl = "${nexusUrl}/service/local/staging/deploy/maven2/"
      def releasesUrl = "${nexusUrl}/content/repositories/releases"
      def snapshotUrl = "${nexusUrl}/content/repositories/snapshots"
      uploadArchives {
        onlyIf { !artifactExists(p, releasesUrl) }
        repositories {
          mavenDeployer {
            snapshotRepository(url: snapshotUrl) {
              authentication(userName: nexusUsername, password: nexusPassword)
            }
            if (!requireStaging) {
              // If we don't require staging to nexus we can just add a standard release deployer
              repository(url: releasesUrl) {
                authentication(userName: nexusUsername, password: nexusPassword)
              }
            }
          }
        }
      }
    }
  }

  void setDefaultTasks() {

    if (project.defaultTasks) {
      // Don't set defaultTasks if it has already been set
      return
    }

    // defaultTasks only need to be set on the root project
    project.defaultTasks = ['clean', 'install']
  }

  /**
   * Determines if the artifact represented by the specified object already exists in nexus
   * @param p
   * @param releasesUrl
   * @return
   */
  static boolean artifactExists(Project p, String releasesUrl) {
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

    MessageFormat urlFormat = new MessageFormat("{0}/{1}/{2}/{3}/{2}-{3}.pom.md5");
    urlFormat.format([repositoryUrlAdapted, groupIdAdapted, artifactId, version].toArray())
  }
}