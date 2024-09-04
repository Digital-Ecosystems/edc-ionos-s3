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
	// Core
	implementation("${edcGroup}:boot:${edcVersion}")
	implementation("${edcGroup}:connector-core:${edcVersion}")
	implementation("${edcGroup}:http:${edcVersion}")
	implementation("${edcGroup}:dsp:${edcVersion}")
	implementation("${edcGroup}:configuration-filesystem:${edcVersion}")

	// Management API
	implementation("${edcGroup}:management-api:${edcVersion}")

	// EDR API
	implementation("${edcGroup}:edr-cache-api:${edcVersion}")
	implementation("${edcGroup}:edr-store-core:${edcVersion}")
	implementation("${edcGroup}:edr-store-receiver:${edcVersion}")

	// Control Plane
	implementation("${edcGroup}:control-api-configuration:${edcVersion}")
	implementation("${edcGroup}:control-plane-api-client:${edcVersion}")
	implementation("${edcGroup}:control-plane-api:${edcVersion}")
	implementation("${edcGroup}:control-plane-core:${edcVersion}")

	// Data Plane
	implementation("${edcGroup}:data-plane-selector-api:${edcVersion}")
	implementation("${edcGroup}:data-plane-selector-core:${edcVersion}")
	implementation("${edcGroup}:data-plane-self-registration:${edcVersion}")
	implementation("${edcGroup}:data-plane-control-api:${edcVersion}")
	implementation("${edcGroup}:data-plane-public-api-v2:${edcVersion}")
	implementation("${edcGroup}:data-plane-core:${edcVersion}")
	implementation("${edcGroup}:data-plane-http:${edcVersion}")
	implementation("${edcGroup}:transfer-data-plane-signaling:${edcVersion}")

	// Vault
	implementation("${edcGroup}:vault-hashicorp:${edcVersion}")

	// Validators
	implementation("${edcGroup}:validator-data-address-http-data:${edcVersion}")

	// Ionos Extensions
	implementation(project(":extensions:provision-ionos-s3"))
	implementation(project(":extensions:data-plane-ionos-s3"))
}

tasks.jar {
	archiveFileName.set("base-connector.jar")
}
