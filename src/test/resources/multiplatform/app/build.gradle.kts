aurora {
    useKotlinDefaults
    useSpringBootDefaults

    versions {
        auroraSpringBootWebFluxStarter = "1.2.+"
        auroraSpringBootMvcStarter = "1.2.+"
    }
}

dependencies {
    implementation(platform("software.amazon.awssdk:bom:2.17.85"))
}
