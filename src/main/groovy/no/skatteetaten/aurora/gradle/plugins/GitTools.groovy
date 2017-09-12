package no.skatteetaten.aurora.gradle.plugins

import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project

import no.skatteetaten.aurora.version.git.GitVersion

class GitTools {

  Project project

  Grgit git

  String versionPrefix = ''

  String branchVersionPostfix = '-SNAPSHOT'

  String fallbackVersion = "0$branchVersionPostfix"

  boolean enforceTagOnMaster = true

  boolean fallbackToBranchNameEnv = true

  GitTools(Project p, Grgit git) {

    this.project = p
    this.git = git
  }

  void setProjectVersionFromGit() {

    String versionToSet = getVersionFromGit(versionPrefix)
    ProjectTools.setProjectVersion(project, versionToSet)
  }

  void setProjectRevision() {
    project.ext["revision"] = git.head().id
  }

  String getVersionFromGit(String versionPrefix) {

    def options = new GitVersion.Options(
        versionPrefix: versionPrefix,
        fallbackToBranchNameEnv: this.fallbackToBranchNameEnv,
        fallbackVersion: this.fallbackVersion,
        versionFromBranchNamePostfix: this.branchVersionPostfix
    )
    GitVersion.determineVersion(git.getRepository().rootDir, options).version
  }
}
