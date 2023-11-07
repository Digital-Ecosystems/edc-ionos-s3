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
 *       ZF Friedrichshafen AG - add dependency
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

	
	  gradlePluginPortal()
}
val javaVersion: String by project
val faaastVersion: String by project
val edcGroup: String by project
val edcVersion: String by project
val okHttpVersion: String by project
val rsApi: String by project
val metaModelVersion: String by project

dependencies {

	implementation("${edcGroup}:control-plane-core:${edcVersion}")
	
	implementation("${edcGroup}:api-observability:${edcVersion}")
	
	implementation("${edcGroup}:configuration-filesystem:${edcVersion}")

	implementation("${edcGroup}:http:${edcVersion}")

	implementation("${edcGroup}:auth-tokenbased:${edcVersion}")	

    implementation("${edcGroup}:management-api:${edcVersion}")
		
	implementation("${edcGroup}:vault-hashicorp:${edcVersion}")

	
	//implementation("$edcGroup:ids:+")
		
	implementation("${edcGroup}:iam-mock:${edcVersion}")
	
    //implementation(project(":example:file-transfer-multicloud:transfer-file"))
	
	//adjust
   // implementation("${edcGroup}:data-plane-selector-api:${edcVersion}")

    //implementation("${edcGroup}:data-plane-api:${edcVersion}")
    //implementation("${edcGroup}:data-plane-http:${edcVersion}")
	
	implementation("${edcGroup}:dsp:${edcVersion}")
	//file-transfer
	implementation("${edcGroup}:control-plane-core:${edcVersion}")
	implementation("${edcGroup}:data-plane-core:${edcVersion}")
	implementation(project(":edc-ionos-extension:data-plane-ionos-s3"))
    implementation("${edcGroup}:data-plane-client:${edcVersion}")
    implementation("${edcGroup}:data-plane-selector-client:${edcVersion}")
    implementation("${edcGroup}:data-plane-selector-core:${edcVersion}")
    implementation("${edcGroup}:transfer-data-plane:${edcVersion}")
	
	//implementation("${edcGroup}:contract-spi:${edcVersion}")	
	//implementation("${edcGroup}:policy-model:${edcVersion}")		
	//implementation("${edcGroup}:policy-spi:${edcVersion}")	
	//implementation("${edcGroup}:core-spi:${edcVersion}")	
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}
tasks.shadowJar {
   isZip64 = true  
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("dataspace-connector.jar")
}
