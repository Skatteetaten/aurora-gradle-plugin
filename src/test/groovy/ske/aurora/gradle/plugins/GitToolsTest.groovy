package ske.aurora.gradle.plugins

import org.ajoberstar.grgit.Grgit
import org.apache.tools.ant.taskdefs.Expand
import org.gradle.api.Project

import spock.lang.Specification

class GitToolsTest extends Specification {

  static String repoFolder

  def setupSpec() {
    // We need a couple of git repositories to test basic git functionality, but there is no easy way to version
    // control one git repository from within another. So I have just zipped these test repos into an archive
    // and unzip them before each test run.
    def ant = new AntBuilder()
    Expand unzip = ant.unzip(src: "src/test/resources/gitrepos.zip",
        dest: "build/resources",
        overwrite: "true")
    repoFolder = "$unzip.dest/gitrepos"
  }

  def "Allows version on master if not on a tag when enforce is disabled"() {

    given:
      def gitTools = new GitTools(Mock(Project), Grgit.open(dir: "$repoFolder/on_master_without_tag"))
      gitTools.enforceTagOnMaster = false

    when:
      String v = gitTools.getVersionFromGit("v")

    then:
      v == 'master-SNAPSHOT'
      noExceptionThrown()
  }
}
