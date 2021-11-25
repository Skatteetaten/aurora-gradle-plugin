import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension

fun Project.extraProps() = extensions.extraProperties.properties

fun Project.missingRepositoryConfiguration(): Boolean = !(
    extraProps().containsKey("repositoryUsername") &&
    extraProps().containsKey("repositoryPassword") &&
    extraProps().containsKey("repositoryReleaseUrl") &&
    extraProps().containsKey("repositorySnapshotUrl")
)

fun Project.configureNexus(publishingExtension: PublishingExtension) {
    val exProps = project.extensions.extraProperties.properties
    val repositoryReleaseUrl = exProps["repositoryReleaseUrl"] as String
    val repositorySnapshotUrl = exProps["repositorySnapshotUrl"] as String
    val repositoryUsername = exProps["repositoryUsername"] as String
    val repositoryPassword = exProps["repositoryPassword"] as String

    publishingExtension.repositories {
        when (version.toString().endsWith("SNAPSHOT")) {
            true -> maven {
                name = "snapshotRepository"
                url = uri(repositorySnapshotUrl)
                isAllowInsecureProtocol = true
                credentials {
                    username = repositoryUsername
                    password = repositoryPassword
                }
            }
            else -> maven {
                name = "repository"
                url = uri(repositoryReleaseUrl)
                isAllowInsecureProtocol = true
                credentials {
                    username = repositoryUsername
                    password = repositoryPassword
                }
            }
        }
    }
}

fun Project.hasKotlin(parentProject: Project) =
    parentProject.name != rootProject.name && parentProject.plugins.hasPlugin("org.jlleitschuh.gradle.ktlint")