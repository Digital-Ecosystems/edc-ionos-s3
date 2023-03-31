/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - initial API and implementation
 *
 */

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:control-plane:contract-spi"))
    api(project(":spi:control-plane:transfer-spi"))
    api(project(":spi:common:transaction-spi"))
    implementation(project(":spi:common:transaction-datasource-spi"))
    implementation(project(":extensions:common:sql:sql-core"))
    implementation(project(":extensions:common:sql:sql-lease"))

    testImplementation(project(":core:common:junit"))
    testImplementation(libs.assertj)
    testImplementation(libs.awaitility)
    testImplementation(libs.postgres)
    testImplementation(testFixtures(project(":spi:control-plane:transfer-spi")))
    testImplementation(testFixtures(project(":extensions:common:sql:sql-lease")))
    testImplementation(testFixtures(project(":extensions:common:sql:sql-core")))

}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}
