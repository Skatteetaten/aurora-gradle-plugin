object Versions {
    const val javaSourceCompatibility: String = "11"
    const val groovy: String = "3.0.5"
    const val spock: String = "1.3-groovy-2.5"
    const val junit5: String = "5.7.0"
    const val cglib: String = "3.3.0"
    const val objenesis: String = "3.1"
    const val auroraSpringBootMvcStarter: String = "1.0.+"
    const val auroraSpringBootWebFluxStarter: String = "1.0.+"
    const val springCloudContract: String = "2.2.4.RELEASE"
    const val kotlinLogging: String = "2.0.3"
    const val checkstyleConfig: String = "2.2.5"
    const val checkstyleConfigFile: String = "checkstyle/checkstyle-with-metrics.xml"
    const val kotlin = "1.4.10"
    const val assertk = "0.23"
}

object PluginVersions {
    const val gradle_test_logger = "2.1.1"
    const val ktlint = "9.4.1"
    const val spring_boot = "2.3.5.RELEASE"
    const val ben_manes_versions = "0.36.0"
    const val gradle_plugin_publish = "0.12.0"
    const val asciidoctor = "2.4.0"
    const val pitest = "1.5.2"
    const val cloud_contract = "2.2.4.RELEASE"
    const val dependency_management = "1.0.10.RELEASE"
    const val sonar = "3.0"
    const val git_properties = "2.2.4"
    const val latest_versions = "0.2.15"
}

object Features {
    const val applyDefaultPlugins: Boolean = true
    const val applyJavaDefaults: Boolean = true
    const val applyDeliveryBundleConfig: Boolean = true
    const val applySpockSupport: Boolean = false
    const val applyCheckstylePlugin: Boolean = true
    const val applyJacocoTestReport: Boolean = true
    const val applyMavenDeployer: Boolean = true
    const val applyJunit5Support: Boolean = true
    const val springDevTools: Boolean = false
    const val useWebFlux: Boolean = false
    const val useBootJar: Boolean = false
    const val useAuroraStarters: Boolean = true
}
