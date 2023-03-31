/*
 *  Copyright (c) 2022 Microsoft Corporation
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
    java
}

dependencies {
    testImplementation(libs.bundles.jupiter)
    testImplementation(libs.restAssured)
    testImplementation(libs.assertj)
    testImplementation(libs.awaitility)
    testImplementation(libs.mockserver.netty)
    testImplementation(libs.mockserver.client)

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":extensions:data-plane:data-plane-http")))
    testImplementation(project(":spi:data-plane:data-plane-spi"))

    testRuntimeOnly(project(":launchers:data-plane-server"))
}
