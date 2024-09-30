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
    id("application")
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

val edcGroup: String by project
val edcVersion: String by project
val postgresVersion: String by project

dependencies {
	implementation(project(":launchers:base:connector"))

    implementation("${edcGroup}:configuration-filesystem:${edcVersion}")
    implementation("${edcGroup}:vault-hashicorp:${edcVersion}")
    implementation("${edcGroup}:iam-mock:${edcVersion}")

	implementation("org.postgresql:postgresql:$postgresVersion")
	implementation("${edcGroup}:sql-pool-apache-commons:$edcVersion")
	implementation("${edcGroup}:transaction-local:$edcVersion")
	implementation("${edcGroup}:transaction-datasource-spi:$edcVersion")

    implementation("${edcGroup}:accesstokendata-store-sql:$edcVersion")
    implementation("${edcGroup}:asset-index-sql:$edcVersion")
    implementation("${edcGroup}:contract-definition-store-sql:$edcVersion")
    implementation("${edcGroup}:contract-negotiation-store-sql:$edcVersion")
    implementation("${edcGroup}:control-plane-sql:$edcVersion")
    implementation("${edcGroup}:data-plane-instance-store-sql:$edcVersion")
    implementation("${edcGroup}:data-plane-store-sql:$edcVersion")
    implementation("${edcGroup}:edr-index-sql:$edcVersion")
    implementation("${edcGroup}:policy-definition-store-sql:$edcVersion")
    implementation("${edcGroup}:transfer-process-store-sql:$edcVersion")
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
	exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("dataspace-connector.jar")
}
