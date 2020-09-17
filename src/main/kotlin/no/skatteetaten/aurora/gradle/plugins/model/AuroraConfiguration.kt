package no.skatteetaten.aurora.gradle.plugins.model

import Features
import Versions
import no.skatteetaten.aurora.gradle.plugins.extensions.getFeaturesExtension
import no.skatteetaten.aurora.gradle.plugins.extensions.getUseSpringBootExtension
import no.skatteetaten.aurora.gradle.plugins.extensions.getVersionsExtension
import org.gradle.api.Project

data class AuroraConfiguration(
    val javaSourceCompatibility: String = Versions.javaSourceCompatibility,
    val groovyVersion: String = Versions.groovy,
    val spockVersion: String = Versions.spock,
    val junit5Version: String = Versions.junit5,
    val cglibVersion: String = Versions.cglib,
    val objenesisVersion: String = Versions.objenesis,
    val auroraSpringBootMvcStarterVersion: String = Versions.auroraSpringBootMvcStarter,
    val auroraSpringBootWebFluxStarterVersion: String = Versions.auroraSpringBootWebFluxStarter,
    val springCloudContractVersion: String = Versions.springCloudContract,
    val kotlinLoggingVersion: String = Versions.kotlinLogging,
    val checkstyleConfigVersion: String = Versions.checkstyleConfig,
    val checkstyleConfigFile: String = Versions.checkstyleConfigFile,
    val applyDefaultPlugins: Boolean = Features.applyDefaultPlugins,
    val applyJavaDefaults: Boolean = Features.applyDefaultPlugins,
    val applyDeliveryBundleConfig: Boolean = Features.applyDeliveryBundleConfig,
    val applySpockSupport: Boolean = Features.applySpockSupport,
    val applyCheckstylePlugin: Boolean = Features.applyCheckstylePlugin,
    val applyJacocoTestReport: Boolean = Features.applyJacocoTestReport,
    val applyMavenDeployer: Boolean = Features.applyMavenDeployer,
    val applyJunit5Support: Boolean = Features.applyJunit5Support,
    val springDevTools: Boolean = Features.springDevTools,
    val useWebFlux: Boolean = Features.useWebFlux,
    val useBootJar: Boolean = Features.useBootJar,
    val useAuroraStarters: Boolean = Features.useAuroraStarters
) {
    override fun toString(): String =
        "AuroraConfiguration(" +
            "javaSourceCompatibility='$javaSourceCompatibility',\n" +
            "groovyVersion='$groovyVersion',\n" +
            "spockVersion='$spockVersion',\n" +
            "junit5Version='$junit5Version',\n" +
            "cglibVersion='$cglibVersion',\n" +
            "objenesisVersion='$objenesisVersion',\n" +
            "auroraSpringBootMvcStarterVersion='$auroraSpringBootMvcStarterVersion',\n" +
            "auroraSpringBootWebFluxStarterVersion='$auroraSpringBootWebFluxStarterVersion',\n" +
            "springCloudContractVersion='$springCloudContractVersion',\n" +
            "kotlinLoggingVersion='$kotlinLoggingVersion',\n" +
            "checkstyleConfigVersion='$checkstyleConfigVersion',\n" +
            "checkstyleConfigFile='$checkstyleConfigFile',\n" +
            "applyDefaultPlugins=$applyDefaultPlugins,\n" +
            "applyJavaDefaults=$applyJavaDefaults,\n" +
            "applyDeliveryBundleConfig=$applyDeliveryBundleConfig,\n" +
            "applySpockSupport=$applySpockSupport,\n" +
            "applyCheckstylePlugin=$applyCheckstylePlugin,\n" +
            "applyJacocoTestReport=$applyJacocoTestReport,\n" +
            "applyMavenDeployer=$applyMavenDeployer,\n" +
            "applyJunit5Support=$applyJunit5Support,\n" +
            "springDevTools=$springDevTools,\n" +
            "useWebFlux=$useWebFlux,\n" +
            "useBootJar=$useBootJar,\n" +
            "useAuroraStarters=$useAuroraStarters)"
}

private fun Map<String, Any>.asString(key: String): String? = get(key) as? String
private fun Map<String, Any>.asBoolean(key: String): Boolean? = (get(key) as? String)?.toBoolean()

fun Project.getConfig(): AuroraConfiguration {
    val prefix = "aurora."
    val props = extensions.extraProperties.properties
        .filter { it.key.startsWith(prefix) }
        .map { (k, v) -> k.substring(prefix.length) to v }
        .toMap()
    props.forEach { (k, v) -> logger.lifecycle("overrides -> $k:$v") }
    val spring = getUseSpringBootExtension()
    val versions = getVersionsExtension()
    val features = getFeaturesExtension()

    return AuroraConfiguration(
        javaSourceCompatibility = versions.javaSourceCompatibility ?: props.asString("javaSourceCompatibility") ?: Versions.javaSourceCompatibility,
        groovyVersion = versions.javaSourceCompatibility ?: props.asString("groovyVersion") ?: Versions.groovy,
        spockVersion = versions.spock ?: props.asString("spockVersion") ?: Versions.spock,
        junit5Version = versions.junit5 ?: props.asString("junit5Version") ?: Versions.junit5,
        cglibVersion = versions.cglib ?: props.asString("cglibVersion") ?: Versions.cglib,
        objenesisVersion = versions.objenesis ?: props.asString("objenesisVersion") ?: Versions.objenesis,
        auroraSpringBootMvcStarterVersion = versions.auroraSpringBootMvcStarter ?: props.asString("auroraSpringBootMvcStarterVersion") ?: Versions.auroraSpringBootMvcStarter,
        auroraSpringBootWebFluxStarterVersion = versions.auroraSpringBootWebFluxStarter ?: props.asString("auroraSpringBootWebFluxStarterVersion") ?: Versions.auroraSpringBootWebFluxStarter,
        springCloudContractVersion = versions.springCloudContract ?: props.asString("springCloudContractVersion") ?: Versions.springCloudContract,
        kotlinLoggingVersion = versions.kotlinLogging ?: props.asString("kotlinLoggingVersion") ?: Versions.kotlinLogging,
        checkstyleConfigVersion = versions.checkstyleConfig ?: props.asString("checkstyleConfigVersion") ?: Versions.checkstyleConfig,
        checkstyleConfigFile = versions.checkstyleConfigFile ?: props.asString("checkstyleConfigFile") ?: Versions.checkstyleConfigFile,
        applyDefaultPlugins = features.defaultPlugins ?: props.asBoolean("applyDefaultPlugins") ?: Features.applyDefaultPlugins,
        applyJavaDefaults = features.javaDefaults ?: props.asBoolean("applyJavaDefaults") ?: Features.applyJavaDefaults,
        applyDeliveryBundleConfig = features.deliveryBundle ?: props.asBoolean("applyDeliveryBundleConfig") ?: Features.applyDeliveryBundleConfig,
        applySpockSupport = features.spock ?: props.asBoolean("applySpockSupport") ?: Features.applySpockSupport,
        applyCheckstylePlugin = features.checkstylePlugin ?: props.asBoolean("applyCheckstylePlugin") ?: Features.applyCheckstylePlugin,
        applyJacocoTestReport = features.jacocoTestReport ?: props.asBoolean("applyJacocoTestReport") ?: Features.applyJacocoTestReport,
        applyMavenDeployer = features.mavenDeployer ?: props.asBoolean("applyMavenDeployer") ?: Features.applyMavenDeployer,
        applyJunit5Support = features.junit5Support ?: props.asBoolean("applyJunit5Support") ?: Features.applyJunit5Support,
        springDevTools = features.springDevTools ?: props.asBoolean("springDevTools") ?: Features.springDevTools,
        useWebFlux = spring.webFluxEnabled ?: props.asBoolean("useWebFlux") ?: Features.useWebFlux,
        useBootJar = spring.bootJarEnabled ?: props.asBoolean("useBootJar") ?: Features.useBootJar,
        useAuroraStarters = features.auroraStarters ?: props.asBoolean("useAuroraStarters") ?: Features.useAuroraStarters
    )
}
