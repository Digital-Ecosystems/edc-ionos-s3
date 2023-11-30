/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */
plugins {
    `java-library`
    `maven-publish`
}

val edcGroup: String by project
val edcVersion: String by project
val extensionsGroup: String by project
val extensionsVersion: String by project

val gitHubPkgsName: String by project
val gitHubPkgsUrl: String by project
val gitHubUser: String? by project
val gitHubToken: String? by project

dependencies {
    api("${edcGroup}:core-spi:${edcVersion}")
    api("${edcGroup}:http-spi:${edcVersion}")

    implementation("${edcGroup}:util:${edcVersion}")
}


java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = extensionsGroup
            artifactId = "vault-hashicorp"
            version = extensionsVersion

            from(components["java"])

            pom {
                name.set("vault-hashicorp")
                description.set("Extension to use Hashicorp Vault to store certificates and secrets")
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