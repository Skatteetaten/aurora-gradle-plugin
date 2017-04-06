package ske.aurora.gradle.plugins

import org.gradle.api.Project

class ProjectTools {

  static void setProjectVersion(Project project, String versionToSet) {

    project.with {
      allprojects {
        version = versionToSet
      }
    }
  }
}
