plugins {
    `java-library`
    `maven-publish`
}

val javaVersion: String by project
val edcGroup: String by project
val edcVersion: String by project
val metaModelVersion: String by project
val extensionsGroup: String by project
val extensionsVersion: String by project

val gitHubPkgsName: String by project
val gitHubPkgsUrl: String by project
val gitHubUser: String? by project
val gitHubToken: String? by project

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = extensionsGroup
            artifactId = "provision-ionos-s3"
            version = extensionsVersion

            from(components["java"])

            pom {
                name.set("provision-ionos-s3")
                description.set("Extension to perform the provisioning process using an IONOS Cloud S3 storage")
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