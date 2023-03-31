/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:data-plane:data-plane-spi"))
    api(project(":spi:control-plane:control-plane-api-client-spi"))

    implementation(project(":core:data-plane:data-plane-util"))
    implementation(project(":core:common:util"))

    implementation(libs.opentelemetry.annotations)

    testImplementation(project(":core:common:junit"))
    testImplementation(libs.awaitility)
    testImplementation(testFixtures(project(":spi:data-plane:data-plane-spi")))
}


publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}
