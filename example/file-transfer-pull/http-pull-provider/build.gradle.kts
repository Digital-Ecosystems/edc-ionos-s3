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
	  maven {
        url = uri("https://maven.iais.fraunhofer.de/artifactory/eis-ids-public/")
    }
}

val edcGroupId = "org.eclipse.edc"
val edcVersion = "0.0.1-milestone-8"
val fraunhoferVersion: String by project

dependencies {
    implementation("${edcGroupId}:control-plane-core:${edcVersion}")
    implementation("${edcGroupId}:ids:${edcVersion}")
    implementation("${edcGroupId}:configuration-filesystem:${edcVersion}")
    implementation("${edcGroupId}:vault-filesystem:${edcVersion}")
    implementation("${edcGroupId}:iam-mock:${edcVersion}")
    implementation("${edcGroupId}:management-api:${edcVersion}")
    implementation("${edcGroupId}:transfer-data-plane:${edcVersion}")
    implementation("${edcGroupId}:transfer-pull-http-receiver:${edcVersion}")

    implementation("${edcGroupId}:data-plane-selector-api:${edcVersion}")
    implementation("${edcGroupId}:data-plane-selector-core:${edcVersion}")
    implementation("${edcGroupId}:data-plane-selector-client:${edcVersion}")

    implementation("${edcGroupId}:data-plane-api:${edcVersion}")
    implementation("${edcGroupId}:data-plane-core:${edcVersion}")
    implementation("${edcGroupId}:data-plane-http:${edcVersion}")

	implementation(project(":edc-ionos-extension:data-plane-ionos-s3"))
	implementation(project(":edc-ionos-extension:provision-ionos-s3"))

}

application {
    mainClass.set("${edcGroupId}.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("http-pull-connector.jar")
}