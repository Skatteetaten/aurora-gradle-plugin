package no.skatteetaten.aurora.gradle.plugins.extensions

open class FeaturesConfiguration {
    var defaultPlugins: Boolean? = null
    var javaDefaults: Boolean? = null
    var deliveryBundle: Boolean? = null
    var spock: Boolean? = null
    var checkstylePlugin: Boolean? = null
    var jacocoTestReport: Boolean? = null
    var mavenDeployer: Boolean? = null
    var junit5Support: Boolean? = null
    var springDevTools: Boolean? = null
}