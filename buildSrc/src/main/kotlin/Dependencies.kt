object Versions {
    const val javaSourceCompatibility: String = "11"
    const val groovy: String = "3.0.8"
    const val spock: String = "2.0-groovy-3.0"
    const val junit5: String = "5.9.1"
    const val jacocoTools: String = "0.8.7"
    const val cglib: String = "3.3.2"
    const val objenesis: String = "3.2"
    const val auroraSpringBootMvcStarter: String = "1.8.+"
    const val auroraSpringBootWebFluxStarter: String = "1.6.+"
    const val springCloudContract: String = "3.1.4"
    const val kotlinLogging: String = "3.0.4"
    const val checkstyleConfig: String = "2.2.7"
    const val checkstyleConfigFile: String = "checkstyle/checkstyle-with-metrics.xml"
    const val kotlin = "1.7.20"
    const val assertk = "0.25"
}

object PluginVersions {
    const val gradle_test_logger = "3.2.0"
    const val ktlint = "11.0.0"
    const val spring_boot = "2.7.5"
    const val ben_manes_versions = "0.43.0"
    const val gradle_plugin_publish = "1.0.0"
    const val asciidoctor = "3.3.2"
    const val pitest = "1.9.0"
    const val cloud_contract = "3.1.4"
    const val dependency_management = "1.1.0"
    const val sonar = "3.5.0.2730"
    const val git_properties = "2.4.0"
    const val latest_versions = "0.2.18"
    const val cyclonedx_versions = "1.7.2"
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
    const val usePython: Boolean = false
    const val useBootJar: Boolean = false
    const val useAuroraStarters: Boolean = true
}
