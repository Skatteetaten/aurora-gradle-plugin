object Versions {
    const val javaSourceCompatibility: String = "11"
    const val groovy: String = "3.0.5"
    const val spock: String = "1.3-groovy-2.5"
    const val junit5: String = "5.7.1"
    const val jacocoTools: String = "0.8.7"
    const val cglib: String = "3.3.2"
    const val objenesis: String = "3.1"
    const val auroraSpringBootMvcStarter: String = "1.2.+"
    const val auroraSpringBootWebFluxStarter: String = "1.2.+"
    const val springCloudContract: String = "3.0.3"
    const val kotlinLogging: String = "2.0.8"
    const val checkstyleConfig: String = "2.2.5"
    const val checkstyleConfigFile: String = "checkstyle/checkstyle-with-metrics.xml"
    const val kotlin = "1.5.10"
    const val assertk = "0.24"
}

object PluginVersions {
    const val gradle_test_logger = "3.0.0"
    const val ktlint = "10.1.0"
    const val spring_boot = "2.5.0"
    const val ben_manes_versions = "0.39.0"
    const val gradle_plugin_publish = "0.15.0"
    const val asciidoctor = "3.3.2"
    const val pitest = "1.6.0"
    const val cloud_contract = "3.0.3"
    const val dependency_management = "1.0.11.RELEASE"
    const val sonar = "3.2.0"
    const val git_properties = "2.2.4"
    const val latest_versions = "0.2.17"
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
