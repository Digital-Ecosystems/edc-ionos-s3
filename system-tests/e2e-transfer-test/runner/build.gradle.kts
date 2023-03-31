/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

plugins {
    java
}

dependencies {
    testImplementation(project(":extensions:common:sql:sql-core"))

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))
    testImplementation(testFixtures(project(":extensions:common:azure:azure-cosmos-core")))
    testImplementation(testFixtures(project(":extensions:common:sql:sql-core")))
    testImplementation(project(":core:common:junit"))

    testImplementation(libs.postgres)
    testImplementation(libs.restAssured)
    testImplementation(libs.assertj)
    testImplementation(libs.awaitility)
    testImplementation(libs.junit.jupiter.api)

    testCompileOnly(project(":system-tests:e2e-transfer-test:backend-service"))
    testCompileOnly(project(":system-tests:e2e-transfer-test:control-plane"))
    testCompileOnly(project(":system-tests:e2e-transfer-test:control-plane-cosmosdb"))
    testCompileOnly(project(":system-tests:e2e-transfer-test:control-plane-postgresql"))
    testCompileOnly(project(":system-tests:e2e-transfer-test:data-plane"))
}
