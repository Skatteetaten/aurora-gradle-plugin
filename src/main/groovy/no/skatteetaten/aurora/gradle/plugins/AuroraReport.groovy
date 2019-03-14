package no.skatteetaten.aurora.gradle.plugins

class AuroraReport {

  String name
  String description = ""
  List<String> pluginsApplied = []
  List<String> dependenciesAdded = []

  String toString() {

    def report = name
    if (description != "") {
      report += "\n  Configuration: $description"
    }
    if (!pluginsApplied.isEmpty()) {
      report += "\n  Plugins      : ${pluginsApplied.join(", ")}"
    }

    if (!dependenciesAdded.isEmpty()) {
      report += "\n  Dependencies :"
      dependenciesAdded.forEach {
        report += "\n    $it"
      }
    }

    return report
  }
}

