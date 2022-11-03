def jenkinsfile

def overrides = [
    scriptVersion  : 'v7',
    openShiftBuild: false,
    pipelineScript: 'https://git.aurora.skead.no/scm/ao/aurora-pipeline-scripts.git',
    credentialsId: "github",
    javaVersion: 11,
    iqOrganizationName: "Team AOS",
    disableAllReports: true,
    deployTo: "gradle-plugin-portal",
    chatRoom: "#aos-notifications",
    deployGoal : "publishPlugins",
    versionStrategy: []
]

fileLoader.withGit(overrides.pipelineScript,, overrides.scriptVersion) {
   jenkinsfile = fileLoader.load('templates/leveransepakke')
}

jenkinsfile.gradle(overrides.scriptVersion, overrides)