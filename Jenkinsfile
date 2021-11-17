def jenkinsfile

def overrides = [
    scriptVersion  : 'v7',
    iq : false,
    checkstyle : false,
    openShiftBuild: false,
    disableAllReports: true,
    pipelineScript: 'https://git.aurora.skead.no/scm/ao/aurora-pipeline-scripts.git',
    credentialsId: "github",
    javaVersion: 11,
    jiraFiksetIKomponentversjon: true,
    deployTo: "gradle-plugin-portal",
    chatRoom: "#aos-notifications",
    deployGoal : "publishPlugins",
    versionStrategy: [
      [branch: 'master', versionHint: '4']
    ]
]

fileLoader.withGit(overrides.pipelineScript,, overrides.scriptVersion) {
   jenkinsfile = fileLoader.load('templates/leveransepakke')
}

jenkinsfile.gradle(overrides.scriptVersion, overrides)