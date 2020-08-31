package no.skatteetaten.aurora.gradle.plugins.model

import org.gradle.api.Project

data class AuroraConfiguration(
    val applyDefaultPlugins: Boolean = true,
    val applyJavaDefaults: Boolean = true,
    val javaSourceCompatibility: String = "1.8",
    val applyDeliveryBundleConfig: Boolean = true,
    val applySpockSupport: Boolean = false,
    val groovyVersion: String = "3.0.5",
    val spockVersion: String = "1.3-groovy-2.5",
    val junit5Version: String = "5.6.2",
    val cglibVersion: String = "3.3.0",
    val objenesisVersion: String = "3.1",
    val applyCheckstylePlugin: Boolean = true,
    val applyJacocoTestReport: Boolean = true,
    val applyMavenDeployer: Boolean = true,
    val auroraSpringBootMvcStarterVersion: String = "1.0.+",
    val auroraSpringBootWebFluxStarterVersion: String = "1.0.+",
    val useWebFlux: Boolean = false,
    val useBootJar: Boolean = false,
    val springCloudContractVersion: String = "2.3.3.RELEASE",
    val kotlinLoggingVersion: String = "1.8.3",
    val checkstyleConfigVersion: String = "2.2.5",
    val checkstyleConfigFile: String = "checkstyle/checkstyle-with-metrics.xml",
    val applyJunit5Support: Boolean = true,
    val springDevTools: Boolean = false
)

fun Project.getConfig(): AuroraConfiguration {
    val prefix = "aurora."
    val props = extensions.extraProperties.properties
        .filter { it.key.startsWith(prefix) }
        .map { (k, v) -> k.substring(prefix.length) to v }
        .toMap()
    props.forEach { (k, v) -> logger.lifecycle("overrides -> $k:$v") }

    return AuroraConfiguration(
        applyDefaultPlugins = (props["applyDefaultPlugins"] as? String)?.toBoolean() ?: true,
        applyJavaDefaults = (props["applyJavaDefaults"] as? String)?.toBoolean() ?: true,
        javaSourceCompatibility = props["javaSourceCompatibility"] as? String ?: "1.8",
        applyDeliveryBundleConfig = (props["applyDeliveryBundleConfig"] as? String)?.toBoolean() ?: true,
        applySpockSupport = (props["applySpockSupport"] as? String)?.toBoolean() ?: false,
        groovyVersion = props["groovyVersion"] as? String ?: "3.0.5",
        spockVersion = props["spockVersion"] as? String ?: "1.3-groovy-2.5",
        junit5Version = props["junit5Version"] as? String ?: "5.6.2",
        cglibVersion = props["cglibVersion"] as? String ?: "3.3.0",
        objenesisVersion = props["objenesisVersion"] as? String ?: "3.1",
        applyCheckstylePlugin = (props["applyCheckstylePlugin"] as? String)?.toBoolean() ?: true,
        applyJacocoTestReport = (props["applyJacocoTestReport"] as? String)?.toBoolean() ?: true,
        applyMavenDeployer = (props["applyMavenDeployer"] as? String)?.toBoolean() ?: true,
        auroraSpringBootMvcStarterVersion = props["auroraSpringBootMvcStarterVersion"] as? String ?: "1.0.+",
        auroraSpringBootWebFluxStarterVersion = props["auroraSpringBootWebFluxStarterVersion"] as? String ?: "1.0.+",
        useWebFlux = (props["useWebFlux"] as? String)?.toBoolean() ?: false,
        useBootJar = (props["useBootJar"] as? String)?.toBoolean() ?: false,
        springCloudContractVersion = props["springCloudContractVersion"] as? String ?: "2.3.3.RELEASE",
        kotlinLoggingVersion = props["kotlinLoggingVersion"] as? String ?: "1.8.3",
        checkstyleConfigVersion = props["checkstyleConfigVersion"] as? String ?: "2.2.5",
        checkstyleConfigFile = props["checkstyleConfigFile"] as? String ?: "checkstyle/checkstyle-with-metrics.xml",
        applyJunit5Support = (props["applyJunit5Support"] as? String)?.toBoolean() ?: true,
        springDevTools = (props["springDevTools"] as? String)?.toBoolean() ?: false
    )
}
