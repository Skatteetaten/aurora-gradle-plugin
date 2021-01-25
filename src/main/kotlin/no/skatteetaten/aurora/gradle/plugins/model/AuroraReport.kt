package no.skatteetaten.aurora.gradle.plugins.model

data class AuroraReport(
    val name: String = "",
    val description: String = "",
    val pluginsApplied: List<String> = listOf(),
    val dependenciesAdded: List<String> = listOf()
) {
    override fun toString(): String =
        "$name\n${when {
            description != "" -> {
                "\n  Configuration: $description"
            }
            else -> ""
        }}\n${when {
            pluginsApplied.isNotEmpty() -> {
                "\n  Plugins      : ${pluginsApplied.joinToString(", ")}"
            }
            else -> ""
        }}\n${when {
            dependenciesAdded.isNotEmpty() -> {
                "\n  Dependencies :\n     ${dependenciesAdded.joinToString("\n     ") }"
            }
            else -> ""
        }}"
}
