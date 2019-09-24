package no.skatteetaten.aurora.gradle.plugins

import org.gradle.api.Project

class MavenTools {

  Project project

  MavenTools(Project p) {

    this.project = p
  }

  AuroraReport addMavenDeployer() {

    if (!(project.ext.has("repositoryUsername")
        && project.ext.has("repositoryPassword")
        && project.ext.has("repositoryReleaseUrl")
        && project.ext.has("repositorySnapshotUrl")
    )) {
      return new AuroraReport(name: "aurora.applyMavenDeployer", description: """One of the following properties are missing in your .gradle file
         repositoryUsername, repositoryPassword, repositoryReleaseUrl, repositorySnapshotUrl""")
    }
    project.with {
      uploadArchives {
        repositories {
          mavenDeployer {
            snapshotRepository(url: repositorySnapshotUrl) {
              authentication(userName: repositoryUsername, password: repositoryPassword)
            }
            repository(url: repositoryReleaseUrl) {
              authentication(userName: repositoryUsername, password: repositoryPassword)
            }
          }
        }
      }

      task('deploy', description: 'Build and deploy artifacts to Nexus') {
        dependsOn 'uploadArchives'
        mustRunAfter 'clean'
      }
    }
    return new AuroraReport(name: "aurora.applyMavenDeployer",
        description: "add deploy task and configure from repository* properties in .gradle.properties.")
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