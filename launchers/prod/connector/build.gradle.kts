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

repositories {
    maven {// while runtime-metamodel dependency is still a snapshot
		url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
	mavenCentral()
	mavenLocal()
}

val edcGroup: String by project
val edcVersion: String by project

dependencies {
	implementation(project(":launchers:base:connector"))

	implementation("${edcGroup}:oauth2-core:${edcVersion}")
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
	exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("dataspace-connector.jar")
}
