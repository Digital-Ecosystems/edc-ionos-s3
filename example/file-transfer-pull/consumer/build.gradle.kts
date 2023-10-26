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
 *
 */

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
	mavenLocal()
	mavenCentral()
    maven {// while runtime-metamodel dependency is still a snapshot
		url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

val edcGroup: String by project
val edcVersion: String by project

dependencies {
    implementation("${edcGroup}:control-plane-core:${edcVersion}")
    implementation("${edcGroup}:control-plane-api-client:${edcVersion}")
	implementation("${edcGroup}:dsp:${edcVersion}")
    implementation("${edcGroup}:configuration-filesystem:${edcVersion}")
    implementation("${edcGroup}:vault-filesystem:${edcVersion}")
    implementation("${edcGroup}:iam-mock:${edcVersion}")
    implementation("${edcGroup}:management-api:${edcVersion}")
    implementation("${edcGroup}:transfer-data-plane:${edcVersion}")
    implementation("${edcGroup}:transfer-pull-http-receiver:${edcVersion}")

    implementation("${edcGroup}:data-plane-selector-api:${edcVersion}")
    implementation("${edcGroup}:data-plane-selector-core:${edcVersion}")
    implementation("${edcGroup}:data-plane-selector-client:${edcVersion}")

    implementation("${edcGroup}:data-plane-api:${edcVersion}")
    implementation("${edcGroup}:data-plane-core:${edcVersion}")
    implementation("${edcGroup}:data-plane-http:${edcVersion}")

	implementation(project(":edc-ionos-extension:data-plane-ionos-s3"))
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("dataspace-connector.jar")
}