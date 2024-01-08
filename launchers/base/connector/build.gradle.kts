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
	implementation("${edcGroup}:boot:${edcVersion}")

	// Control Plane
    implementation("${edcGroup}:control-plane-core:${edcVersion}")
	implementation("${edcGroup}:control-plane-api-client:${edcVersion}")

	implementation("${edcGroup}:http:${edcVersion}")
	implementation("${edcGroup}:dsp:${edcVersion}")
	implementation("${edcGroup}:auth-tokenbased:${edcVersion}")
	implementation("${edcGroup}:configuration-filesystem:${edcVersion}")

    implementation("$edcGroup:management-api:$edcVersion")
	implementation("${edcGroup}:api-observability:${edcVersion}")

	implementation("${edcGroup}:vault-hashicorp:${edcVersion}")
	
	// Data Plane
	implementation("${edcGroup}:data-plane-selector-client:${edcVersion}")
	implementation("${edcGroup}:data-plane-selector-core:${edcVersion}")
	implementation("${edcGroup}:data-plane-core:${edcVersion}")
	implementation("${edcGroup}:data-plane-client:${edcVersion}")
	implementation("${edcGroup}:transfer-data-plane:${edcVersion}")

	// Ionos Extensions
	implementation(project(":extensions:provision-ionos-s3"))
	implementation(project(":extensions:data-plane-ionos-s3"))
}

tasks.jar {
	archiveFileName.set("base-connector.jar")
}
