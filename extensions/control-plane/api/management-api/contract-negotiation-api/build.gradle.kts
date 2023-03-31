/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *   ZF Friedrichshafen AG - Initial API and Implementation
 */
plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    api(project(":spi:control-plane:control-plane-spi"))
    api(project(":spi:common:transaction-spi"))
    implementation(project(":extensions:common:api:api-core"))
    implementation(project(":extensions:common:api:management-api-configuration"))

    implementation(libs.jakarta.rsApi)

    testImplementation(project(":core:common:util"))
    testImplementation(project(":core:control-plane:control-plane-core"))
    testImplementation(project(":extensions:common:http"))
    testImplementation(project(":core:common:junit"))

    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
}

edcBuild {
    swagger {
        apiGroup.set("management-api")
    }
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}
