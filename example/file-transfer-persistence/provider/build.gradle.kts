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
   
	  gradlePluginPortal()
}
val javaVersion: String by project
val faaastVersion: String by project
val edcGroup: String by project
val postgresqlGroup: String by project
val postgresqlVersion: String by project
val edcVersion: String by project
val okHttpVersion: String by project
val rsApi: String by project
val metaModelVersion: String by project

dependencies {
    implementation("${edcGroup}:asset-index-sql:$edcVersion")
    implementation("${edcGroup}:policy-definition-store-sql:$edcVersion")
    implementation("${edcGroup}:contract-definition-store-sql:$edcVersion")
    implementation("${edcGroup}:contract-negotiation-store-sql:$edcVersion")
    implementation("${edcGroup}:transfer-process-store-sql:$edcVersion")
    implementation("${edcGroup}:transaction-datasource-spi:$edcVersion")
    implementation("${edcGroup}:sql-pool-apache-commons:$edcVersion")
    implementation("${edcGroup}:transaction-local:$edcVersion")
    implementation("${postgresqlGroup}:postgresql:$postgresqlVersion")
    implementation("${edcGroup}:control-plane-sql:$edcVersion")

	implementation("${edcGroup}:control-plane-core:${edcVersion}")
	
	implementation("${edcGroup}:api-observability:${edcVersion}")
	
	implementation("${edcGroup}:configuration-filesystem:${edcVersion}")

	implementation("${edcGroup}:http:${edcVersion}")

	implementation("${edcGroup}:auth-tokenbased:${edcVersion}")	

    implementation("${edcGroup}:management-api:${edcVersion}")
	
	implementation("${edcGroup}:vault-hashicorp:${edcVersion}")	

	implementation("${edcGroup}:iam-mock:${edcVersion}")
	
    implementation(project(":example:file-transfer-persistence:transfer-file"))
	
	//new
	implementation("${edcGroup}:dsp:${edcVersion}")
	
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
