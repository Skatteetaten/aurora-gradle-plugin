package no.skatteetaten.aurora.gradle.plugins

import java.text.MessageFormat

import org.gradle.api.Project

class MavenTools {

  Project project

  MavenTools(Project p) {

    this.project = p
  }

  void addMavenDeployer(boolean requireStaging = true, String stagingProfileId = null) {

    if (!(project.ext.has("nexusUsername") && project.ext.has("nexusPassword") && project.ext.has("nexusUrl"))) {
      return
    }
    if (requireStaging) {
      if (!stagingProfileId) {
        throw new IllegalArgumentException("Required stagingProfileId property not set")
      }
    }
    project.with {

//      def stagingUrl = "${nexusUrl}/service/local/staging/deploy/maven2/"
      def releasesUrl = "${nexusUrl}/content/repositories/releases"
      def snapshotUrl = "${nexusUrl}/content/repositories/snapshots"
      uploadArchives {
        onlyIf { !NexusTools.artifactExists(project, nexusUrl) }
        repositories {
          mavenDeployer {
            snapshotRepository(url: snapshotUrl) {
              authentication(userName: nexusUsername, password: nexusPassword)
            }
            repository(url: releasesUrl) {
              authentication(userName: nexusUsername, password: nexusPassword)
            }
          }
        }
      }

      String deployTask = requireStaging ? 'releaseStagingRepository' : 'uploadArchives'
      deployTask = version?.endsWith('-SNAPSHOT') ? 'uploadArchives' : deployTask

      task('deploy', description: 'Build and deploy artifacts to Nexus, potentially via staging') {
        dependsOn deployTask
        mustRunAfter 'clean'
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
}