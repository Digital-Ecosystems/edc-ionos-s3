val edcVersion: String by project
val edcGroup: String by project

plugins {
    `java-library`
}

dependencies {

    api("${edcGroup}:core-spi:${edcVersion}")
    api("${edcGroup}:control-plane-spi:${edcVersion}")
    implementation("${edcGroup}:api-core:${edcVersion}")
    implementation("${edcGroup}:control-api-configuration:${edcVersion}")

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
    implementation("jakarta.validation:jakarta.validation-api:3.0.2")
}
