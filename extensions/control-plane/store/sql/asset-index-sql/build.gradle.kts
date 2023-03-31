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
    api(project(":spi:common:transaction-spi"))
    implementation(project(":spi:common:transaction-datasource-spi"))
    implementation(project(":extensions:common:sql:sql-core"))

    testImplementation(project(":core:common:junit"))

    testImplementation(project(":spi:common:policy-model"))
    testImplementation(libs.assertj)
    testImplementation(testFixtures(project(":spi:common:core-spi")))
    testImplementation(testFixtures(project(":extensions:common:sql:sql-core")))
    testImplementation(libs.postgres)

}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}
