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
    api(project(":extensions:common:azure:azure-blob-core"))

    implementation(libs.azure.storageblob)
    implementation(libs.failsafe.core)

    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}
