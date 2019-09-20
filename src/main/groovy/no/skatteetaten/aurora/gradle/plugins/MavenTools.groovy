package no.skatteetaten.aurora.gradle.plugins

import java.text.MessageFormat

import org.gradle.api.Project

class MavenTools {

  Project project

  MavenTools(Project p) {

    this.project = p
  }

  AuroraReport addMavenDeployer() {

    if (!(project.ext.has("nexusUsername")
        && project.ext.has("nexusPassword")
        && project.ext.has("nexusReleaseUrl")
        && project.ext.has("nexusSnapshotUrl")
    )) {
      return
    }
    project.with {
      uploadArchives {
        repositories {
          mavenDeployer {
            snapshotRepository(url: nexusSnapshotUrl) {
              authentication(userName: nexusUsername, password: nexusPassword)
            }
            repository(url: nexusReleaseUrl) {
              authentication(userName: nexusUsername, password: nexusPassword)
            }
          }
        }
      }

      task('deploy', description: 'Build and deploy artifacts to Nexus, potentially via staging') {
        dependsOn 'uploadArchives'
        mustRunAfter 'clean'
      }
    }
    return new AuroraReport(name :"aurora.applyMavenDeployer",
        description: "add uploadArchives task and configure from nexusUrl in .gradle.properties")
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