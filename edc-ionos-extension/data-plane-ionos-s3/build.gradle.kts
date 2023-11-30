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
 *       Fraunhofer Institute for Software and Systems Engineering - added dependencies
 *
 */
 
plugins {
    `java-library`
    `maven-publish`
}

val javaVersion: String by project
val edcGroup: String by project
val edcVersion: String by project
val extensionsGroup: String by project
val extensionsVersion: String by project

val gitHubPkgsName: String by project
val gitHubPkgsUrl: String by project
val gitHubUser: String? by project
val gitHubToken: String? by project






dependencies {

   
	api("${edcGroup}:data-plane-spi:${edcVersion}")
	implementation("${edcGroup}:util:${edcVersion}")
	implementation("${edcGroup}:transfer-spi:${edcVersion}")
	implementation("${edcGroup}:data-plane-util:${edcVersion}")
	implementation(project(":edc-ionos-extension:core-ionos-s3"))
	
    implementation("${edcGroup}:data-plane-core:${edcVersion}")
	testImplementation("${edcGroup}:data-plane-core:${edcVersion}")
	
	implementation("${edcGroup}:http:${edcVersion}")
			
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
	
    testImplementation("org.assertj:assertj-core:3.22.0")
    implementation("org.realityforge.org.jetbrains.annotations:org.jetbrains.annotations:1.7.0")

}

java {
	withJavadocJar()
	withSourcesJar()
}

publishing {
	publications {
		create<MavenPublication>("maven") {
			groupId = extensionsGroup
			artifactId = "data-plane-ionos-s3"
			version = extensionsVersion

			from(components["java"])

			pom {
				name.set("data-plane-ionos-s3")
				description.set("Extension to perform the data exchange process using an IONOS Cloud S3 storage")
			}
		}
	}
	repositories {
		maven {
			name = gitHubPkgsName
			url = uri(gitHubPkgsUrl)
			credentials {
				username = gitHubUser
				password = gitHubToken
			}
		}
	}
}