package no.skatteetaten.aurora.gradle.plugins.configurators

import no.skatteetaten.aurora.gradle.plugins.model.AuroraReport

interface Configurator {
    fun configure(): List<AuroraReport>
}
