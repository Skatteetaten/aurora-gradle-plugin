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

  void addMavenDeployer() {

    if (!(project.ext.has("nexusUsername") && project.ext.has("nexusPassword"))) {
      return
    }

    Set<Project> projectsWithMavenPlugin = findAllMavenProjects()
    projectsWithMavenPlugin.each {
      it.with { p ->
        def releasesUrl = "${nexusUrl}/content/repositories/releases"
        def stagingUrl = "${nexusUrl}/service/local/staging/deploy/maven2/"
        def snapshotUrl = "${nexusUrl}/content/repositories/snapshots"
        uploadArchives {
          onlyIf { !artifactExists(p, releasesUrl) }
          repositories {
            mavenDeployer {
              snapshotRepository(url: snapshotUrl) {
                authentication(userName: nexusUsername, password: nexusPassword)
              }

              repository(url: stagingUrl) {
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
    if (findAllMavenProjects().empty) {
      // Don't set defaultTasks if there are no maven projects because we are going to set it
      // to tasks that require the maven plugin to be applied.
      return
    }

    // defaultTasks only need to be set on the root project
    project.defaultTasks = ['clean', 'install']
  }

  private Set<Project> findAllMavenProjects() {

    project.allprojects.findAll { it.plugins.hasPlugin("maven") }
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