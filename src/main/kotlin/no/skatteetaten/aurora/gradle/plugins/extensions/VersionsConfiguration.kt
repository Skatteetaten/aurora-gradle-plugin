@file:Suppress("unused")

package no.skatteetaten.aurora.gradle.plugins.extensions

open class VersionsConfiguration {
    var javaSourceCompatibility: String? = null
    var groovy: String? = null
    var spock: String? = null
    var junit5: String? = null
    var cglib: String? = null
    var objenesis: String? = null
    var auroraSpringBootMvcStarter: String? = null
    var auroraSpringBootWebFluxStarter: String? = null
    var springCloudContract: String? = null
    var kotlinLogging: String? = null
    var checkstyleConfig: String? = null
    var checkstyleConfigFile: String? = null
}
